package com.sachetto.streaming.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.sachetto.streaming.entity.Chunk;

import jakarta.transaction.Transactional;

@Repository
public interface ChunkRepository extends JpaRepository<Chunk, UUID> {

    @Query("SELECT c.path FROM Chunk c WHERE c.file.id = :fileId ORDER BY c.index ASC")
    List<String> findPathsByFileIdOrderByIndexAsc(@Param("fileId") UUID fileId);

    List<Chunk> findAllByFileIdOrderByIndexAsc(UUID fileId);
	
    @Modifying
    @Transactional
    @Query("DELETE FROM Chunk c WHERE c.file.id = :fileId")
    void deleteByFileId(@Param("fileId") UUID fileId);
}