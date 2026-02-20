package com.sachetto.streaming.seed;

import java.time.LocalDateTime;

import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import com.sachetto.streaming.entity.Usuario;
import com.sachetto.streaming.repository.UsuarioRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class UsuarioSeeder implements CommandLineRunner {

    private final UsuarioRepository usuarioRepository;

    @Override
    public void run(String... args) throws Exception {
        if (usuarioRepository.count() == 0) {
            log.info("Seeding default user...");
            Usuario usuario = Usuario.builder()
                .nome("Admin")
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
            
            usuarioRepository.save(usuario);
            log.info("Default user seeded: {}", usuario);
        } else {
            log.info("Users already exist. Skipping seed.");
        }
    }
}
