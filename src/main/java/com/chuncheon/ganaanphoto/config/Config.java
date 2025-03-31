package com.chuncheon.ganaanphoto.config;

import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

@Configuration
public class Config {

    private static Environment env; // 정적 필드

    // 생성자 주입
    @Autowired
    public Config(Environment env) {
        Config.env = env; // Environment를 정적 필드에 주입
    }

    // @PostConstruct로 초기화 메서드 설정
    @PostConstruct
    public void init() {
        // 초기화 작업 (필요시 추가 설정)
    }

    // 프로퍼티 키를 받아서 값 반환
    public static String getProperty(String key) {
        return env.getProperty(key);
    }

    public static int getProperty(String key, int defaultVal) {
        try {
            return Integer.parseInt(env.getProperty(key));
        } catch (NumberFormatException | NullPointerException e) {
            return defaultVal;
        }
    }

}
