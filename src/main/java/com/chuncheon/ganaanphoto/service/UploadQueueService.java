package com.chuncheon.ganaanphoto.service;

import java.awt.*;
import java.awt.image.BufferedImage;
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
		try (InputStream in = task.inputStream();
			 OutputStream out = Files.newOutputStream(targetPath)) {
			BufferedImage image = ImageIO.read(in);
			if (image == null) {
				log.error("[IMAGE] 이미지 포맷 파싱 실패: {}", originalFilename);
				throw new IOException("이미지 파일이 아님: " + originalFilename);
			}
			BufferedImage resized = resize(image, 2560);
			ImageIO.write(resized, ext, out);
		}

		// ✅ DB 저장
		FileUploadEntity entity = FileUploadEntity.builder()
			.originalName(originalFilename)
			.savedName(newFileName)
			.fileExtension(ext)
			.filePath(uploadDir)
			.regDt(LocalDateTime.now())
			.build();

		fileUploadRepository.save(entity);

		// ✅ SSE 전송
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

}
