package com.chuncheon.ganaanphoto.controller;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Page;
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
import org.springframework.web.bind.annotation.RequestParam;

@Controller
@RequiredArgsConstructor
public class ViewController {

	private final FileUploadService fileUploadService;  // 파일 업로드 서비스

	//페이지네이션이 추가된 /view
	@GetMapping("/view")
	public String showViewPage(@RequestParam(value = "pageNumber", defaultValue = "1") int pageNumber,
							   @RequestParam(value = "pageSize", defaultValue = "15") int pageSize, Model model) {
		Page<FileUploadDTO> fileUploadPage = fileUploadService.getPaginatedFiles(pageNumber, pageSize);
		model.addAttribute("fileUploadDTOList", fileUploadPage.getContent());
		model.addAttribute("currentPage", pageNumber);
		model.addAttribute("totalPages", fileUploadPage.getTotalPages());
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
	public ResponseEntity<Resource> getImage(@PathVariable("fileName") String fileName) {
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

	/**
	 * 파일 목록을 페이지네이션하여 가져옵니다.
	 * @param pageNumber 페이지 번호 (기본값 1)
	 * @param pageSize 페이지 크기 (기본값 10)
	 * @return 페이지네이션된 파일 목록
	 */
	@GetMapping("/rest/photo/files")
	public ResponseEntity<Page<FileUploadDTO>> getFiles(
			@RequestParam(defaultValue = "1") int pageNumber,
			@RequestParam(defaultValue = "10") int pageSize) {

		Page<FileUploadDTO> filePage = fileUploadService.getPaginatedFiles(pageNumber, pageSize);
		return ResponseEntity.ok(filePage);
	}
}
