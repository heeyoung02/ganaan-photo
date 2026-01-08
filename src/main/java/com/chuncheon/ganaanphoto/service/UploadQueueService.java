package com.chuncheon.ganaanphoto.service;

import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.image.AffineTransformOp;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URLEncoder;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;

import javax.imageio.ImageIO;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import com.chuncheon.ganaanphoto.dto.FileUploadTask;
import com.chuncheon.ganaanphoto.entity.FileUploadEntity;
import com.chuncheon.ganaanphoto.repository.FileUploadRepository;
import com.drew.imaging.ImageMetadataReader;
import com.drew.metadata.Directory;
import com.drew.metadata.Metadata;
import com.drew.metadata.exif.ExifIFD0Directory;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;

@Service
@RequiredArgsConstructor
@Log4j2
public class UploadQueueService {

	@Value("${file.upload-dir}")
	private String uploadDir;

	private static final int MAX_QUEUE_SIZE = 100; // 최대 큐 사이즈
	private final BlockingQueue<FileUploadTask> uploadQueue = new LinkedBlockingQueue<>(MAX_QUEUE_SIZE);

	// private final ExecutorService executor = Executors.newSingleThreadExecutor(); // 순차처리용
	private static final int WORKER_THREAD_COUNT = 2; // 처리 스레드 수
	private final ExecutorService executor = Executors.newFixedThreadPool(WORKER_THREAD_COUNT);

	private final FileUploadRepository fileUploadRepository;
	private final SseService sseService;
	private final FileUploadService fileUploadService;

	/**
	 * 업로드 워커 실행
	 */
	@PostConstruct
	public void startWorker() {
		for (int i = 0; i < WORKER_THREAD_COUNT; i++) {
			final int workerId = i + 1;
			executor.submit(() -> {
				log.info("📦 업로드 워커 #{} 시작", workerId);
				while (true) {
					try {
						FileUploadTask task = uploadQueue.take();
						handleFile(task);
					} catch (Exception e) {
						log.error("[TASK ERROR] 워커 #{} 처리 중 예외 발생", workerId, e);
					}
				}
			});
		}
	}

	/**
	 * 큐에 파일 넣기
	 * @param task
	 */
	public void enqueueFile(FileUploadTask task) {
		if (!uploadQueue.offer(task)) {
			log.warn("[QUEUE] 업로드 대기열이 가득 참: {}", task.originalName());
			throw new RuntimeException("업로드 대기열이 가득 찼습니다.");
		}
	}

	/**
	 * worker가 실행할 task (파일저장 + DB저장 + SSE전송)
	 * @param task
	 * @throws IOException
	 */
	private void handleFile(FileUploadTask task) throws IOException {

		// 파일명 처리
		String originalFilename = StringUtils.cleanPath(task.originalName());
		String ext = fileUploadService.getFileExtension(task.originalName()).toLowerCase();
		Set<String> supportedFormats = Set.of("jpg", "jpeg", "png", "bmp"); // 리사이즈시 안정적으로 지원하는 포맷들
		if (!supportedFormats.contains(ext)) { // 지원하는 포맷에 해당하지 않을 경우 jpg로 기본값 대체
			ext = "jpg";
		}
		String newFileName = UUID.randomUUID() + "." + ext;

		// 저장 경로 준비
		Path uploadPath = Path.of(uploadDir);
		if (!Files.exists(uploadPath)) {
			Files.createDirectories(uploadPath);
		}

		// 파일 저장
		Path targetPath = uploadPath.resolve(newFileName);
		try (
			InputStream originalIn = task.inputStream();
			ByteArrayOutputStream baos = new ByteArrayOutputStream()
		) {
			originalIn.transferTo(baos);
			byte[] bytes = baos.toByteArray();
			try (
				InputStream exifIn = new ByteArrayInputStream(bytes);
				InputStream imageIn = new ByteArrayInputStream(bytes);
				OutputStream out = Files.newOutputStream(targetPath)
			) {
				BufferedImage image = ImageIO.read(imageIn);
				if (image == null) {
					log.error("[IMAGE] 이미지 포맷 파싱 실패: {}", originalFilename);
					throw new IOException("이미지 파일이 아님: " + originalFilename);
				}
				BufferedImage orientedImage = applyExifOrientation(exifIn, image); // 회전정보 적용하여 저장
				// BufferedImage resized = resize(orientedImage, 2560);
				// ImageIO.write(resized, ext, out);
				ImageIO.write(orientedImage, ext, out);
			}
		}

		// DB 저장
		FileUploadEntity entity = FileUploadEntity.builder()
			.originalName(originalFilename)
			.savedName(newFileName)
			.fileExtension(ext)
			.filePath(uploadDir)
			.regDt(LocalDateTime.now())
			.build();

		fileUploadRepository.save(entity);

		// SSE 전송
		String imageUrl = "/rest/photo/" + URLEncoder.encode(newFileName, "UTF-8").replaceAll("\\+", "%20");
		sseService.broadcastNewImage(imageUrl);

		if (uploadQueue.size() % 10 == 0 || uploadQueue.size() < 5) {
			log.info("🟢 업로드 완료: {}, 현재 큐 상태: {}개 남음", originalFilename, uploadQueue.size());
		}

	}

