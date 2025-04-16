package com.chuncheon.ganaanphoto.restController;

import java.io.File;

import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import com.chuncheon.ganaanphoto.config.Config;

@RestController

public class ViewRestController {
    // 이미지 파일을 반환하는 API
    @GetMapping("/rest/photo/{fileName}")
    public ResponseEntity<Resource> getFile(@PathVariable String fileName) {
        File file = new File(Config.getProperty("file.upload-dir") + "/" + fileName);

        if (file.exists()) {
            Resource resource = new FileSystemResource(file);

            // 확장자에 따른 MIME 타입 설정
            String contentType = "image/jpeg";  // 기본적으로 JPEG 이미지로 설정
            if (fileName.endsWith(".png")) {
                contentType = "image/png";
            } else if (fileName.endsWith(".gif")) {
                contentType = "image/gif";
            }

            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(contentType))  // 이미지 MIME 타입 설정
                    .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + file.getName() + "\"")  // 파일 이름을 헤더에 설정
                    .body(resource);
        } else {
            return ResponseEntity.notFound().build();  // 파일이 존재하지 않으면 404 응답
        }
    }
}
