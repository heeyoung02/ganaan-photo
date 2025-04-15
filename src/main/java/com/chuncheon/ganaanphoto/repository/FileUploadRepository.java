package com.chuncheon.ganaanphoto.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.chuncheon.ganaanphoto.entity.FileUploadEntity;

public interface FileUploadRepository extends JpaRepository<FileUploadEntity, Long> {
    List<FileUploadEntity> findAllByOrderByIdDesc();

    // 파일 이름으로 하나만 삭제
    Optional<FileUploadEntity> findBySavedName(String savedName);

    // 파일 이름 리스트를 받아서 파일을 삭제하는 메서드
    List<FileUploadEntity> findAllBySavedNameIn(List<String> savedNames);
}
