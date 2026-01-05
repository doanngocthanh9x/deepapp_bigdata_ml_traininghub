package com.deepapp.vn.io.AA.A0.AAA0_0203.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * NER Annotation entity
 * Represents a single entity annotation (span of text with entity type)
 */
@Entity
@Table(name = "ner_annotations", indexes = {
    @Index(name = "idx_annotation_sample", columnList = "sample_id"),
    @Index(name = "idx_annotation_entity_type", columnList = "entity_type")
})
public class NERAnnotation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sample_id", nullable = false)
    private NERSample sample;

    @Column(nullable = false)
    private Integer startPos;

    @Column(nullable = false)
    private Integer endPos;

    @Column(nullable = false, length = 255)
    private String text;

    @Column(nullable = false, length = 50)
    private String entityType;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @Column(length = 100)
    private String confidence;

    // Constructors
    public NERAnnotation() {
        this.createdAt = LocalDateTime.now();
    }

    public NERAnnotation(NERSample sample, Integer startPos, Integer endPos, String text, String entityType) {
        this();
        this.sample = sample;
        this.startPos = startPos;
        this.endPos = endPos;
        this.text = text;
        this.entityType = entityType;
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public NERSample getSample() {
        return sample;
    }

    public void setSample(NERSample sample) {
        this.sample = sample;
    }

    public Integer getStartPos() {
        return startPos;
    }

    public void setStartPos(Integer startPos) {
        this.startPos = startPos;
    }

    public Integer getEndPos() {
        return endPos;
    }

    public void setEndPos(Integer endPos) {
        this.endPos = endPos;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public String getEntityType() {
        return entityType;
    }

    public void setEntityType(String entityType) {
        this.entityType = entityType;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public String getConfidence() {
        return confidence;
    }

    public void setConfidence(String confidence) {
        this.confidence = confidence;
    }

    // Helper methods
    public boolean overlaps(NERAnnotation other) {
        return !(this.endPos <= other.startPos || this.startPos >= other.endPos);
    }

    public boolean contains(Integer position) {
        return position >= this.startPos && position < this.endPos;
    }
}
