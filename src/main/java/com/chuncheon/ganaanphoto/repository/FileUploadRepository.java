package com.chuncheon.ganaanphoto.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import com.chuncheon.ganaanphoto.entity.FileUploadEntity;

public interface FileUploadRepository extends JpaRepository<FileUploadEntity, Long> {

    List<FileUploadEntity> findAllByOrderByIdDesc();

    Optional<FileUploadEntity> findBySavedName(String savedName);

    List<FileUploadEntity> findAllBySavedNameIn(List<String> savedNames);

    // 페이지네이션을 위한 메서드
    Page<FileUploadEntity> findAll(Pageable pageable);
    long count();
}
