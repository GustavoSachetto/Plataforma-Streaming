package com.sachetto.streaming.listener;

import org.springframework.context.annotation.Lazy;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.stereotype.Component;

import com.sachetto.streaming.entity.File;
import com.sachetto.streaming.entity.FileRead;

import jakarta.persistence.PostPersist;
import jakarta.persistence.PostRemove;
import jakarta.persistence.PostUpdate;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class FileElasticSyncListener {

	@Lazy 
    private final ElasticsearchOperations elasticsearchOperations;

    @PostPersist
    @PostUpdate
    public void onPostSave(File file) {
        FileRead fileRead = FileRead.builder()
                .id(file.getId().toString())
                .name(file.getName())
                .content(file.getContent())
                .thumbnail("http://localhost:8080/api/v1/download/" + file.getId() + "/thumbnail")
                .createdAt(file.getCreatedAt())
                .build();
        
        elasticsearchOperations.save(fileRead);
    }

    @PostRemove
    public void onPostDelete(File file) {
        elasticsearchOperations.delete(file.getId().toString(), FileRead.class);
    }
}
