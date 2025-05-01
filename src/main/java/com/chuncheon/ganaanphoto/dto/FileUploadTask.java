package com.chuncheon.ganaanphoto.dto;

import java.io.InputStream;

public record FileUploadTask(String originalName, InputStream inputStream) {}
