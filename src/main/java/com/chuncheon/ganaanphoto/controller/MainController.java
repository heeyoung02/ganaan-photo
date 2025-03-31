package com.chuncheon.ganaanphoto.controller;

import com.chuncheon.ganaanphoto.config.Config;
import com.chuncheon.ganaanphoto.dto.FileUploadDTO;
import com.chuncheon.ganaanphoto.service.FileUploadService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

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

    @PostMapping("/save")
    public String uploadFiles(@ModelAttribute FileUploadDTO fileUploadDTO, Model model, RedirectAttributes redirectAttributes) {
        List<MultipartFile> files = fileUploadDTO.getFiles();

        // 설정값 가져오기
        int maxCount = Config.getProperty("file.max-count", 10); // 최대 업로드 파일 갯수
        long defaultAllowSize = Config.getProperty("file.default-allow-size", 1048576); // 기본 1MB
        long maxSize = Config.getProperty("file.max-size", 5) * defaultAllowSize; // 최대 업로드 파일 크기 (5MB)

        // 파일 개수 제한
        if (files.size() > maxCount) {
            redirectAttributes.addFlashAttribute("message", "최대 " + maxCount + "개의 파일만 업로드 가능합니다.");
            return "redirect:/upload"; // 업로드 페이지로 리다이렉션
        }

        // 파일 크기 제한
        for (MultipartFile file : files) {
            if (file.getSize() > maxSize) { // 5MB 제한
                redirectAttributes.addFlashAttribute("message", "파일 크기는 " + maxSize / 1048576 + "MB를 초과할 수 없습니다.");
                return "redirect:/upload"; // 업로드 페이지로 리다이렉션
            }
        }
        try {
            fileUploadService.saveFiles(files);
            redirectAttributes.addFlashAttribute("message", "파일 업로드 성공!");
        } catch (IOException e) {
            redirectAttributes.addFlashAttribute("message", "파일 업로드 실패: " + e.getMessage());
        }

        return "redirect:upload";
    }
}
