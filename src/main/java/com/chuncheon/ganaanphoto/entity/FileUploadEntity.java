package com.chuncheon.ganaanphoto.entity;

import java.time.LocalDateTime;

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

//    1. DB에서 시간 생성 (추천 상황 많음)
    @Column(name = "REG_DT", columnDefinition = "DATETIME DEFAULT CURRENT_TIMESTAMP")
    private LocalDateTime regDt;

//    2. Java (Spring) 코드에서 시간 생성
//    @Column(name = "reg_dt", columnDefinition = "DATETIME DEFAULT CURRENT_TIMESTAMP")
//    private LocalDateTime regDt = LocalDateTime.now();
}
