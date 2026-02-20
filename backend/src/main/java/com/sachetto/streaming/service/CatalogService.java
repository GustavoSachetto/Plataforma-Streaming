package com.sachetto.streaming.service;

import java.util.List;

import org.springframework.stereotype.Service;

import com.sachetto.streaming.dto.CatalogSearchResponseDto;
import com.sachetto.streaming.repository.FileReadRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class CatalogService {

    private final FileReadRepository fileReadRepository;

    public List<CatalogSearchResponseDto> searchCatalog(String query) {
        return fileReadRepository.searchByNameOrContent(query)
                .stream()
                .map(fileRead -> CatalogSearchResponseDto.builder()
                        .id(fileRead.getId())
                        .name(fileRead.getName())
                        .content(fileRead.getContent())
                        .build())
                .toList();
    }
}
