package com.sachetto.streaming.service;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class DownloadService {

    private final StorageService storageService;

    private final FFmpegService ffmpegService;

    public Resource getPlaylist(UUID uploadId) {
        Path path = Paths.get("uploads", uploadId.toString(), "playlist.m3u8");
        log.debug("Loading playlist from: {}", path);
        return storageService.load(path.toString());
    }
    
    public Resource getSegment(UUID uploadId, String segmentName) {
         Path originalPath = Paths.get("uploads", uploadId.toString(), segmentName);
         Path watermarkDir = Paths.get("uploads", uploadId.toString(), "watermarked");
         Path watermarkPath = watermarkDir.resolve(segmentName);

         log.debug("Requesting segment: {}", segmentName);

         try {
             if (java.nio.file.Files.exists(watermarkPath)) {
                 log.debug("Serving cached watermarked segment: {}", watermarkPath);
                 return storageService.load(watermarkPath.toString());
             }

             if (java.nio.file.Files.notExists(watermarkDir)) {
                 java.nio.file.Files.createDirectories(watermarkDir);
             }

             log.info("Generating watermark for segment: {}", segmentName);
             ffmpegService.addWatermark(originalPath, watermarkPath);

             return storageService.load(watermarkPath.toString());

         } catch (java.io.IOException e) {
             log.error("Error handling watermark for segment: {}", segmentName, e);
             log.warn("Falling back to original segment due to watermark error");
             return storageService.load(originalPath.toString());
         }
    }
    public Resource exportFile(UUID uploadId) {
        Path playlistPath = Paths.get("uploads", uploadId.toString(), "playlist.m3u8");
        Path exportPath = Paths.get("uploads", uploadId.toString(), "export.mp4");

        log.debug("Requesting export: {}", exportPath);

        if (java.nio.file.Files.exists(exportPath)) {
		    log.debug("Serving cached export: {}", exportPath);
		    return storageService.load(exportPath.toString());
		}

		log.info("Generating export for uploadId: {}", uploadId);
		ffmpegService.export(playlistPath, exportPath);

		return storageService.load(exportPath.toString());
    }
}
