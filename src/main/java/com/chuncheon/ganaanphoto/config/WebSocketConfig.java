package com.chuncheon.ganaanphoto.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

import com.chuncheon.ganaanphoto.service.FileUploadService;
import com.chuncheon.ganaanphoto.webSocket.SlideShowWebSocketHandler;

@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {
    private final FileUploadService fileUploadService;

    // 생성자를 이용한 의존성 주입 (Spring이 자동으로 주입)
    public WebSocketConfig(FileUploadService fileUploadService) {
        this.fileUploadService = fileUploadService;
    }

    @Bean
    public SlideShowWebSocketHandler slideShowWebSocketHandler() {
        return new SlideShowWebSocketHandler(fileUploadService);
    }

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(slideShowWebSocketHandler(), "/ws/files").setAllowedOrigins("*");
    }
}
