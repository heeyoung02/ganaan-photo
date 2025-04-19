package com.chuncheon.ganaanphoto.controller;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@RestController
@RequestMapping("/sse")
public class SseController {

	private final List<SseEmitter> emitters = new CopyOnWriteArrayList<>();

	/**
	 * server sent events 생성
	 * @return
	 */
	@GetMapping("/subscribe")
	public SseEmitter subscribe() {
		SseEmitter emitter = new SseEmitter(0L); // 무제한 타임아웃
		emitters.add(emitter);

		emitter.onCompletion(() -> emitters.remove(emitter));
		emitter.onTimeout(() -> emitters.remove(emitter));
		emitter.onError(e -> emitters.remove(emitter));

		return emitter;
	}

	/**
	 * event 발생시 client에게 전송
	 * @param imageUrl
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
}
