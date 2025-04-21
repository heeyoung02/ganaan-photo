package com.chuncheon.ganaanphoto.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class MainController {

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
