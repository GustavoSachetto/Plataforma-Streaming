package com.sachetto.streaming.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.sachetto.streaming.entity.File;
import com.sachetto.streaming.entity.Usuario;
import com.sachetto.streaming.entity.Watermark;

@Repository
public interface WatermarkRepository extends JpaRepository<Watermark, Long> { 
	Optional<Watermark> findByUsuarioAndFile(Usuario usuario, File file);
}
