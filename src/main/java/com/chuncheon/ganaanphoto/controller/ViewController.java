package com.chuncheon.ganaanphoto.controller;

import java.io.File;
import java.util.List;

import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import com.chuncheon.ganaanphoto.config.Config;
import com.chuncheon.ganaanphoto.dto.FileUploadDTO;
import com.chuncheon.ganaanphoto.service.FileUploadService;

import lombok.RequiredArgsConstructor;

@Controller
@RequiredArgsConstructor
public class ViewController {

	private final FileUploadService fileUploadService;  // 파일 업로드 서비스

	@GetMapping("/view")
	public String showViewPage(Model model) {
		List<FileUploadDTO> fileUploadDTOList = fileUploadService.getUploadFileDTOFromDB();
		model.addAttribute("fileUploadDTOList", fileUploadDTOList);
		return "view";  // view.html 렌더링
	}

	@GetMapping("/slideShow")
	public String showSlideShow(Model model) {
		List<String> fileNames = fileUploadService.getSavedFileNamesFromDB();  // DB에서 최신순 파일 이름 가져오기
		model.addAttribute("fileList", fileNames);
		return "sseSlideShow";
	}

	/**
	 * 이미지 파일 반환
	 * @param fileName
	 * @return
	 */
	@GetMapping("/photo/{fileName}")
	public ResponseEntity<Resource> getImage(@PathVariable String fileName) {
		// DB에 저장된 경로를 기반으로 파일 생성
		String uploadDir = Config.getProperty("file.upload-dir");
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
