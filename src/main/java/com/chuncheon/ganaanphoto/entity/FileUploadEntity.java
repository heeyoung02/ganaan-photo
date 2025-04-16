package com.chuncheon.ganaanphoto.entity;

import java.time.LocalDateTime;

import com.chuncheon.ganaanphoto.dto.FileUploadDTO;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "photo_files")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class FileUploadEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "ORG_NM", nullable = false)
    private String originalName;

    @Column(name = "SAV_NM", nullable = false)
    private String savedName;

    @Column(name = "FILE_EXT", nullable = false)
    private String fileExtension;

    @Column(name = "FILE_PATH", nullable = false)
    private String filePath;

    @Column(name = "REG_DT", columnDefinition = "DATETIME DEFAULT CURRENT_TIMESTAMP")
    private LocalDateTime regDt;

    public static FileUploadDTO fromEntity(FileUploadEntity entity) {
        return FileUploadDTO.builder()
            .id(entity.getId())
            .originalName(entity.getOriginalName())
            .savedName(entity.getSavedName())
            .fileExtension(entity.getFileExtension())
            .filePath(entity.getFilePath())
            .regDt(entity.getRegDt())
            .build();
    }
}
