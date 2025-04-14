package com.chuncheon.ganaanphoto.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import com.chuncheon.ganaanphoto.service.FileUploadService;

@Controller
public class SlideShowController {

    @Autowired
    private FileUploadService fileUploadService;

    @GetMapping("/slideShow")
    public String showSlideShow(Model model) {
        List<String> fileNames = fileUploadService.getSavedFileNamesFromDB();  // DB에서 최신순 파일 이름 가져오기
        model.addAttribute("fileList", fileNames);
        return "slideShow";  // slideShow.html 렌더링
    }
    // 파일 이름 목록을 JSON으로 반환하는 API
    @GetMapping("/getUploadedFiles")
    @ResponseBody
    public List<String> getUploadedFiles() {
        return fileUploadService.getSavedFileNamesFromDB();  // DB에서 최신순 파일 이름 반환
    }
}
