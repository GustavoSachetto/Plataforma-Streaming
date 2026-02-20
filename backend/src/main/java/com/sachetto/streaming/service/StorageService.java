package com.sachetto.streaming.service;

import java.io.InputStream;
import java.util.UUID;

import org.springframework.core.io.Resource;
import org.springframework.web.multipart.MultipartFile;

public interface StorageService {
	String upload(UUID uploadId, Long index, InputStream file);
	String saveThumbnail(UUID uploadId, MultipartFile file);
	Resource load(String path);
}
