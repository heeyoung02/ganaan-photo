package com.chuncheon.ganaanphoto.dto;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.web.multipart.MultipartFile;

import com.chuncheon.ganaanphoto.entity.FileUploadEntity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

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
    // 파일 복제 위한 항목
    private byte[] content;

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
