package com.chuncheon.ganaanphoto.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

import jakarta.annotation.PostConstruct;

@Configuration
public class Config {

    private static Environment env; // 정적 필드

    // 생성자 주입
    @Autowired
    public Config(Environment env) {
        Config.env = env; // Environment를 정적 필드에 주입
    }

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

    /**
     * 확장자 허용 체크 여부
     */
    public static boolean checkAllowImg(String ext) throws Exception {
        if (getProperty("file.upload.img.allowed-extensions").contains(ext)) {
            return true;
        } else {
            return false;
        }
    }

}
