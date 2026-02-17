package com.sachetto.streaming.dto;

import java.util.UUID;

import org.springframework.web.multipart.MultipartFile;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record ChunkRequestDto(
	@NotNull
	@Min(0)
	Long index,
	
	@NotNull
	UUID uploadId,
	
	@NotBlank
	String chunkHash,
	
	@NotNull
	MultipartFile file
) { }
