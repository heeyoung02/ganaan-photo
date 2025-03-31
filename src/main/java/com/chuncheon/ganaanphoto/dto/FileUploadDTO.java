package com.chuncheon.ganaanphoto.dto;

import lombok.Getter;
import lombok.Setter;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Getter
@Setter
public class FileUploadDTO {
    private List<MultipartFile> files;
}
