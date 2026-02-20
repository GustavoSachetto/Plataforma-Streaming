package com.sachetto.streaming.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.sachetto.streaming.dto.CatalogSearchResponseDto;
import com.sachetto.streaming.service.CatalogService;

import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/v1/catalog")
public class CatalogController {

    private final CatalogService catalogService;

    @GetMapping("/search")
    public ResponseEntity<List<CatalogSearchResponseDto>> searchCatalog(@RequestParam(name = "q") String query) {
        return ResponseEntity.ok(catalogService.searchCatalog(query));
    }
}
