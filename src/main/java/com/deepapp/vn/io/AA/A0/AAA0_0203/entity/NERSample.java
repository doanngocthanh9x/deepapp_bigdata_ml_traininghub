package com.deepapp.vn.io.AA.A0.AAA0_0203.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * NER Training Sample entity
 * Represents a single text sample with its annotations
 */
@Entity
@Table(name = "ner_samples", indexes = {
    @Index(name = "idx_sample_dataset", columnList = "dataset_id"),
    @Index(name = "idx_sample_approved", columnList = "approved"),
    @Index(name = "idx_sample_dataset_approved", columnList = "dataset_id,approved")
})
public class NERSample {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 100)
    private String sampleId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "dataset_id", nullable = false)
    private NERTrainingDataset dataset;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String text;

    @Column(nullable = false)
    private Boolean approved = false;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    @Column(length = 255)
    private String sourceDocumentId;

    @Column(length = 100)
    private String documentType;

    @OneToMany(mappedBy = "sample", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<NERAnnotation> annotations = new ArrayList<>();

    @Column(nullable = false)
    private Integer annotationCount = 0;

    // Constructors
    public NERSample() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    public NERSample(String sampleId, NERTrainingDataset dataset, String text) {
        this();
        this.sampleId = sampleId;
        this.dataset = dataset;
        this.text = text;
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getSampleId() {
        return sampleId;
    }

    public void setSampleId(String sampleId) {
        this.sampleId = sampleId;
    }

    public NERTrainingDataset getDataset() {
        return dataset;
    }

    public void setDataset(NERTrainingDataset dataset) {
        this.dataset = dataset;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
        this.updatedAt = LocalDateTime.now();
    }

    public Boolean getApproved() {
        return approved;
    }

    public void setApproved(Boolean approved) {
        this.approved = approved;
        this.updatedAt = LocalDateTime.now();
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

    public String getSourceDocumentId() {
        return sourceDocumentId;
    }

    public void setSourceDocumentId(String sourceDocumentId) {
        this.sourceDocumentId = sourceDocumentId;
    }

    public String getDocumentType() {
        return documentType;
    }

    public void setDocumentType(String documentType) {
        this.documentType = documentType;
    }

    public List<NERAnnotation> getAnnotations() {
        return annotations;
    }

    public void setAnnotations(List<NERAnnotation> annotations) {
        this.annotations = annotations;
        this.annotationCount = annotations.size();
        this.updatedAt = LocalDateTime.now();
    }

    public Integer getAnnotationCount() {
        return annotationCount;
    }

    public void setAnnotationCount(Integer annotationCount) {
        this.annotationCount = annotationCount;
    }

    // Helper methods
    public void addAnnotation(NERAnnotation annotation) {
        annotations.add(annotation);
        annotation.setSample(this);
        this.annotationCount = annotations.size();
        this.updatedAt = LocalDateTime.now();
    }

    public void removeAnnotation(NERAnnotation annotation) {
        annotations.remove(annotation);
        annotation.setSample(null);
        this.annotationCount = annotations.size();
        this.updatedAt = LocalDateTime.now();
    }

    public void clearAnnotations() {
        annotations.clear();
        this.annotationCount = 0;
        this.updatedAt = LocalDateTime.now();
    }

    public void updateTimestamp() {
        this.updatedAt = LocalDateTime.now();
    }
}
