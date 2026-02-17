package com.sachetto.streaming.dto;

import java.util.UUID;

import org.springframework.web.multipart.MultipartFile;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record FullRequestDto(	
	@NotNull
	UUID uploadId,

	@NotNull
	MultipartFile file,

	@NotBlank
	String filename,

	@NotBlank
	String fileHash
) { }
