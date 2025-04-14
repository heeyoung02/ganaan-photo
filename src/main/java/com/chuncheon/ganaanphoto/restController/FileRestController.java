package com.chuncheon.ganaanphoto.restController;

import java.util.HashMap;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import com.chuncheon.ganaanphoto.service.FileUploadService;

@RestController

public class FileRestController {

    private final FileUploadService fileUploadService;

    // 생성자 주입
    public FileRestController(FileUploadService fileUploadService) {
        this.fileUploadService = fileUploadService;
    }

    @DeleteMapping("/rest/photo/deleteFile/{fileName}")
    public ResponseEntity<Map<String, Object>> deleteFile(@PathVariable String fileName) {
        boolean isDeleted = fileUploadService.deleteFileByFileName(fileName);  // DB 삭제

        Map<String, Object> response = new HashMap<>();
        response.put("success", isDeleted);

        return ResponseEntity.ok(response);
    }
}
