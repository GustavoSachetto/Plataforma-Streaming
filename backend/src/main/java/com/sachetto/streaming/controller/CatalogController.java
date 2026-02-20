package com.sachetto.streaming.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.sachetto.streaming.dto.CatalogSearchResponseDto;
import com.sachetto.streaming.service.CatalogService;

import lombok.RequiredArgsConstructor;

import org.springframework.data.domain.Page;

@RestController
@RequiredArgsConstructor
@RequestMapping("/v1/catalog")
public class CatalogController {

    private final CatalogService catalogService;

    @GetMapping("/search")
    public ResponseEntity<Page<CatalogSearchResponseDto>> searchCatalog(
            @RequestParam(name = "q") String query,
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "10") int size) {
        return ResponseEntity.ok(catalogService.searchCatalog(query, page, size));
    }

    @GetMapping("/latest")
    public ResponseEntity<Page<CatalogSearchResponseDto>> getLatestCatalog() {
        return ResponseEntity.ok(catalogService.getLatestCatalog());
    }
}
