package com.sachetto.streaming.repository;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.sachetto.streaming.entity.File;

@Repository
public interface FileRepository extends JpaRepository<File, UUID> { }
