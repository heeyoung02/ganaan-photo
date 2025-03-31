package com.chuncheon.ganaanphoto.controller;

import com.chuncheon.ganaanphoto.dto.FileUploadDTO;
import com.chuncheon.ganaanphoto.service.FileUploadService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@Controller
public class MainController {
    @Autowired
    private FileUploadService fileUploadService;

    @GetMapping("/main")
    public String main() {
        return "main";
    }

    @GetMapping("/upload")
    public String showUploadPage() {
        return "upload"; // upload.html 렌더링
    }

    @PostMapping("/upload")
    public String uploadFiles(@ModelAttribute FileUploadDTO fileUploadDTO, Model model) {
        List<MultipartFile> files = fileUploadDTO.getFiles();

        // 파일 개수 제한
        if (files.size() > 10) {
            model.addAttribute("message", "최대 10개의 파일만 업로드 가능합니다.");
            return "upload";
        }

        // 파일 크기 제한
        for (MultipartFile file : files) {
            if (file.getSize() > 5 * 1024 * 1024) { // 5MB 제한
                model.addAttribute("message", "파일 크기는 5MB를 초과할 수 없습니다.");
                return "upload";
            }
        }
        try {
            fileUploadService.saveFiles(files);
            model.addAttribute("message", "파일 업로드 성공!");
        } catch (IOException e) {
            model.addAttribute("message", "파일 업로드 실패: " + e.getMessage());
        }

        return "upload";
    }
}
