package com.sachetto.streaming.service;

import java.io.InputStream;
import java.util.UUID;

import org.springframework.core.io.Resource;

public interface StorageService {
	String upload(UUID uploadId, Long index, InputStream file);
	Resource load(String path);
}
