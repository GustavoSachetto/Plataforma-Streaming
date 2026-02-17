package com.sachetto.streaming.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.sachetto.streaming.dto.ChunkRequestDto;
import com.sachetto.streaming.dto.ChunkResponseDto;
import com.sachetto.streaming.dto.CompleteRequestDto;
import com.sachetto.streaming.dto.CompleteResponseDto;
import com.sachetto.streaming.dto.FullRequestDto;
import com.sachetto.streaming.dto.FullResponseDto;
import com.sachetto.streaming.dto.InitRequestDto;
import com.sachetto.streaming.dto.InitResponseDto;
import com.sachetto.streaming.service.UploadService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/v1/upload")
public class UploadController {
	
	private final UploadService uploadService;

	@PostMapping("/init")
	public ResponseEntity<InitResponseDto> init(@RequestBody @Valid InitRequestDto initRequestDto) {
		return ResponseEntity.status(HttpStatus.CREATED).body(uploadService.init(initRequestDto));
	}
	
	@PostMapping(value = "/chunk", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
	public ResponseEntity<ChunkResponseDto> chunk(@Valid @ModelAttribute ChunkRequestDto chunkRequestDto) {
		return ResponseEntity.status(HttpStatus.CREATED).body(uploadService.chunk(chunkRequestDto)); 
	}
	
	@PostMapping(value = "/full", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
	public ResponseEntity<FullResponseDto> full(@Valid @ModelAttribute FullRequestDto fullRequestDto) {
		return ResponseEntity.status(HttpStatus.CREATED).body(uploadService.full(fullRequestDto)); 
	}
	
	@PostMapping("/complete")
	public ResponseEntity<CompleteResponseDto> complete(@RequestBody @Valid CompleteRequestDto completeRequestDto) {
		return ResponseEntity.status(HttpStatus.CREATED).body(uploadService.complete(completeRequestDto)); 
	}
}
