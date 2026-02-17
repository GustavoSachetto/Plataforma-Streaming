package com.sachetto.streaming.dto;

import java.util.UUID;

import jakarta.validation.constraints.NotNull;

public record CompleteRequestDto(
	@NotNull
	UUID uploadId
) { }