	/**
	 * 이미지 리사이즈
	 * @param original
	 * @param targetWidth
	 * @return
	 */
	private BufferedImage resize(BufferedImage original, int targetWidth) {
		int height = (int) (original.getHeight() * (targetWidth / (double) original.getWidth()));
		BufferedImage resized = new BufferedImage(targetWidth, height, BufferedImage.TYPE_INT_RGB);
		Graphics2D g = resized.createGraphics();
		g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
		g.drawImage(original, 0, 0, targetWidth, height, null);
		g.dispose();
		return resized;
	}

	// 이미지 원본의 Orientation 태그를 확인하여 그것에 맞는 회전 적용
	private BufferedImage applyExifOrientation(InputStream exifStream, BufferedImage image) {
		try {
			Metadata metadata = ImageMetadataReader.readMetadata(exifStream);
			Directory dir = metadata.getFirstDirectoryOfType(ExifIFD0Directory.class);
			if (dir != null && dir.containsTag(ExifIFD0Directory.TAG_ORIENTATION)) {
				int orientation = dir.getInt(ExifIFD0Directory.TAG_ORIENTATION);
				return transformImageByOrientation(image, orientation);
			}
		} catch (Exception e) {
			log.warn("EXIF Orientation 처리 실패: {}", e.getMessage());
		}
		return image; // Orientation이 없거나 처리 실패 시 원본 이미지 그대로 반환
	}

	// Orientation 값에 따라 이미지 회전, 변환 적용
	private BufferedImage transformImageByOrientation(BufferedImage image, int orientation) {
		int w = image.getWidth();
		int h = image.getHeight();

		AffineTransform tx = new AffineTransform();
		int newW = w;
		int newH = h;

		switch (orientation) {
			case 6 -> { // 90°
				tx.translate(h, 0);
				tx.rotate(Math.toRadians(90));
				newW = h;
				newH = w;
			}
			case 3 -> { // 180°
				tx.translate(w, h);
				tx.rotate(Math.toRadians(180));
			}
			case 8 -> { // 270°
				tx.translate(0, w);
				tx.rotate(Math.toRadians(270));
				newW = h;
				newH = w;
			}
			default -> {
				return image;
			}
		}

		BufferedImage rotated = new BufferedImage(
			newW,
			newH,
			BufferedImage.TYPE_INT_RGB
		);

		Graphics2D g = rotated.createGraphics();
		g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
		g.drawImage(image, tx, null);
		g.dispose();

		return rotated;
	}

	// 최대 큐 사이즈 반환
	public int getMaxQueueCapacity() {
		return MAX_QUEUE_SIZE;
	}

	// 현재 큐 사이즈 반환
	public int getCurrentQueueSize() {
		return uploadQueue.size();
	}


}
