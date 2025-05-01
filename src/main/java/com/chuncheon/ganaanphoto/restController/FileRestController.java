package com.chuncheon.ganaanphoto.restController;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.chuncheon.ganaanphoto.dto.FileUploadTask;
import com.chuncheon.ganaanphoto.service.FileUploadService;
import com.chuncheon.ganaanphoto.service.UploadQueueService;

import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
public class FileRestController {

    private final FileUploadService fileUploadService;
    private final UploadQueueService uploadQueueService;

    /**
     * 파일 업로드
     * @param files
     * @return
     */
    @PostMapping("/save")
    public ResponseEntity<String> uploadFiles(@RequestParam("files") List<MultipartFile> files) {

        int currentSize = uploadQueueService.getCurrentQueueSize();
        int maxCapacity = uploadQueueService.getMaxQueueCapacity();

        if (currentSize + files.size() > maxCapacity) {
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                .body("업로드 요청이 너무 많습니다. 잠시 후 다시 시도해주세요.");
        }

        for (MultipartFile file : files) {
            try {
                InputStream copiedInput = new BufferedInputStream(file.getInputStream()); // 스트리밍
                // // 큐에 등록하여 순차적으로 저장 처리
                uploadQueueService.enqueueFile(new FileUploadTask(file.getOriginalFilename(), copiedInput));
            } catch (IOException e) {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("파일 처리 실패: " + file.getOriginalFilename());
            }
        }
        return ResponseEntity.ok("업로드 요청이 접수되었습니다.");
    }

    /**
     * 단일파일 삭제
     * @param fileName
     * @return
     */
    @DeleteMapping("/rest/photo/deleteFile/{fileName}")
    public ResponseEntity<Map<String, Object>> deleteFile(@PathVariable("fileName") String fileName) {
        boolean isDeleted = fileUploadService.deleteFileByFileName(fileName);  // DB 삭제

        Map<String, Object> response = new HashMap<>();
        response.put("success", isDeleted);

        return ResponseEntity.ok(response);
    }

    /**
     * 다중파일 삭제
     * @param requestBody
     * @return
     */
    @DeleteMapping("/rest/photo/deleteSelected")
    public ResponseEntity<Map<String, Object>> deleteSelectedFiles(@RequestBody Map<String, Object> requestBody) {
        List<String> fileNames = (List<String>) requestBody.get("fileNames");
        boolean isDeleted = fileUploadService.deleteFilesByFileNames(fileNames);

        Map<String, Object> response = new HashMap<>();
        response.put("success", isDeleted);

        return ResponseEntity.ok(response);
    }


    //전체삭제(위험해서 주석처리함)
//    @DeleteMapping("/rest/photo/deleteAll")
//    public ResponseEntity<Map<String, Object>> deleteAllFiles() {
//        boolean isDeleted = fileUploadService.deleteAllFiles();
//
//        Map<String, Object> response = new HashMap<>();
//        response.put("success", isDeleted);
//
//        return ResponseEntity.ok(response);
//    }

}
