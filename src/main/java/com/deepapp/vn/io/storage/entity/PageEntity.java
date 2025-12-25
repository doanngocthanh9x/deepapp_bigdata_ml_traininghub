package com.deepapp.vn.io.storage.entity;

import jakarta.persistence.*;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.Base64;

@Entity
@Table(name = "pages")
public class PageEntity {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "document_id", nullable = false)
    private DocumentEntity document;
    
    @Column(name = "request_id", nullable = false)
    private String requestId;
    
    @Column(name = "page_number", nullable = false)
    private Integer pageNumber;
    
    @Column
    private Integer width = 0;
    
    @Column
    private Integer height = 0;
    
    @Column
    private Integer dpi = 150;
    
    @Column
    private String format;
    
    @Column(name = "image_path", length = 500)
    private String imagePath;
    
    @Column(name = "text", columnDefinition = "TEXT")
    private String text;
    
    @Column(nullable = false)
    private String status = "rendered";
    
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();
    
    // Constructors
    public PageEntity() {}
    
    public PageEntity(String requestId, Integer pageNumber) {
        this.requestId = requestId;
        this.pageNumber = pageNumber;
    }
    
    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    
    public DocumentEntity getDocument() { return document; }
    public void setDocument(DocumentEntity document) { this.document = document; }
    
    public String getRequestId() { return requestId; }
    public void setRequestId(String requestId) { this.requestId = requestId; }
    
    public Integer getPageNumber() { return pageNumber; }
    public void setPageNumber(Integer pageNumber) { this.pageNumber = pageNumber; }
    
    public Integer getWidth() { return width; }
    public void setWidth(Integer width) { this.width = width; }
    
    public Integer getHeight() { return height; }
    public void setHeight(Integer height) { this.height = height; }
    
    public Integer getDpi() { return dpi; }
    public void setDpi(Integer dpi) { this.dpi = dpi; }
    
    public String getFormat() { return format; }
    public void setFormat(String format) { this.format = format; }
    
    public String getImagePath() { return imagePath; }
    public void setImagePath(String imagePath) { this.imagePath = imagePath; }
    
    public String getText() { return text; }
    public void setText(String text) { this.text = text; }
    
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    
    /**
     * Load image data from file system and return as base64
     */
    public String getImageDataBase64() {
        if (imagePath == null || imagePath.isEmpty()) {
            return null;
        }
        
        try {
            Path path = Paths.get(imagePath);
            if (!Files.exists(path)) {
                return null;
            }
            byte[] imageBytes = Files.readAllBytes(path);
            return Base64.getEncoder().encodeToString(imageBytes);
        } catch (IOException e) {
            return null;
        }
    }
}
