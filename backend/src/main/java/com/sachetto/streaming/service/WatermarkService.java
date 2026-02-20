package com.sachetto.streaming.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sachetto.streaming.entity.File;
import com.sachetto.streaming.entity.Usuario;
import com.sachetto.streaming.entity.Watermark;
import com.sachetto.streaming.repository.WatermarkRepository;
import com.sachetto.streaming.util.FixedProtocolGenerator;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class WatermarkService {
	
	private final WatermarkRepository watermarkRepository;

	@Transactional
	public String criarOuRecuperar(Usuario usuario, File file) {
		Watermark watermark = watermarkRepository.findByUsuarioAndFile(usuario, file).orElseGet(() -> criar(usuario, file));
		return watermark.getCodigo();
	}
	
	private Watermark criar(Usuario usuario, File file) {
		Watermark watermark = Watermark.builder()
				.file(file)
				.usuario(usuario)
				.codigo(FixedProtocolGenerator.generate())
				.build();
			
		return watermarkRepository.save(watermark);
	}
}
