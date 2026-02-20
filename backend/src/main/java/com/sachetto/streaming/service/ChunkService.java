package com.sachetto.streaming.service;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import com.sachetto.streaming.exception.ArquivoIOException;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChunkService {

    private final StringRedisTemplate stringRedisTemplate;

    public void registerUpload(UUID uploadId, Long totalChunks) {
        log.debug("Criando registro de upload de chunks no Redis: uploadId={}, totalChunks={}", uploadId, totalChunks);
        stringRedisTemplate.opsForHash().put("upload:" + uploadId, "total", String.valueOf(totalChunks));
    }

    public void registerChunk(UUID uploadId, Long chunkIndex) {
        log.debug("Registrando chunk {} no Redis para uploadId={}", chunkIndex, uploadId);
        String redisKeyChunks = "upload:" + uploadId + ":chunks";
        stringRedisTemplate.opsForSet().add(redisKeyChunks, String.valueOf(chunkIndex));
    }

    public List<String> validateAndGetChunkPaths(UUID uploadId) {
        String uploadKey = "upload:" + uploadId;
        String chunksKey = uploadKey + ":chunks";

        Long scard = stringRedisTemplate.opsForSet().size(chunksKey);
        Long totalChunksInRedis = scard != null ? scard : 0L;
        
        Object totalStrObj = stringRedisTemplate.opsForHash().get(uploadKey, "total");
        Long expectedChunks = totalStrObj != null ? Long.parseLong(totalStrObj.toString()) : 0L;

        if (!totalChunksInRedis.equals(expectedChunks)) {
            log.error("Upload incompleto para {}. Esperado: {}, Encontrado: {}", uploadId, expectedChunks, totalChunksInRedis);
            throw new ArquivoIOException();
        }

        List<String> chunksPath = new ArrayList<>();
        for (long i = 1; i <= expectedChunks; i++) {
            Path chunkPath = Paths.get("uploads", uploadId.toString(), i + ".mp4");
            chunksPath.add(chunkPath.toAbsolutePath().toString());
        }

        return chunksPath;
    }

    public void cleanup(UUID uploadId) {
        log.debug("Limpando metadados do Redis para uploadId={}", uploadId);
        String uploadKey = "upload:" + uploadId;
        String chunksKey = uploadKey + ":chunks";
        stringRedisTemplate.delete(List.of(uploadKey, chunksKey));
    }
}
