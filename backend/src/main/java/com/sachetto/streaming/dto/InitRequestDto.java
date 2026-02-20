package com.sachetto.streaming.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record InitRequestDto(
	@NotNull
	@Positive
	Long fileSize,
	
	@NotBlank
	String filename,
	
	@NotBlank
	String filecontent,
	
	@NotBlank
	String fileHash,
	
	@NotNull
	@Positive
	Long totalChunks
) { }
