package com.chuncheon.ganaanphoto.service;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URLEncoder;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import javax.imageio.ImageIO;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.chuncheon.ganaanphoto.config.Config;
import com.chuncheon.ganaanphoto.dto.FileUploadDTO;
import com.chuncheon.ganaanphoto.entity.FileUploadEntity;
import com.chuncheon.ganaanphoto.repository.FileUploadRepository;
import com.chuncheon.ganaanphoto.utils.InMemoryMultipartFile;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class FileUploadService {

    private static final Logger log = LoggerFactory.getLogger(FileUploadService.class);
    private final FileUploadRepository fileUploadRepository;
    private final SseService sseService;

    /**
     * 업로드 파일 처리(검증)
     * @param files
     * @throws Exception
     */
    public void processUploadFiles(List<MultipartFile> files) throws Exception {
        int maxCount = Config.getProperty("file.max-count", 10);
        long defaultAllowSize = Config.getProperty("file.default-allow-size", 1048576);
        long maxSize = Config.getProperty("file.max-size", 5) * defaultAllowSize;

        // 갯수 check
        if (files.size() > maxCount) {
            throw new IllegalArgumentException("최대 " + maxCount + "개의 파일만 업로드 가능합니다.");
        }

        List<MultipartFile> processedFiles = new ArrayList<>();
        for (MultipartFile file : files) {
            String originalName = file.getOriginalFilename();
            String extension = getFileExtension(originalName);
            // 확장자 check
            if (!Config.checkAllowImg(extension)) {
                throw new IllegalArgumentException("허용되지 않는 파일 형식입니다: " + extension);
            }
            // 리사이즈 및 크기 check
            MultipartFile resizedFile = resizeImage(file, 2560);
            if (resizedFile.getSize() > maxSize) {
                throw new IllegalArgumentException("파일 크기는 " + maxSize / 1048576 + "MB를 초과할 수 없습니다.");
            }
            processedFiles.add(resizedFile);
        }
        // 저장
        saveFiles(processedFiles);
    }

    /**
     * 파일 업로드 및 DB 저장
     * @param files
     * @throws IOException
     */
    public void saveFiles(List<MultipartFile> files) throws IOException {
        String uploadDir = Config.getProperty("file.upload-dir"); // 업로드 경로
        if (uploadDir == null || uploadDir.isBlank()) {
            throw new IllegalArgumentException("파일 업로드 경로가 설정되지 않았습니다.");
        }
        // 저장 폴더 없으면 생성
        Files.createDirectories(Paths.get(uploadDir));

        for (MultipartFile file : files) {
            if (!file.isEmpty()) {
                String originalName = file.getOriginalFilename();
                String extension = getFileExtension(originalName);
                String savedName = UUID.randomUUID().toString() + "." + extension;

				// file 저장
                File saveFile = new File(uploadDir, savedName);
                file.transferTo(saveFile);

                // DB에 저장
                FileUploadEntity entity = FileUploadEntity.builder()
                    .originalName(originalName)
                    .savedName(savedName)
                    .fileExtension(extension)
                    .filePath(uploadDir)
                    .regDt(LocalDateTime.now()) // DB에서 CURRENT_TIMESTAMP 자동 생성
                    .build();

                fileUploadRepository.save(entity);

                // SSE 전송
                String imageUrl = "/rest/photo/" + URLEncoder.encode(savedName, "UTF-8").replaceAll("\\+", "%20");
                sseService.broadcastNewImage(imageUrl);

            }
        }
    }

    /**
     * 파일 및 db 삭제
     * @param savedName
     * @throws IOException
     */
	public void deleteFile(String savedName) throws IOException {
		String uploadDir = Config.getProperty("file.upload-dir");
		if (uploadDir == null || uploadDir.isBlank()) {
			throw new IllegalArgumentException("파일 업로드 경로가 설정되지 않았습니다.");
		}

		// DB에서 해당 파일 정보 조회
		Optional<FileUploadEntity> optional = fileUploadRepository.findBySavedName(savedName);
		if (optional.isEmpty()) {
			throw new FileNotFoundException("해당 파일이 존재하지 않습니다: " + savedName);
		}

		// DB 삭제
		FileUploadEntity entity = optional.get();
		fileUploadRepository.delete(entity);

		// 파일 삭제
		File file = new File(uploadDir, savedName);
		if (file.exists()) {
			if (!file.delete()) {
				throw new IOException("파일 삭제에 실패했습니다: " + file.getAbsolutePath());
			}
		}

		// 4. SSE로 삭제 알림 전송 (프론트에서 해당 이미지 제거하도록)
		String imageUrl = "/rest/photo/" + URLEncoder.encode(savedName, "UTF-8").replaceAll("\\+", "%20");
		sseService.broadcastImageDelete(imageUrl);
	}

    /**
     * 최신순으로 저장된 파일 이름 list 가져오기
     * @return
     */
    public List<String> getSavedFileNamesFromDB() {
        return fileUploadRepository.findAllByOrderByIdDesc()
            .stream()
            .map(FileUploadEntity::getSavedName)
            .collect(Collectors.toList());
    }

    /**
     * 최신순으로 저장된 file upload dto list 가져오기
     * @return
     */
    public List<FileUploadDTO> getUploadFileDTOFromDB() {
        return fileUploadRepository.findAllByOrderByIdDesc().stream().map(FileUploadEntity::fromEntity).collect(Collectors.toList());
    }

    /**
     * 파일 이름으로 데이터 삭제
     * @param savedName
     * @return
     */
    public boolean deleteFileByFileName(String savedName) {
        try {
            this.deleteFile(savedName);
            return true;
        } catch (IOException e) {
            log.error("[failed to delete file!] " + e.getMessage());
            return false;
		}
    }

    /**
     * 여러 파일을 저장된 이름을 기준으로 데이터 삭제
     * @param savedNames
     * @return
     */
    public boolean deleteFilesByFileNames(List<String> savedNames) {
        try{
            for(String savedName : savedNames){
                this.deleteFile(savedName);
            }
            return true;
        } catch (IOException e) {
			log.error("[failed to delete files!] " + e.getMessage());
            return false;
		}
    }

    // 모든 파일을 삭제하는 메서드 (위험해서 주석처리함)
    //    public boolean deleteAllFiles() {
    //        try {
    //            List<FileUploadEntity> allFiles = fileUploadRepository.findAll(); // 모든 파일 가져오기
    //            if (allFiles.isEmpty()) {
    //                return false;
    //            }
    //
    //            fileUploadRepository.deleteAll(allFiles); // 모든 파일 삭제
    //            return true;
    //        } catch (Exception e) {
    //            return false;
    //        }
    //    }

    /**
     * 이미지 리사이징
     * @param originalFile
     * @param targetWidth
     * @return
     * @throws IOException
     */
    public MultipartFile resizeImage(MultipartFile originalFile, int targetWidth) throws IOException {
        InputStream input = originalFile.getInputStream();
        BufferedImage originalImage = ImageIO.read(input);

        // 기존 이미지 크기 가져오기
        int width = originalImage.getWidth();
        int height = originalImage.getHeight();

        // 리사이징할 크기 계산
        double scale = (double) targetWidth / width;
        int newWidth = targetWidth;
        int newHeight = (int) (height * scale);

        // 새 BufferedImage 생성
        BufferedImage resizedImage = new BufferedImage(newWidth, newHeight, BufferedImage.TYPE_INT_RGB);
        Graphics2D g2d = resizedImage.createGraphics();
        g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
        g2d.drawImage(originalImage, 0, 0, newWidth, newHeight, null);
        g2d.dispose();

        // 압축하여 ByteArrayOutputStream에 저장
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(resizedImage, "jpg", baos);
        byte[] compressedBytes = baos.toByteArray();

        // MultipartFile로 변환
        return new InMemoryMultipartFile(originalFile.getName(), originalFile.getOriginalFilename(), "image/jpeg", compressedBytes);
    }

    /**
     * 확장자 가져오기
     * @param filename
     * @return
     */
    private String getFileExtension(String filename) {
        int dotIndex = filename.lastIndexOf('.');
        return (dotIndex != -1) ? filename.substring(dotIndex + 1) : "";
    }

}
