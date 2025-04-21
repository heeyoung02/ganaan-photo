package com.chuncheon.ganaanphoto.service;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@Service
public class SseService {

	private final List<SseEmitter> emitters = new CopyOnWriteArrayList<>();

	/**
	 * 클라이언트가 SSE 구독
	 */
	public SseEmitter addEmitter() {
		SseEmitter emitter = new SseEmitter(0L); // 무제한 타임아웃
		emitters.add(emitter);

		emitter.onCompletion(() -> emitters.remove(emitter));
		emitter.onTimeout(() -> emitters.remove(emitter));
		emitter.onError(e -> emitters.remove(emitter));

		return emitter;
	}

	/**
	 * 새로운 이미지가 업로드될 때 클라이언트에게 이벤트 전송
	 */
	public void broadcastNewImage(String imageUrl) {
		for (SseEmitter emitter : emitters) {
			try {
				emitter.send(SseEmitter.event()
					.name("imageUpdate")
					.data(imageUrl));
			} catch (IOException e) {
				emitter.completeWithError(e);
				emitters.remove(emitter);
			}
		}
	}

	/**
	 * 60초마다 핑 전송 스케쥴러 (sse끊킴 방지)
	 */
	@Scheduled(fixedRate = 60000)
	public void sendPingToAllClients() {
		System.out.println("서버 → 클라이언트 ping 전송: " + LocalDateTime.now());
		for (SseEmitter emitter : emitters) {
			try {
				emitter.send(SseEmitter.event()
					.name("ping")
					.data("keep-alive"));
			} catch (IOException e) {
				emitter.completeWithError(e);
				emitters.remove(emitter);
			}
		}
	}

}
