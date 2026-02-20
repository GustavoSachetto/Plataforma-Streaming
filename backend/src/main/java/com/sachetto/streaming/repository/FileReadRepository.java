package com.sachetto.streaming.repository;

import java.util.List;

import org.springframework.data.elasticsearch.annotations.Query;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import org.springframework.stereotype.Repository;

import com.sachetto.streaming.entity.FileRead;

@Repository
public interface FileReadRepository extends ElasticsearchRepository<FileRead, String> {

    @Query("{\"multi_match\": {\"query\": \"?0\", \"fields\": [\"name\", \"content\"]}}")
    List<FileRead> searchByNameOrContent(String query);
}
