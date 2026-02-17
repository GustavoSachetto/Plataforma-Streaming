package com.sachetto.streaming.dto;

import java.util.List;
import java.util.UUID;

public record CompleteResponseDto(
	UUID arquivoId,
	List<String> chunksPath
) { }
