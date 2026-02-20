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

    private static final String UPLOADS_DIR = "uploads";
    private static final String FFMPEG_CMD = "ffmpeg";
    private static final String FAVICON_CLASSPATH = "classpath:static/favicon.ico";
    private static final String FAVICON_FALLBACK_PATH = "src/main/resources/static/favicon.ico";
    private static final String WATERMARK_FILTER_BASE = "[1:v]scale=50:-1[logo]; [0:v][logo]overlay=W-w-15:H-h-15";
    private static final String CODEC_H264 = "libx264";

    public Stream<Path> split(java.io.File inputFile, UUID uploadId) {
        log.info("Iniciando split do arquivo: {} para uploadId: {}", inputFile.getName(), uploadId);
        Path outputDirPath = Paths.get(UPLOADS_DIR, uploadId.toString());

        try {
            if (Files.notExists(outputDirPath)) {
                log.debug("Criando diretório de saída: {}", outputDirPath);
                Files.createDirectories(outputDirPath);
            }

            String outputPattern = outputDirPath.toAbsolutePath().resolve("video_%03d.ts").toString();           
            ProcessBuilder pb = new ProcessBuilder(
                FFMPEG_CMD,
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
        Path outputDirPath = Paths.get(UPLOADS_DIR, uploadId.toString());
        
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
            
            ProcessBuilder pb = new ProcessBuilder(
                FFMPEG_CMD,
                "-f", "concat",
                "-safe", "0",
                "-i", listFilePath.toAbsolutePath().toString(),
                "-c:v", CODEC_H264,
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

            logProcessOutput(process, "FFmpeg:");

            int exitCode = process.waitFor();
            if (exitCode != 0) {
                log.error("Erro ao executar FFmpeg. Código de saída: {}", exitCode);
                throw new ComandoFFMpegException();
            }
            
            log.info("HLS formatado com sucesso. Removendo chunks...");
            
            for (String chunk : chunksPath) {
                deleteChunk(chunk);
            }
            
            Files.deleteIfExists(listFilePath);
        } catch (IOException | InterruptedException e) {
            log.error("Erro durante a formatação HLS", e);
            Thread.currentThread().interrupt();
            throw new ArquivoIOException();
        }
    }

    public void addWatermark(Path inputPath, Path outputPath, String code) {
        log.info("Adicionando marca d'água em: {}", inputPath.getFileName());
        try {
            String faviconPath = getFaviconPath();

            java.io.File faviconFile = new java.io.File(faviconPath);
            if (!faviconFile.exists()) {
                log.error("Favicon não encontrado em: {}", faviconPath);
                throw new java.io.FileNotFoundException("Favicon não encontrado: " + faviconPath);
            }

            String filterComplex = WATERMARK_FILTER_BASE;
            Path tempWatermarkImage = null;

            java.util.List<String> cmdList = new java.util.ArrayList<>();
            cmdList.add(FFMPEG_CMD);
            cmdList.add("-y");
            cmdList.add("-copyts");
            cmdList.add("-i");
            cmdList.add(inputPath.toAbsolutePath().toString());
            cmdList.add("-i");
            cmdList.add(faviconPath);

            if (code != null && !code.isEmpty()) {
                tempWatermarkImage = createRoundedWatermarkText(code);
                cmdList.add("-i");
                cmdList.add(tempWatermarkImage.toAbsolutePath().toString());
                filterComplex += "[v1];[v1][2:v]overlay=10:10";
            }

            log.debug("Usando favicon: {}", faviconPath);

            cmdList.addAll(java.util.Arrays.asList(
                "-filter_complex", filterComplex,
                "-c:v", CODEC_H264,
                "-crf", "20",
                "-an", 
                "-muxdelay", "0",
                outputPath.toAbsolutePath().toString()
            ));

            ProcessBuilder pb = new ProcessBuilder(cmdList);

            pb.redirectErrorStream(true);
            Process process = pb.start();

            StringBuilder outputLog = new StringBuilder();
            logProcessOutputAndAppend(process, "FFmpeg Watermark:", outputLog);

            int exitCode = process.waitFor();

            if (tempWatermarkImage != null) {
                try {
                    Files.deleteIfExists(tempWatermarkImage);
                } catch (Exception ignored) {}
            }

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
    
    public void export(Path inputPath, Path outputPath, String code) {
        log.info("Exporting video with watermark: {}", inputPath.getFileName());
        try {          
            String faviconPath = getFaviconPath();

            java.io.File faviconFile = new java.io.File(faviconPath);
            if (!faviconFile.exists()) {
                log.error("Favicon não encontrado em: {}", faviconPath);
                throw new java.io.FileNotFoundException("Favicon não encontrado: " + faviconPath);
            }

            String filterComplex = WATERMARK_FILTER_BASE;
            Path tempWatermarkImage = null;

            java.util.List<String> cmdList = new java.util.ArrayList<>();
            cmdList.add(FFMPEG_CMD);
            cmdList.add("-y");
            cmdList.add("-copyts");
            cmdList.add("-i");
            cmdList.add(inputPath.toAbsolutePath().toString());
            cmdList.add("-i");
            cmdList.add(faviconPath);

            if (code != null && !code.isEmpty()) {
                tempWatermarkImage = createRoundedWatermarkText(code);
                cmdList.add("-i");
                cmdList.add(tempWatermarkImage.toAbsolutePath().toString());
                filterComplex += "[v1];[v1][2:v]overlay=10:10";
            }

            cmdList.addAll(java.util.Arrays.asList(
                "-filter_complex", filterComplex,
                "-c:v", CODEC_H264,
                "-crf", "20",
                "-an",
                "-muxdelay", "0",
                outputPath.toAbsolutePath().toString()
            ));

            ProcessBuilder pb = new ProcessBuilder(cmdList);

            pb.redirectErrorStream(true);
            Process process = pb.start();

            StringBuilder outputLog = new StringBuilder();
            logProcessOutputAndAppend(process, "FFmpeg Export:", outputLog);

            int exitCode = process.waitFor();

            if (tempWatermarkImage != null) {
                try {
                    Files.deleteIfExists(tempWatermarkImage);
                } catch (Exception ignored) {}
            }

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

    private void logProcessOutput(Process process, String logPrefix) throws IOException {
        try (java.io.BufferedReader reader = new java.io.BufferedReader(new java.io.InputStreamReader(process.getInputStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                log.debug("{} {}", logPrefix, line);
            }
        }
    }

    private void logProcessOutputAndAppend(Process process, String logPrefix, StringBuilder outputLog) throws IOException {
        try (java.io.BufferedReader reader = new java.io.BufferedReader(new java.io.InputStreamReader(process.getInputStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                log.debug("{} {}", logPrefix, line);
                outputLog.append(line).append("\n");
            }
        }
    }

    private void deleteChunk(String chunk) {
        try {
            Files.deleteIfExists(Paths.get(chunk));
        } catch (IOException e) {
            log.warn("Erro ao remover chunk: {}", chunk, e);
        }
    }

    private String getFaviconPath() {
        try {
            java.io.File resource = org.springframework.util.ResourceUtils.getFile(FAVICON_CLASSPATH);
            return resource.getAbsolutePath();
        } catch (java.io.FileNotFoundException ignored) {
            return FAVICON_FALLBACK_PATH;
        }
    }

    private Path createRoundedWatermarkText(String text) throws IOException {
        int padding = 10;
        int fontSize = 24;
        int cornerRadius = 12;

        java.awt.Font font = new java.awt.Font("SansSerif", java.awt.Font.BOLD, fontSize);
        
        java.awt.image.BufferedImage img = new java.awt.image.BufferedImage(1, 1, java.awt.image.BufferedImage.TYPE_INT_ARGB);
        java.awt.Graphics2D g2d = img.createGraphics();
        g2d.setFont(font);
        java.awt.FontMetrics fm = g2d.getFontMetrics();
        int textWidth = fm.stringWidth(text);
        int textHeight = fm.getHeight();
        g2d.dispose();

        int width = textWidth + padding * 2;
        int height = textHeight + padding * 2;

        img = new java.awt.image.BufferedImage(width, height, java.awt.image.BufferedImage.TYPE_INT_ARGB);
        g2d = img.createGraphics();
        
        g2d.setRenderingHint(java.awt.RenderingHints.KEY_ANTIALIASING, java.awt.RenderingHints.VALUE_ANTIALIAS_ON);
        g2d.setRenderingHint(java.awt.RenderingHints.KEY_TEXT_ANTIALIASING, java.awt.RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        g2d.setColor(new java.awt.Color(0, 0, 0, 128));
        g2d.fillRoundRect(0, 0, width, height, cornerRadius, cornerRadius);

        g2d.setColor(java.awt.Color.WHITE);
        g2d.setFont(font);
        g2d.drawString(text, padding, fm.getAscent() + padding);
        g2d.dispose();

        Path tempFile = Files.createTempFile("watermark_text_", ".png");
        javax.imageio.ImageIO.write(img, "png", tempFile.toFile());
        
        return tempFile;
    }
}