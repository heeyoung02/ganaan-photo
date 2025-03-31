package com.chuncheon.ganaanphoto.service;

import com.chuncheon.ganaanphoto.config.Config;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class FileUploadService {

    public void saveFiles(List<MultipartFile> files) throws IOException {
        
        String uploadDir = Config.getProperty("file.upload-dir"); // 업로드 경로

        // 저장 폴더 없으면 생성
        Files.createDirectories(Paths.get(uploadDir));

        for (MultipartFile file : files) {
            if (!file.isEmpty()) {
                // 파일명 랜덤 UUID + 확장자 유지
                String fileName = UUID.randomUUID() + "_" + file.getOriginalFilename();
                File saveFile = new File(uploadDir, fileName);

                file.transferTo(saveFile);
            }
        }
    }
}
