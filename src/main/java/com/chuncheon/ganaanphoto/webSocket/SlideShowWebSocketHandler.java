package com.chuncheon.ganaanphoto.webSocket;

import java.io.IOException;
import java.util.List;

import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import com.chuncheon.ganaanphoto.service.FileUploadService;

public class SlideShowWebSocketHandler extends TextWebSocketHandler {
    private final FileUploadService fileUploadService; // 파일 업로드 서비스

    // 파일 리스트를 관리하는 리스트 변수
    private List<String> fileList;

    // 생성자에서 서비스 주입
    public SlideShowWebSocketHandler(FileUploadService fileUploadService) {
        this.fileUploadService = fileUploadService;
    }

    // 클라이언트가 메시지를 보냈을 때 처리하는 메서드
    @Override
    public void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        String payload = message.getPayload();

        if ("start".equals(payload)) {
            // 슬라이드쇼 시작 시 파일 리스트 전송
            sendFileList(session);
        } else if ("update".equals(payload)) {
            // 파일 리스트 업데이트 후 전송
            updateFileListAndSend(session);
        }
    }

    // 파일 리스트를 클라이언트로 전송하는 메서드
    private void sendFileList(WebSocketSession session) throws IOException {
        // DB에서 최신순으로 파일명 가져오기
        fileList = fileUploadService.getSavedFileNamesFromDB();

        // 클라이언트로 전송
        String files = String.join(",", fileList);
        session.sendMessage(new TextMessage(files));
    }

    // 파일 리스트를 업데이트한 후 새 파일 리스트를 클라이언트에 전송
    private void updateFileListAndSend(WebSocketSession session) throws IOException {
        // 파일 리스트를 새로 업데이트 (예: 새로운 파일 추가)
        sendFileList(session); // 클라이언트로 새 파일 리스트 전송
    }
}
