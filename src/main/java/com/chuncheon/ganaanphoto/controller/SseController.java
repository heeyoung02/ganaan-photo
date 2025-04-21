package com.chuncheon.ganaanphoto.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import com.chuncheon.ganaanphoto.service.SseService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/sse")
@RequiredArgsConstructor
public class SseController {

	private final SseService sseService;


	/**
	 * server sent events 생성
	 * @return
	 */
	@GetMapping("/subscribe")
	public SseEmitter subscribe() {
		return sseService.addEmitter();
	}

}
