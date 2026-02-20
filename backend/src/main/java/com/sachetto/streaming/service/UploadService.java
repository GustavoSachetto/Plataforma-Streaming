package com.sachetto.streaming.service;

import java.util.List;

import org.springframework.stereotype.Service;

import com.sachetto.streaming.dto.ChunkRequestDto;
import com.sachetto.streaming.dto.ChunkResponseDto;
import com.sachetto.streaming.dto.CompleteRequestDto;
import com.sachetto.streaming.dto.CompleteResponseDto;
import com.sachetto.streaming.dto.InitRequestDto;
import com.sachetto.streaming.dto.InitResponseDto;
import com.sachetto.streaming.entity.File;
import com.sachetto.streaming.exception.ArquivoIOException;
import com.sachetto.streaming.exception.ChecksumException;
import com.sachetto.streaming.repository.FileRepository;
import com.sachetto.streaming.util.CheckSumUtil;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class UploadService {
	
	private final FileRepository fileRepository; 
	private final ChunkService chunkService;
	private final FFmpegService ffmpegService;
	private final StorageService storageService;

	@Transactional
	public InitResponseDto init(InitRequestDto initRequestDto) {
		log.info("Iniciando upload: {}", initRequestDto.filename());
		
		File file = fileRepository.save(
			File.builder()
				.name(initRequestDto.filename())
				.hash(initRequestDto.fileHash())
				.size(initRequestDto.fileSize())
				.content(initRequestDto.filecontent())
				.valid(false)
				.build());
		
		log.info("Upload inicializado com ID: {}", file.getId());
		chunkService.registerUpload(file.getId(), initRequestDto.totalChunks());
		return new InitResponseDto(file.getId());
	}

	@Transactional
	public ChunkResponseDto chunk(ChunkRequestDto chunkRequestDto) {
		log.info("Recebendo chunk {} para upload ID: {}", chunkRequestDto.index(), chunkRequestDto.uploadId());
	
		validarCheckSumPorChunk(chunkRequestDto);
		
		try {
			storageService.upload(chunkRequestDto.uploadId(), chunkRequestDto.index(), chunkRequestDto.file().getInputStream());
		} catch (java.io.IOException e) {
			log.error("Erro ao processar arquivo do chunk", e);
			throw new ArquivoIOException();
		}
		
		chunkService.registerChunk(chunkRequestDto.uploadId(), chunkRequestDto.index());
			
		log.debug("Chunk {} salvo com sucesso.", chunkRequestDto.index());
		return new ChunkResponseDto(chunkRequestDto.uploadId());
	}
	
	@Transactional
	public CompleteResponseDto complete(CompleteRequestDto completeRequestDto) {
		log.info("Finalizando upload ID: {}", completeRequestDto.uploadId());
		File file = fileRepository.findById(completeRequestDto.uploadId()).orElseThrow();
		List<String> chunksPath = chunkService.validateAndGetChunkPaths(completeRequestDto.uploadId());

		validarCheckSumPorFile(chunksPath, file);
		ffmpegService.formatHLS(chunksPath, file.getId());
		
		file.setValid(true);
		file = fileRepository.save(file);
		
		chunkService.cleanup(file.getId());
		
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
}
