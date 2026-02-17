package com.sachetto.streaming.controller;

import java.util.UUID;

import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.sachetto.streaming.service.DownloadService;

import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/v1/download")
public class DownloadController {

    private final DownloadService downloadService;

    @GetMapping("/{uploadId}/playlist.m3u8")
    public ResponseEntity<Resource> getPlaylist(@PathVariable UUID uploadId) {
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType("application/vnd.apple.mpegurl"))
                .body(downloadService.getPlaylist(uploadId));
    }

    @GetMapping("/{uploadId}/{segmentName:.+\\.ts}")
    public ResponseEntity<Resource> getSegment(@PathVariable UUID uploadId, @PathVariable String segmentName) {
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType("video/mp2t"))
                .body(downloadService.getSegment(uploadId, segmentName));
    }
    
    @GetMapping("/{uploadId}/export")
    public ResponseEntity<Resource> export(@PathVariable UUID uploadId) {
        Resource resource = downloadService.exportFile(uploadId);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType("video/mp4"))
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + resource.getFilename() + "\"")
                .body(resource);
    }
}
