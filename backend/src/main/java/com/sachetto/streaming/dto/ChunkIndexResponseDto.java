package com.sachetto.streaming.dto;

public record ChunkIndexResponseDto(
    Long index,
    String hash
) { }
