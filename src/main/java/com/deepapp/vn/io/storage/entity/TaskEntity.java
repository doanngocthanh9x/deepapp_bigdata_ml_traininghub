package com.deepapp.vn.io.storage.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "tasks")
public class TaskEntity {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "request_id", referencedColumnName = "request_id", unique = true)
    private DocumentEntity document;
    
    @Column(name = "task_type", nullable = false)
    private String taskType;
    
    @Column(nullable = false)
    private String status = "pending";
    
    @Column(name = "total_pages")
    private Integer totalPages = 0;
    
    @Column(name = "processed_pages")
    private Integer processedPages = 0;
    
    @Column(name = "started_at", nullable = false, updatable = false)
    private LocalDateTime startedAt = LocalDateTime.now();
    
    @Column(name = "completed_at")
    private LocalDateTime completedAt;
    
    @Column(name = "error_message", length = 1000)
    private String errorMessage;
    
    // Constructors
    public TaskEntity() {}
    
    public TaskEntity(String taskType) {
        this.taskType = taskType;
    }
    
    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    
    public DocumentEntity getDocument() { return document; }
    public void setDocument(DocumentEntity document) { this.document = document; }
    
    public String getTaskType() { return taskType; }
    public void setTaskType(String taskType) { this.taskType = taskType; }
    
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    
    public Integer getTotalPages() { return totalPages; }
    public void setTotalPages(Integer totalPages) { this.totalPages = totalPages; }
    
    public Integer getProcessedPages() { return processedPages; }
    public void setProcessedPages(Integer processedPages) { this.processedPages = processedPages; }
    
    public LocalDateTime getStartedAt() { return startedAt; }
    public void setStartedAt(LocalDateTime startedAt) { this.startedAt = startedAt; }
    
    public LocalDateTime getCompletedAt() { return completedAt; }
    public void setCompletedAt(LocalDateTime completedAt) { this.completedAt = completedAt; }
    
    public String getErrorMessage() { return errorMessage; }
    public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }
    
    // Helper methods
    public int getProgressPercentage() {
        if (totalPages == null || totalPages == 0) return 0;
        return (int) ((processedPages * 100.0) / totalPages);
    }
}
