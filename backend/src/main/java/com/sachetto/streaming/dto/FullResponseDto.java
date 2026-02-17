package com.sachetto.streaming.dto;

import java.util.List;
import java.util.UUID;

public record FullResponseDto(
	UUID arquivoId,
	List<String> chunksPath
) { }
