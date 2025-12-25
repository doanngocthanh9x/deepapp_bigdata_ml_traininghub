package com.deepapp.vn.io.storage.dto;

import com.deepapp.vn.io.storage.entity.PageEntity;

public class PageDTO {
    private Long id;
    private String requestId;
    private Integer pageNumber;
    private Integer width;
    private Integer height;
    private Integer dpi;
    private String format;
    private String imagePath;
    private String text;
    private String status;
    private String imageData; // Base64 encoded image
    
    public PageDTO() {}
    
    public PageDTO(PageEntity entity, boolean includeImageData) {
        this.id = entity.getId();
        this.requestId = entity.getRequestId();
        this.pageNumber = entity.getPageNumber();
        this.width = entity.getWidth();
        this.height = entity.getHeight();
        this.dpi = entity.getDpi();
        this.format = entity.getFormat();
        this.imagePath = entity.getImagePath();
        this.text = entity.getText();
        this.status = entity.getStatus();
        
        if (includeImageData) {
            this.imageData = entity.getImageDataBase64();
        }
    }
    
    public static PageDTO fromEntity(PageEntity entity) {
        return new PageDTO(entity, true);
    }
    
    public static PageDTO fromEntityWithoutImage(PageEntity entity) {
        return new PageDTO(entity, false);
    }
    
    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    
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
    
    public String getImageData() { return imageData; }
    public void setImageData(String imageData) { this.imageData = imageData; }
}
