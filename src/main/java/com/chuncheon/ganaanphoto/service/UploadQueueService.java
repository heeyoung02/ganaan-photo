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
	private final BlockingQueue<FileUploadTask> uploadQueue = new LinkedBlockingQueue<>(100);
	private final ExecutorService executor = Executors.newSingleThreadExecutor(); // 순차처리용
	private final FileUploadRepository fileUploadRepository;
	private final SseService sseService;
	private final FileUploadService fileUploadService;


	@PostConstruct
	public void startWorker() {
		executor.submit(() -> {
			while (true) {
				try {
					FileUploadTask task = uploadQueue.take();
					handleFile(task); // 실제 저장
				} catch (Exception e) {
					log.error("[TASK ERROR] 업로드 처리 중 예외 발생",e);
				}
			}
		});
	}

	public void enqueueFile(FileUploadTask task) {
		if (!uploadQueue.offer(task)) {
			log.warn("[QUEUE] 업로드 대기열이 가득 참: {}", task.originalName());
			throw new RuntimeException("업로드 대기열이 가득 찼습니다.");
		}
	}

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
				BufferedImage resized = resize(orientedImage, 2560);
				ImageIO.write(resized, ext, out);
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
		AffineTransform tx = new AffineTransform();
		switch (orientation) {
			case 6 -> tx.rotate(Math.toRadians(90), image.getWidth() / 2.0, image.getHeight() / 2.0);
			case 3 -> tx.rotate(Math.toRadians(180), image.getWidth() / 2.0, image.getHeight() / 2.0);
			case 8 -> tx.rotate(Math.toRadians(270), image.getWidth() / 2.0, image.getHeight() / 2.0);
			default -> { return image; }
		}

		AffineTransformOp op = new AffineTransformOp(tx, AffineTransformOp.TYPE_BICUBIC);
		BufferedImage rotated = new BufferedImage(image.getWidth(), image.getHeight(), image.getType());
		op.filter(image, rotated);
		return rotated;
	}
}
