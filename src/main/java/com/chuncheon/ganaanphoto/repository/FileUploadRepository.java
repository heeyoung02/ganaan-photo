package com.chuncheon.ganaanphoto.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.chuncheon.ganaanphoto.entity.FileUploadEntity;

public interface FileUploadRepository extends JpaRepository<FileUploadEntity, Long> {

    List<FileUploadEntity> findAllByOrderByIdDesc();

    Optional<FileUploadEntity> findBySavedName(String savedName);

    List<FileUploadEntity> findAllBySavedNameIn(List<String> savedNames);
}
