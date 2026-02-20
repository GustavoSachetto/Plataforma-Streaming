package com.sachetto.streaming.service;

import org.springframework.stereotype.Service;

import com.sachetto.streaming.dto.CatalogSearchResponseDto;
import com.sachetto.streaming.repository.FileReadRepository;

import lombok.RequiredArgsConstructor;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

@Service
@RequiredArgsConstructor
public class CatalogService {

    private final FileReadRepository fileReadRepository;

    public Page<CatalogSearchResponseDto> searchCatalog(String query, int page, int size) {
        return fileReadRepository.searchByNameOrContent(query, PageRequest.of(page, size))
                .map(fileRead -> new CatalogSearchResponseDto(fileRead.getId(), fileRead.getName(), fileRead.getContent(), fileRead.getThumbnail()));
    }
    
    public Page<CatalogSearchResponseDto> getLatestCatalog() {
        return fileReadRepository.findAll(PageRequest.of(0, 10, Sort.by(Sort.Direction.DESC, "createdAt")))
                .map(fileRead -> new CatalogSearchResponseDto(fileRead.getId(), fileRead.getName(), fileRead.getContent(), fileRead.getThumbnail()));
    }
}
