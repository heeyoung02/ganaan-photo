package com.chuncheon.ganaanphoto.restController;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import com.chuncheon.ganaanphoto.dto.FileUploadDTO;
import com.chuncheon.ganaanphoto.service.FileUploadService;

@RestController
public class FileRestController {

    @Autowired
    private FileUploadService fileUploadService;

    @PostMapping("/save")
    public ResponseEntity<String> uploadFiles(@ModelAttribute FileUploadDTO fileUploadDTO) {
        try {
            fileUploadService.processUploadFiles(fileUploadDTO.getFiles());
            return ResponseEntity.ok("파일 업로드 성공!");
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("파일 업로드 실패: " + e.getMessage());
        }
    }

    /**
     * 단일파일 삭제
     * @param fileName
     * @return
     */
    @DeleteMapping("/rest/photo/deleteFile/{fileName}")
    public ResponseEntity<Map<String, Object>> deleteFile(@PathVariable String fileName) {
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
        // Map에서 fileNames를 List<String>으로 받아옵니다.
        List<String> fileNames = (List<String>) requestBody.get("fileNames");
        boolean isDeleted = fileUploadService.deleteFilesByFileNames(fileNames);  // DB에서 여러 파일 삭제

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
