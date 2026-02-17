package com.sachetto.streaming.service;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.UUID;
import java.net.MalformedURLException;

import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;

import org.springframework.stereotype.Service;

import com.sachetto.streaming.exception.ArquivoIOException;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class StorageServiceFileSystem implements StorageService {

    private static final Path RAIZ_UPLOADS = Paths.get("uploads");

    @Override
    public String upload(UUID uploadId, Long index, InputStream file) {
        try {
            Path pastaUpload = RAIZ_UPLOADS.resolve(uploadId.toString());
            
            if (!Files.exists(pastaUpload)) {
                log.debug("Criando diretório de upload: {}", pastaUpload);
                Files.createDirectories(pastaUpload);
            }

            String nomeArquivo = index + ".mp4";
            Path destino = pastaUpload.resolve(nomeArquivo);
            
            log.info("Salvando arquivo: {} para uploadId: {}", nomeArquivo, uploadId);
            Files.copy(file, destino, StandardCopyOption.REPLACE_EXISTING);
            log.debug("Arquivo salvo em: {}", destino.toAbsolutePath());
            
            return destino.toAbsolutePath().toString();
        } catch (IOException e) {
            log.error("Erro ao salvar arquivo para uploadId: {}", uploadId, e);
            throw new ArquivoIOException();
        }
    }

    @Override
    public Resource load(String path) {
        try {
            Path filePath = Paths.get(path);
            Resource resource = new UrlResource(filePath.toUri());
            
            if (resource.exists() || resource.isReadable()) {
                return resource;
            } else {
                throw new ArquivoIOException();
            }
        } catch (MalformedURLException e) {
            log.error("Erro ao carregar arquivo: {}", path, e);
            throw new ArquivoIOException();
        }
    }
}