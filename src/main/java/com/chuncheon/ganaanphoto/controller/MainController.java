package com.chuncheon.ganaanphoto.controller;

import com.chuncheon.ganaanphoto.config.Config;
import com.chuncheon.ganaanphoto.dto.FileUploadDTO;
import com.chuncheon.ganaanphoto.service.FileUploadService;
import com.chuncheon.ganaanphoto.utils.InMemoryMultipartFile;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
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

    //form으로 받기
//    @PostMapping("/save")
//    public String uploadFiles(@ModelAttribute FileUploadDTO fileUploadDTO, Model model, RedirectAttributes redirectAttributes) {
//
//        List<MultipartFile> files = fileUploadDTO.getFiles();
//
//        // 설정값 가져오기
//        int maxCount = Config.getProperty("file.max-count", 10); // 최대 업로드 파일 갯수
//        long defaultAllowSize = Config.getProperty("file.default-allow-size", 1048576); // 기본 1MB
//        long maxSize = Config.getProperty("file.max-size", 5) * defaultAllowSize; // 최대 업로드 파일 크기 (5MB)
//
//        // 파일 개수 제한
//        if (files.size() > maxCount) {
//            redirectAttributes.addFlashAttribute("message", "최대 " + maxCount + "개의 파일만 업로드 가능합니다.");
//            return "redirect:/upload"; // 업로드 페이지로 리다이렉션
//        }
//        ///////////////////////////////////////////////
//        // 파일 크기 제한
//        //        for (MultipartFile file : files) {
//        //            if (file.getSize() > maxSize) { // 5MB 제한
//        //                redirectAttributes.addFlashAttribute("message", "파일 크기는 " + maxSize / 1048576 + "MB를 초과할 수 없습니다.");
//        //                return "redirect:/upload"; // 업로드 페이지로 리다이렉션
//        //            }
//        //        }
//        //        try {
//        //            fileUploadService.saveFiles(files);
//        //            redirectAttributes.addFlashAttribute("message", "파일 업로드 성공!");
//        //        } catch (IOException e) {
//        //            redirectAttributes.addFlashAttribute("message", "파일 업로드 실패: " + e.getMessage());
//        //        }
//        //
//        //        return "redirect:upload";
//        ///////////////////////////////////////////////
//
//        for (MultipartFile file : files) {
//            try {
//                // 1️⃣ QHD(2560px)로 리사이징 후 저장
//                MultipartFile resizedFile = resizeImage(file, 2560);
//
//                // 2️⃣ 파일 크기 제한 체크
//                if (resizedFile.getSize() > maxSize) {
//                    redirectAttributes.addFlashAttribute("message", "파일 크기는 " + maxSize / 1048576 + "MB를 초과할 수 없습니다.");
//                    return "redirect:/upload";
//                }
//
//                // 3️⃣ 파일 저장
//                fileUploadService.saveFiles(List.of(resizedFile));
//
//            } catch (IOException e) {
//                redirectAttributes.addFlashAttribute("message", "파일 업로드 실패: " + e.getMessage());
//                return "redirect:/upload";
//            }
//        }
//
//        redirectAttributes.addFlashAttribute("message", "파일 업로드 성공!");
//        return "redirect:/upload";
//    }

    //fetch로 받기
    @PostMapping("/save")
    @ResponseBody
    public ResponseEntity<String> uploadFiles(@ModelAttribute FileUploadDTO fileUploadDTO) {
        List<MultipartFile> files = fileUploadDTO.getFiles();

        int maxCount = Config.getProperty("file.max-count", 10);
        long defaultAllowSize = Config.getProperty("file.default-allow-size", 1048576);
        long maxSize = Config.getProperty("file.max-size", 5) * defaultAllowSize;

        if (files.size() > maxCount) {
            return ResponseEntity.badRequest().body("최대 " + maxCount + "개의 파일만 업로드 가능합니다.");
        }

        for (MultipartFile file : files) {
            try {
                // 1️⃣ QHD(2560px)로 리사이징
                MultipartFile resizedFile = resizeImage(file, 2560);

                // 2️⃣ 크기 체크
                if (resizedFile.getSize() > maxSize) {
                    return ResponseEntity.badRequest().body("파일 크기는 " + maxSize / 1048576 + "MB를 초과할 수 없습니다.");
                }

                // 3️⃣ 파일 저장
                fileUploadService.saveFiles(List.of(resizedFile));

            } catch (IOException e) {
                return ResponseEntity.internalServerError().body("파일 업로드 실패: " + e.getMessage());
            }
        }

        return ResponseEntity.ok("파일 업로드 성공!");

//        return "redirect:/upload";
    }

    private MultipartFile resizeImage(MultipartFile originalFile, int targetWidth) throws IOException {
        InputStream input = originalFile.getInputStream();
        BufferedImage originalImage = ImageIO.read(input);

        // 기존 이미지 크기 가져오기
        int width = originalImage.getWidth();
        int height = originalImage.getHeight();

        // 리사이징할 크기 계산
        double scale = (double) targetWidth / width;
        int newWidth = targetWidth;
        int newHeight = (int) (height * scale);

        // 새 BufferedImage 생성
        BufferedImage resizedImage = new BufferedImage(newWidth, newHeight, BufferedImage.TYPE_INT_RGB);
        Graphics2D g2d = resizedImage.createGraphics();
        g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
        g2d.drawImage(originalImage, 0, 0, newWidth, newHeight, null);
        g2d.dispose();

        // 압축하여 ByteArrayOutputStream에 저장
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(resizedImage, "jpg", baos);
        byte[] compressedBytes = baos.toByteArray();

        // MultipartFile로 변환
        return new InMemoryMultipartFile(originalFile.getName(), originalFile.getOriginalFilename(), "image/jpeg", compressedBytes);
    }
}
