package com.deepapp.vn.io.AA.A0.AAA0_0203.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * NER Training Dataset entity
 * Represents a collection of training samples for a specific template/document type
 */
@Entity
@Table(name = "ner_training_datasets")
public class NERTrainingDataset {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 100)
    private String templateId;

    @Column(length = 255)
    private String filename;

    @Column(nullable = false)
    private Integer totalSamples = 0;

    @Column(nullable = false)
    private Integer approvedCount = 0;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    @Column(length = 500)
    private String description;

    @Column(nullable = false)
    private Boolean active = true;

    // Constructors
    public NERTrainingDataset() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    public NERTrainingDataset(String templateId) {
        this();
        this.templateId = templateId;
        this.filename = templateId + "_training_data.json";
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getTemplateId() {
        return templateId;
    }

    public void setTemplateId(String templateId) {
        this.templateId = templateId;
    }

    public String getFilename() {
        return filename;
    }

    public void setFilename(String filename) {
        this.filename = filename;
    }

    public Integer getTotalSamples() {
        return totalSamples;
    }

    public void setTotalSamples(Integer totalSamples) {
        this.totalSamples = totalSamples;
    }

    public Integer getApprovedCount() {
        return approvedCount;
    }

    public void setApprovedCount(Integer approvedCount) {
        this.approvedCount = approvedCount;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Boolean getActive() {
        return active;
    }

    public void setActive(Boolean active) {
        this.active = active;
    }

    // Helper methods
    public void incrementTotalSamples() {
        this.totalSamples++;
        this.updatedAt = LocalDateTime.now();
    }

    public void incrementApprovedCount() {
        this.approvedCount++;
        this.updatedAt = LocalDateTime.now();
    }

    public void decrementApprovedCount() {
        if (this.approvedCount > 0) {
            this.approvedCount--;
            this.updatedAt = LocalDateTime.now();
        }
    }

    public void updateTimestamp() {
        this.updatedAt = LocalDateTime.now();
    }
}
