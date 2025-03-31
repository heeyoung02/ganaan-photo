package com.chuncheon.ganaanphoto.service;

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
    private final String UPLOAD_DIR = "D:/upload/ganaan-photo"; // 로컬 저장 경로

    public void saveFiles(List<MultipartFile> files) throws IOException {
        // 저장 폴더 없으면 생성
        Files.createDirectories(Paths.get(UPLOAD_DIR));

        for (MultipartFile file : files) {
            if (!file.isEmpty()) {
                // 파일명 랜덤 UUID + 확장자 유지
                String fileName = UUID.randomUUID() + "_" + file.getOriginalFilename();
                File saveFile = new File(UPLOAD_DIR, fileName);

                file.transferTo(saveFile);
            }
        }
    }
}
