package com.sachetto.streaming.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.sachetto.streaming.entity.Usuario;

@Repository
public interface UsuarioRepository extends JpaRepository<Usuario, Long> { }
