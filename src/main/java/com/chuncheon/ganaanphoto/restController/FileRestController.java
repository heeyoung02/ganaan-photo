package com.chuncheon.ganaanphoto.restController;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.chuncheon.ganaanphoto.dto.FileUploadDTO;
import com.chuncheon.ganaanphoto.service.FileUploadService;

import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
public class FileRestController {

    private final FileUploadService fileUploadService;


    @PostMapping("/save")
    public ResponseEntity<String> uploadFiles(@RequestParam("files") List<MultipartFile> files) throws Exception {
        List<FileUploadDTO> copiedFiles = new ArrayList<>();

        for (MultipartFile file : files) {
            if (!file.isEmpty()) {
                copiedFiles.add(FileUploadDTO.builder()
                    .originalName(file.getOriginalFilename())
                    .content(file.getBytes())
                    .build());
            }
        }
        // 비동기로 복제된 파일 넘기기(복제하고 넘겨야 temp에서 multipartfile이 사라져서 업로드 실패하는것을 방지할수있다)
        fileUploadService.processUploadFilesAsync(copiedFiles);

        return ResponseEntity.ok("파일 업로드 성공");
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
