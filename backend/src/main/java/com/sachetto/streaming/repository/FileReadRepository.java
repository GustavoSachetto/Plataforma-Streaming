package com.sachetto.streaming.repository;

import org.springframework.data.elasticsearch.annotations.Query;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import org.springframework.stereotype.Repository;

import com.sachetto.streaming.entity.FileRead;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

@Repository
public interface FileReadRepository extends ElasticsearchRepository<FileRead, String> {

    @Query("{\"multi_match\": {\"query\": \"?0\", \"fields\": [\"name\", \"content\"]}}")
    Page<FileRead> searchByNameOrContent(String query, Pageable pageable);

    Page<FileRead> findAll(Pageable pageable);
}
