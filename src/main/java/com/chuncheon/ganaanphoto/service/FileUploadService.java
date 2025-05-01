package com.chuncheon.ganaanphoto.service;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URLEncoder;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import com.chuncheon.ganaanphoto.config.Config;
import com.chuncheon.ganaanphoto.dto.FileUploadDTO;
import com.chuncheon.ganaanphoto.entity.FileUploadEntity;
import com.chuncheon.ganaanphoto.repository.FileUploadRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;

@Service
@RequiredArgsConstructor
@Log4j2
public class FileUploadService {

    @Value("${file.upload-dir}")
    private String uploadDir;

    private final FileUploadRepository fileUploadRepository;
    private final SseService sseService;

    /**
     * 파일 및 db 삭제
     * @param savedName
     * @throws IOException
     */
	public void deleteFile(String savedName) throws IOException {
		String uploadDir = Config.getProperty("file.upload-dir");
		if (uploadDir == null || uploadDir.isBlank()) {
			throw new IllegalArgumentException("파일 업로드 경로가 설정되지 않았습니다.");
		}

		// DB에서 해당 파일 정보 조회
		Optional<FileUploadEntity> optional = fileUploadRepository.findBySavedName(savedName);
		if (optional.isEmpty()) {
			throw new FileNotFoundException("해당 파일이 존재하지 않습니다: " + savedName);
		}

		// DB 삭제
		FileUploadEntity entity = optional.get();
		fileUploadRepository.delete(entity);

		// 파일 삭제
		File file = new File(uploadDir, savedName);
		if (file.exists()) {
			if (!file.delete()) {
				throw new IOException("파일 삭제에 실패했습니다: " + file.getAbsolutePath());
			}
		}

		// 4. SSE로 삭제 알림 전송 (프론트에서 해당 이미지 제거하도록)
		String imageUrl = "/rest/photo/" + URLEncoder.encode(savedName, "UTF-8").replaceAll("\\+", "%20");
		sseService.broadcastImageDelete(imageUrl);
	}

    /**
     * 최신순으로 저장된 파일 이름 list 가져오기
     * @return
     */
    public List<String> getSavedFileNamesFromDB() {
        return fileUploadRepository.findAllByOrderByIdDesc()
            .stream()
            .map(FileUploadEntity::getSavedName)
            .collect(Collectors.toList());
    }

    /**
     * 최신순으로 저장된 file upload dto list 가져오기
     * @return
     */
    public List<FileUploadDTO> getUploadFileDTOFromDB() {
        return fileUploadRepository.findAllByOrderByIdDesc().stream().map(FileUploadEntity::fromEntity).collect(Collectors.toList());
    }

    /**
     * 파일 이름으로 데이터 삭제
     * @param savedName
     * @return
     */
    public boolean deleteFileByFileName(String savedName) {
        try {
            this.deleteFile(savedName);
            return true;
        } catch (IOException e) {
            log.error("[failed to delete file!] " + e.getMessage());
            return false;
		}
    }

    /**
     * 여러 파일을 저장된 이름을 기준으로 데이터 삭제
     * @param savedNames
     * @return
     */
    public boolean deleteFilesByFileNames(List<String> savedNames) {
        try{
            for(String savedName : savedNames){
                this.deleteFile(savedName);
            }
            return true;
        } catch (IOException e) {
			log.error("[failed to delete files!] " + e.getMessage());
            return false;
		}
    }

    // 모든 파일을 삭제하는 메서드 (위험해서 주석처리함)
    //    public boolean deleteAllFiles() {
    //        try {
    //            List<FileUploadEntity> allFiles = fileUploadRepository.findAll(); // 모든 파일 가져오기
    //            if (allFiles.isEmpty()) {
    //                return false;
    //            }
    //
    //            fileUploadRepository.deleteAll(allFiles); // 모든 파일 삭제
    //            return true;
    //        } catch (Exception e) {
    //            return false;
    //        }
    //    }


    /**
     * 확장자 가져오기
     * @param filename
     * @returnd
     */
    public String getFileExtension(String filename) {
        int dotIndex = filename.lastIndexOf('.');
        return (dotIndex != -1) ? filename.substring(dotIndex + 1) : "";
    }

    public Page<FileUploadDTO> getPaginatedFiles(int pageNumber, int pageSize) {
        Pageable pageable = PageRequest.of(pageNumber - 1, pageSize);  // pageNumber는 1부터 시작하므로 -1 해줌
        Page<FileUploadEntity> page = fileUploadRepository.findAll(pageable);
        return page.map(FileUploadEntity::fromEntity);  // Entity를 DTO로 변환
    }
}
