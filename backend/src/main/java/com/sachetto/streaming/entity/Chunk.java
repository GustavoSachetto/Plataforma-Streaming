package com.sachetto.streaming.entity;

import java.time.LocalDateTime;
import java.util.UUID;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
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
@Table(name = "tb_chunk", uniqueConstraints = {
	@UniqueConstraint(name = "uq_file_index", columnNames = {"fk_file", "index"})
})
@EntityListeners(AuditingEntityListener.class)
public class Chunk {
	
	@Id
	@Column(name = "pk_chunk")
	@GeneratedValue(strategy = GenerationType.UUID)
	private UUID id;
	
	private Long index;
	private String hash;
	private String path;

	@ManyToOne
	@JoinColumn(name = "fk_file")
	private File file;
	
	@CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}