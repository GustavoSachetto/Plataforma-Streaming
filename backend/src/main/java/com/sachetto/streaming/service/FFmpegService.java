package com.sachetto.streaming.service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;

import org.springframework.stereotype.Service;

import com.sachetto.streaming.exception.ArquivoIOException;
import com.sachetto.streaming.exception.ComandoFFMpegException;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class FFmpegService {

    public Stream<Path> split(java.io.File inputFile, UUID uploadId) {
        log.info("Iniciando split do arquivo: {} para uploadId: {}", inputFile.getName(), uploadId);
        Path outputDirPath = Paths.get("uploads", uploadId.toString());

        try {
            if (Files.notExists(outputDirPath)) {
                log.debug("Criando diretório de saída: {}", outputDirPath);
                Files.createDirectories(outputDirPath);
            }

            String outputPattern = outputDirPath.toAbsolutePath().resolve("video_%03d.ts").toString();
            // Use system ffmpeg
            String ffmpegPath = "ffmpeg";

            log.debug("Caminho do FFmpeg: {}", ffmpegPath);
            
            ProcessBuilder pb = new ProcessBuilder(
                ffmpegPath,
                "-i", inputFile.getAbsolutePath(),
                "-c", "copy",           
                "-map", "0",            
                "-f", "segment",        
                "-segment_time", "10",  
                "-initial_offset", "0", 
                outputPattern
            );

            pb.redirectErrorStream(true);
            log.info("Executando comando FFmpeg...");
            Process process = pb.start();

            int exitCode = process.waitFor();
            if (exitCode != 0) {
                log.error("Erro ao executar FFmpeg. Código de saída: {}", exitCode);
                throw new ComandoFFMpegException();
            }
            
            log.info("Split concluído com sucesso.");
            return Files.list(outputDirPath)
                    .filter(path -> path.getFileName().toString().endsWith(".ts"))
                    .sorted();
            
        } catch (IOException | InterruptedException e) {
            log.error("Erro durante o processo de split", e);
            Thread.currentThread().interrupt();
            throw new ArquivoIOException();
        }
    }
    
    public void formatHLS(List<String> chunksPath, UUID uploadId) {
        log.info("Iniciando formatação do HLS para uploadId: {}", uploadId);
        Path outputDirPath = Paths.get("uploads", uploadId.toString());
        
        try {
            if (Files.notExists(outputDirPath)) {
                Files.createDirectories(outputDirPath);
            }

            Path listFilePath = outputDirPath.resolve("lista.txt");
            StringBuilder listContent = new StringBuilder();
            for (String chunk : chunksPath) {
                listContent.append("file '").append(chunk).append("'\n");
            }
            Files.write(listFilePath, listContent.toString().getBytes());

            String playlistPath = outputDirPath.toAbsolutePath().resolve("playlist.m3u8").toString();
            // Use system ffmpeg
            String ffmpegPath = "ffmpeg";

            log.debug("Caminho do FFmpeg: {}", ffmpegPath);
            
            ProcessBuilder pb = new ProcessBuilder(
                ffmpegPath,
                "-f", "concat",
                "-safe", "0",
                "-i", listFilePath.toAbsolutePath().toString(),
                "-c:v", "libx264",
                "-preset", "veryfast",
                "-g", "60",
                "-keyint_min", "60",
                "-sc_threshold", "0",
                "-c:a", "copy",
                "-hls_time", "4",
                "-hls_list_size", "0",
                "-hls_flags", "independent_segments",
                "-f", "hls",
                playlistPath
            );

            pb.redirectErrorStream(true);
            log.info("Executando comando FFmpeg para HLS...");
            Process process = pb.start();

            try (java.io.BufferedReader reader = new java.io.BufferedReader(new java.io.InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    log.debug("FFmpeg: {}", line);
                }
            }

            int exitCode = process.waitFor();
            if (exitCode != 0) {
                log.error("Erro ao executar FFmpeg. Código de saída: {}", exitCode);
                throw new ComandoFFMpegException();
            }
            
            log.info("HLS formatado com sucesso. Removendo chunks...");
            
            // Remove chunks
            for (String chunk : chunksPath) {
                try {
                    Files.deleteIfExists(Paths.get(chunk));
                } catch (IOException e) {
                    log.warn("Erro ao remover chunk: {}", chunk, e);
                }
            }
            
            // Remove list file
            Files.deleteIfExists(listFilePath);
            
        } catch (IOException | InterruptedException e) {
            log.error("Erro durante a formatação HLS", e);
            Thread.currentThread().interrupt();
            throw new ArquivoIOException();
        }
    }

    public void addWatermark(Path inputPath, Path outputPath) {
        log.info("Adicionando marca d'água em: {}", inputPath.getFileName());
        try {
            // Use system ffmpeg to avoid segfault with bundled version 4.4.1
            String ffmpegPath = "ffmpeg"; 
            
            String faviconPath;
            try {
                java.io.File resource = org.springframework.util.ResourceUtils.getFile("classpath:static/favicon.ico");
                faviconPath = resource.getAbsolutePath();
            } catch (java.io.FileNotFoundException e) {
                 faviconPath = "src/main/resources/static/favicon.ico";
            }

            // Verify if favicon exists
            java.io.File faviconFile = new java.io.File(faviconPath);
            if (!faviconFile.exists()) {
                log.error("Favicon não encontrado em: {}", faviconPath);
                throw new java.io.FileNotFoundException("Favicon não encontrado: " + faviconPath);
            }

            log.debug("Usando favicon: {}", faviconPath);

            // ffmpeg -i playlist0.ts -i ... -filter_complex "[1:v]scale=50:-1[logo]; [0:v][logo]overlay=W-w-15:H-h-15" -c:v libx264 -crf 20 -c:a copy teste.ts
            ProcessBuilder pb = new ProcessBuilder(
                ffmpegPath,
                "-y",
                "-copyts", // MANTÉM OS TIMESTAMPS ORIGINAIS DO CHUNK
                "-i", inputPath.toAbsolutePath().toString(),
                "-i", faviconPath,
                "-filter_complex", "[1:v]scale=50:-1[logo]; [0:v][logo]overlay=W-w-15:H-h-15",
                "-c:v", "libx264",
                "-crf", "20",
                "-an", // Garantindo que não tente processar áudio inexistente
                "-muxdelay", "0", // Remove atraso de multiplexação
                outputPath.toAbsolutePath().toString()
            );

            pb.redirectErrorStream(true);
            Process process = pb.start();

            StringBuilder outputLog = new StringBuilder();
            try (java.io.BufferedReader reader = new java.io.BufferedReader(new java.io.InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    log.debug("FFmpeg Watermark: {}", line);
                    outputLog.append(line).append("\n");
                }
            }

            int exitCode = process.waitFor();
            if (exitCode != 0) {
                log.error("Erro ao aplicar watermark. Cód: {}. Check logs for details.", exitCode);
                log.error("FFmpeg Output/Error: {}", outputLog.toString());
                throw new ComandoFFMpegException();
            }
            log.info("Marca d'água aplicada com sucesso: {}", outputPath.getFileName());

        } catch (IOException | InterruptedException e) {
            log.error("Erro ao aplicar watermark", e);
            Thread.currentThread().interrupt();
            throw new ArquivoIOException();
        }
    }
    public void export(Path inputPath, Path outputPath) {
        log.info("Exporting video with watermark: {}", inputPath.getFileName());
        try {
            String ffmpegPath = "ffmpeg";
            
            String faviconPath;
            try {
                java.io.File resource = org.springframework.util.ResourceUtils.getFile("classpath:static/favicon.ico");
                faviconPath = resource.getAbsolutePath();
            } catch (java.io.FileNotFoundException e) {
                 faviconPath = "src/main/resources/static/favicon.ico";
            }

            java.io.File faviconFile = new java.io.File(faviconPath);
            if (!faviconFile.exists()) {
                log.error("Favicon não encontrado em: {}", faviconPath);
                throw new java.io.FileNotFoundException("Favicon não encontrado: " + faviconPath);
            }

            ProcessBuilder pb = new ProcessBuilder(
                ffmpegPath,
                "-y",
                "-copyts",
                "-i", inputPath.toAbsolutePath().toString(),
                "-i", faviconPath,
                "-filter_complex", "[1:v]scale=50:-1[logo]; [0:v][logo]overlay=W-w-15:H-h-15",
                "-c:v", "libx264",
                "-crf", "20",
                "-an",
                "-muxdelay", "0",
                outputPath.toAbsolutePath().toString()
            );

            pb.redirectErrorStream(true);
            Process process = pb.start();

            StringBuilder outputLog = new StringBuilder();
            try (java.io.BufferedReader reader = new java.io.BufferedReader(new java.io.InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    log.debug("FFmpeg Export: {}", line);
                    outputLog.append(line).append("\n");
                }
            }

            int exitCode = process.waitFor();
            if (exitCode != 0) {
                log.error("Erro ao exportar vídeo. Cód: {}. Check logs for details.", exitCode);
                log.error("FFmpeg Output/Error: {}", outputLog.toString());
                throw new ComandoFFMpegException();
            }
            log.info("Export concluído com sucesso: {}", outputPath.getFileName());

        } catch (IOException | InterruptedException e) {
            log.error("Erro ao exportar vídeo", e);
            Thread.currentThread().interrupt();
            throw new ArquivoIOException();
        }
    }
}