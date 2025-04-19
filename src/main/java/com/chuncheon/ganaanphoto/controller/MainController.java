package com.chuncheon.ganaanphoto.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

import com.chuncheon.ganaanphoto.service.FileUploadService;

@Controller
public class MainController {

    @Autowired
    private FileUploadService fileUploadService;

    /**
     * main page
     * @return
     */
    @GetMapping("/")
    public String main() {
        return "main";
    }

    /**
     * upload page
     * @return
     */
    @GetMapping("/upload")
    public String showUploadPage() {
        return "upload"; // upload.html 렌더링
    }

}
