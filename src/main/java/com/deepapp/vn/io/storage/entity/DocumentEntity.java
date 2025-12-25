package com.deepapp.vn.io.storage.entity;

import com.fasterxml.jackson.annotation.JsonManagedReference;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "documents")
public class DocumentEntity {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "request_id", unique = true, nullable = false)
    private String requestId;
    
    @Column(nullable = false)
    private String filename;
    
    @Column(name = "file_path")
    private String filePath;
    
    @Column(nullable = false)
    private String format;
    
    @Column(name = "page_count")
    private Integer pageCount = 0;
    
    @Column(name = "file_size")
    private Long fileSize = 0L;
    
    @Column(nullable = false)
    private String status = "processing";
    
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();
    
    @Column(name = "updated_at")
    private LocalDateTime updatedAt = LocalDateTime.now();
    
    @Column(name = "error_message", length = 1000)
    private String errorMessage;
    
    @OneToMany(mappedBy = "document", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonManagedReference("document-pages")
    private List<PageEntity> pages = new ArrayList<>();

    @OneToOne(mappedBy = "document", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonManagedReference("document-task")
    private TaskEntity task;
    
    // Constructors
    public DocumentEntity() {}
    
    public DocumentEntity(String requestId, String filename, String format) {
        this.requestId = requestId;
        this.filename = filename;
        this.format = format;
    }
    
    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    
    public String getRequestId() { return requestId; }
    public void setRequestId(String requestId) { this.requestId = requestId; }
    
    public String getFilename() { return filename; }
    public void setFilename(String filename) { this.filename = filename; }
    
    public String getFilePath() { return filePath; }
    public void setFilePath(String filePath) { this.filePath = filePath; }
    
    public String getFormat() { return format; }
    public void setFormat(String format) { this.format = format; }
    
    public Integer getPageCount() { return pageCount; }
    public void setPageCount(Integer pageCount) { this.pageCount = pageCount; }
    
    public Long getFileSize() { return fileSize; }
    public void setFileSize(Long fileSize) { this.fileSize = fileSize; }
    
    public String getStatus() { return status; }
    public void setStatus(String status) { 
        this.status = status;
        this.updatedAt = LocalDateTime.now();
    }
    
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
    
    public String getErrorMessage() { return errorMessage; }
    public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }
    
    public List<PageEntity> getPages() { return pages; }
    public void setPages(List<PageEntity> pages) { this.pages = pages; }
    
    public TaskEntity getTask() { return task; }
    public void setTask(TaskEntity task) { this.task = task; }
    
    // Helper methods
    public void addPage(PageEntity page) {
        pages.add(page);
        page.setDocument(this);
    }
    
    public void removePage(PageEntity page) {
        pages.remove(page);
        page.setDocument(null);
    }
    
    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
