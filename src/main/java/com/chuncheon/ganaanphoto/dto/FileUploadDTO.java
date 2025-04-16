package com.chuncheon.ganaanphoto.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.Date;
import java.util.List;

import com.chuncheon.ganaanphoto.entity.FileUploadEntity;

@Data
@ToString
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class FileUploadDTO {
    private List<MultipartFile> files;
    private Long id;
    private String originalName;
    private String savedName;
    private String fileExtension;
    private String filePath;
    private LocalDateTime regDt;

    public FileUploadEntity toEntity() {
        return FileUploadEntity.builder()
            .id(id)
            .originalName(originalName)
            .savedName(savedName)
            .fileExtension(fileExtension)
            .filePath(filePath)
            .regDt(regDt)
            .build();
    }
}
