package com.chuncheon.ganaanphoto.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import com.chuncheon.ganaanphoto.config.Config;

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
    public String showUploadPage(Model model) {
        // properties에 설정된 최대 업로드갯수, 허용용량 전송
        model.addAttribute("maxFileCount", Config.getProperty("file.max-count"));
        model.addAttribute("maxFileSize", Config.getProperty("file.max-size"));
        return "upload";
    }

}
