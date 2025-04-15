package com.chuncheon.ganaanphoto.service;

import com.chuncheon.ganaanphoto.config.Config;
import com.chuncheon.ganaanphoto.dto.FileUploadDTO;
import com.chuncheon.ganaanphoto.entity.FileUploadEntity;
import com.chuncheon.ganaanphoto.repository.FileUploadRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class FileUploadService {

    private final FileUploadRepository fileUploadRepository;

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

                File saveFile = new File(uploadDir, savedName);
                file.transferTo(saveFile);

                // DB에 저장
                FileUploadEntity entity = FileUploadEntity.builder()
                    .originalName(originalName)
                    .savedName(savedName)
                    .fileExtension(extension)
                    .filePath(uploadDir)
                    .regDt(null) // DB에서 CURRENT_TIMESTAMP 자동 생성
                    .build();

                fileUploadRepository.save(entity);
            }
        }
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
        System.out.println("삭제 시도 파일명: " + savedName);  // ✅ 로그로 확인
        Optional<FileUploadEntity> fileOpt = fileUploadRepository.findBySavedName(savedName);

        if (fileOpt.isPresent()) {
            System.out.println("삭제 대상 있음. 삭제 진행.");  // ✅ 로그
            fileUploadRepository.delete(fileOpt.get());
            return true;
        } else {
            System.out.println("삭제 대상 없음.");  // ✅ 로그
            return false;
        }
    }

    //    // 파일 목록을 생성 시간 기준으로 정렬
    //    public List<File> getFilesSortedByCreationTime() {
    //        File directory = new File(Config.getProperty("file.upload-dir"));
    //        File[] files = directory.listFiles();
    //
    //        if (files == null) {
    //            return Collections.emptyList();
    //        }
    //
    //        // 파일 생성 시간 기준으로 정렬
    //        return Arrays.stream(files)
    //                .sorted((file1, file2) -> Long.compare(file2.lastModified(), file1.lastModified())) // 내림차순 정렬
    //                .collect(Collectors.toList());
    //    }

    //여러 파일을 저장된 이름을 기준으로 삭제하는 메서드
    public boolean deleteFilesByFileNames(List<String> savedNames) {
        try {
            // 여러 파일을 삭제하는 로직
            List<FileUploadEntity> filesToDelete = fileUploadRepository.findAllBySavedNameIn(savedNames);

            if (filesToDelete.isEmpty()) {
                return false;
            }

            // 파일 삭제
            fileUploadRepository.deleteAll(filesToDelete);
            return true;
        } catch (Exception e) {
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
}
