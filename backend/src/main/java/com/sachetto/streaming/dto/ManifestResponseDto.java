package com.sachetto.streaming.dto;

import java.util.List;
import java.util.UUID;

public record ManifestResponseDto(
    UUID fileId,
    String fileName,
    Long fileSize,
    String fileHash,
    List<ChunkIndexResponseDto> chunks
) { }
