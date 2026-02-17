package com.sachetto.streaming.service;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;

import org.springframework.stereotype.Service;

import com.sachetto.streaming.dto.ChunkRequestDto;
import com.sachetto.streaming.dto.ChunkResponseDto;
import com.sachetto.streaming.dto.CompleteRequestDto;
import com.sachetto.streaming.dto.CompleteResponseDto;
import com.sachetto.streaming.dto.FullRequestDto;
import com.sachetto.streaming.dto.FullResponseDto;
import com.sachetto.streaming.dto.InitRequestDto;
import com.sachetto.streaming.dto.InitResponseDto;
import com.sachetto.streaming.entity.Chunk;
import com.sachetto.streaming.entity.File;
import com.sachetto.streaming.exception.ArquivoIOException;
import com.sachetto.streaming.exception.ChecksumException;
import com.sachetto.streaming.repository.ChunkRepository;
import com.sachetto.streaming.repository.FileRepository;
import com.sachetto.streaming.util.CheckSumUtil;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class UploadService {
	
	private final FileRepository fileRepository; 
	private final ChunkRepository chunkRepository;
	private final StorageService storageService;
	private final FFmpegService ffmpegService;

	@Transactional
	public InitResponseDto init(InitRequestDto initRequestDto) {
		log.info("Iniciando upload: {}", initRequestDto.filename());
		File file = fileRepository.save(
			File.builder()
				.name(initRequestDto.filename())
				.hash(initRequestDto.fileHash())
				.size(initRequestDto.fileSize())
				.valid(false)
				.build());
		
		log.info("Upload inicializado com ID: {}", file.getId());
		return new InitResponseDto(file.getId());
	}
	
	@Transactional
	public FullResponseDto full(FullRequestDto fullRequestDto) {
		log.info("Recebendo upload completo para ID: {}", fullRequestDto.uploadId());
        File file = fileRepository.findById(fullRequestDto.uploadId())
                .orElseThrow(() -> new RuntimeException("Arquivo não encontrado"));
        
        java.io.File tempFile = null;

        try {
            tempFile = java.io.File.createTempFile("original_", ".mp4");
            log.debug("Arquivo temporário criado: {}", tempFile.getAbsolutePath());
            
            String calculatedHash;
            try (InputStream is = fullRequestDto.file().getInputStream();
                 java.io.OutputStream os = new java.io.FileOutputStream(tempFile)) {
                
                java.security.MessageDigest digest = java.security.MessageDigest.getInstance("SHA-256");
                byte[] buffer = new byte[8192];
                int bytesRead;
                while ((bytesRead = is.read(buffer)) != -1) {
                    digest.update(buffer, 0, bytesRead);
                    os.write(buffer, 0, bytesRead);
                }
                calculatedHash = java.util.HexFormat.of().formatHex(digest.digest());
            }

            if (!file.getHash().equals(calculatedHash)) {
            	log.error("Hash inválido. Esperado: {}, Calculado: {}", file.getHash(), calculatedHash);
                throw new ChecksumException();
            }

            log.info("Hash verificado com sucesso para arquivo ID: {}", file.getId());
            file.setName(fullRequestDto.filename());
            file.setHash(fullRequestDto.fileHash());
            file.setSize(fullRequestDto.file().getSize());
            fileRepository.save(file);

            log.info("Iniciando processamento com FFmpeg para arquivo ID: {}", file.getId());
            try (Stream<Path> chunkStream = ffmpegService.split(tempFile, file.getId())) {
                AtomicInteger indexCounter = new AtomicInteger(0);

                chunkStream.forEach(path -> {
                    int index = indexCounter.getAndIncrement();
                    
                    String chunkHash = CheckSumUtil.calculateSingleFileHash(path);

                    try (InputStream is = Files.newInputStream(path)) {
                        String storagePath = storageService.upload(
                            file.getId(), 
                            (long) index, 
                            is
                        );
                        chunkRepository.save(
                            Chunk.builder()
                                .index((long) index)
                                .hash(chunkHash)
                                .path(storagePath)
                                .file(file)
                                .build()
                        );
                        log.debug("Chunk {} processado e salvo.", index);
                    } catch (IOException e) {
                    	log.error("Erro ao processar chunk {}", index, e);
                        throw new ArquivoIOException();
                    } finally {
                        deleteQuietly(path);
                    }
                });
            }
            
            file.setValid(true);
            fileRepository.save(file);
            log.info("Upload completo e processado com sucesso para arquivo ID: {}", file.getId());
   
            List<String> chunksPath = chunkRepository.findPathsByFileIdOrderByIndexAsc(file.getId());

            return new FullResponseDto(file.getId(), chunksPath);
        } catch (Exception e) {
        	log.error("Erro no processamento do upload completo", e);
            throw new ArquivoIOException();
        } finally {
            if (tempFile != null && tempFile.exists()) {
            	try {
                	Files.deleteIfExists(Paths.get(tempFile.toURI()));
            	} catch (IOException e) { 
            		log.warn("Falha ao deletar arquivo temporário: {}", tempFile.getAbsolutePath(), e);
            	}
            }
        }
    }

	@Transactional
	public ChunkResponseDto chunk(ChunkRequestDto chunkRequestDto) {
		log.info("Recebendo chunk {} para upload ID: {}", chunkRequestDto.index(), chunkRequestDto.uploadId());
		try {
			File file = fileRepository.findById(chunkRequestDto.uploadId()).orElseThrow();
			
			validarCheckSumPorChunk(chunkRequestDto);
			
			String path = storageService.upload(chunkRequestDto.uploadId(), chunkRequestDto.index(), chunkRequestDto.file().getInputStream());
			
			Chunk chunk = chunkRepository.save(
				Chunk.builder()
					.hash(chunkRequestDto.chunkHash())
					.index(chunkRequestDto.index())
					.path(path)
					.file(file)
					.build());
			
			log.debug("Chunk {} salvo com sucesso.", chunkRequestDto.index());
			return new ChunkResponseDto(chunk.getId());
		} catch (IOException e) {
			log.error("Erro ao salvar chunk", e);
			throw new ArquivoIOException();
		}
	}
	
	@Transactional
	public CompleteResponseDto complete(CompleteRequestDto completeRequestDto) {
		log.info("Finalizando upload ID: {}", completeRequestDto.uploadId());
		File file = fileRepository.findById(completeRequestDto.uploadId()).orElseThrow();
		
		List<String> chunksPath = chunkRepository.findPathsByFileIdOrderByIndexAsc(completeRequestDto.uploadId());

		validarCheckSumPorFile(chunksPath, file);
		ffmpegService.formatHLS(chunksPath, file.getId());
		
		file.setValid(true);
		file = fileRepository.save(file);
		
		chunkRepository.deleteByFileId(file.getId());
		
		log.info("Upload ID: {} finalizado e validado.", file.getId());
		return new CompleteResponseDto(file.getId(), chunksPath);
	}
	
	private void validarCheckSumPorChunk(ChunkRequestDto chunkRequestDto) {
		if (!CheckSumUtil.isValid(chunkRequestDto.file(), chunkRequestDto.chunkHash())) {
			log.error("Checksum inválido para chunk {}", chunkRequestDto.index());
			throw new ChecksumException();
		}
	}
	
	private void validarCheckSumPorFile(List<String> chunksPath , File file) {
		if (!CheckSumUtil.isValidFullFile(chunksPath, file.getHash())) {
			log.error("Checksum inválido para o arquivo completo ID: {}", file.getId());
			throw new ChecksumException();
		}
	}
	
    private void deleteQuietly(Path path) {
        try {
            Files.deleteIfExists(path);
        } catch (IOException e) {
        	log.warn("Erro ao deletar arquivo: {}", path, e);
            throw new ArquivoIOException();
        }
    }
}
