package com.sachetto.streaming.entity;

import java.time.LocalDateTime;
import java.util.UUID;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import com.sachetto.streaming.listener.FileElasticSyncListener;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Entity
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "tb_file", uniqueConstraints = {
	@UniqueConstraint(name = "uq_name", columnNames = {"name"})
})
@EntityListeners({AuditingEntityListener.class, FileElasticSyncListener.class})
public class File {

	@Id
	@Column(name = "pk_file")
	@GeneratedValue(strategy = GenerationType.UUID)
	private UUID id;
	
	private Long size;
	private String hash;
	private String name;

	@Column(columnDefinition = "TEXT")
	private String content;
	private Boolean valid;
	private String thumbnail;
	
	@CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}
