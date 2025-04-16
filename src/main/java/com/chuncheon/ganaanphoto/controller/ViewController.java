package com.chuncheon.ganaanphoto.controller;

import com.chuncheon.ganaanphoto.dto.FileUploadDTO;
import com.chuncheon.ganaanphoto.service.FileUploadService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.core.io.Resource;


import java.io.File;
import java.util.ArrayList;
import java.util.List;

@Controller
public class ViewController {

	@Autowired
	private FileUploadService fileUploadService;  // 파일 업로드 서비스

	@GetMapping("/view")
	public String showViewPage(Model model) {
		List<FileUploadDTO> fileUploadDTOList = fileUploadService.getUploadFileDTOFromDB();
		model.addAttribute("fileUploadDTOList", fileUploadDTOList);
		return "view";  // view.html 렌더링
	}

	// 이미지 파일을 반환하는 메서드
	@GetMapping("/photo/{fileName}")
	public ResponseEntity<Resource> getImage(@PathVariable String fileName) {
		// DB에 저장된 경로를 기반으로 파일 생성
		String uploadDir = "D:/upload/ganaan-photo/"; // 또는 Config.getProperty("file.upload-dir");
		File file = new File(uploadDir + fileName);

		if (file.exists()) {
			Resource resource = new FileSystemResource(file);
			String contentType = "image/jpeg";  // 기본은 jpeg. 확장자에 따라 다르게 하고 싶으면 로직 추가 가능
			return ResponseEntity.ok()
				.contentType(MediaType.parseMediaType(contentType))
				.header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + file.getName() + "\"")
				.body(resource);
		} else {
			return ResponseEntity.notFound().build();
		}
	}

}
