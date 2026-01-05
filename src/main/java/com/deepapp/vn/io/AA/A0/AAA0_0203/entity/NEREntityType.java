package com.deepapp.vn.io.AA.A0.AAA0_0203.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * Entity type definition for NER training
 * Allows dynamic creation of entity labels per dataset/template
 */
@Entity
@Table(name = "ner_entity_types", 
    indexes = {
        @Index(name = "idx_entity_type_template", columnList = "template_id"),
        @Index(name = "idx_entity_type_code", columnList = "entity_code"),
        @Index(name = "idx_entity_type_active", columnList = "active")
    }
)
public class NEREntityType {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    /**
     * Template/Dataset this entity type belongs to
     * null = global entity type (available for all datasets)
     */
    @Column(name = "template_id")
    private String templateId;
    
    /**
     * Unique code for this entity type (e.g., "PERSON", "DATE", "DIAGNOSIS")
     */
    @Column(name = "entity_code", nullable = false, length = 50)
    private String entityCode;
    
    /**
     * Display label (e.g., "Tên người bệnh", "Ngày tháng")
     */
    @Column(name = "display_label", nullable = false, length = 100)
    private String displayLabel;
    
    /**
     * Description of this entity type
     */
    @Column(name = "description", length = 500)
    private String description;
    
    /**
     * Color for UI display (e.g., "blue", "#3B82F6")
     */
    @Column(name = "color", length = 50)
    private String color;
    
    /**
     * Icon name for UI display (e.g., "Users", "Calendar")
     */
    @Column(name = "icon", length = 50)
    private String icon;
    
    /**
     * Display order for sorting
     */
    @Column(name = "display_order")
    private Integer displayOrder = 0;
    
    /**
     * Whether this entity type is active
     */
    @Column(name = "active")
    private Boolean active = true;
    
    /**
     * Examples of this entity type
     */
    @Column(name = "examples", length = 1000)
    private String examples;
    
    @Column(name = "created_at")
    private LocalDateTime createdAt;
    
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    public NEREntityType() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
        this.active = true;  // Ensure active is always true by default
        this.displayOrder = 0;
    }
    
    public NEREntityType(String entityCode, String displayLabel) {
        this();
        this.entityCode = entityCode;
        this.displayLabel = displayLabel;
    }
    
    public NEREntityType(String templateId, String entityCode, String displayLabel) {
        this(entityCode, displayLabel);
        this.templateId = templateId;
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
    
    public String getEntityCode() {
        return entityCode;
    }
    
    public void setEntityCode(String entityCode) {
        this.entityCode = entityCode;
        this.updatedAt = LocalDateTime.now();
    }
    
    public String getDisplayLabel() {
        return displayLabel;
    }
    
    public void setDisplayLabel(String displayLabel) {
        this.displayLabel = displayLabel;
        this.updatedAt = LocalDateTime.now();
    }
    
    public String getDescription() {
        return description;
    }
    
    public void setDescription(String description) {
        this.description = description;
        this.updatedAt = LocalDateTime.now();
    }
    
    public String getColor() {
        return color;
    }
    
    public void setColor(String color) {
        this.color = color;
        this.updatedAt = LocalDateTime.now();
    }
    
    public String getIcon() {
        return icon;
    }
    
    public void setIcon(String icon) {
        this.icon = icon;
        this.updatedAt = LocalDateTime.now();
    }
    
    public Integer getDisplayOrder() {
        return displayOrder;
    }
    
    public void setDisplayOrder(Integer displayOrder) {
        this.displayOrder = displayOrder;
        this.updatedAt = LocalDateTime.now();
    }
    
    public Boolean getActive() {
        return active;
    }
    
    public void setActive(Boolean active) {
        this.active = active;
        this.updatedAt = LocalDateTime.now();
    }
    
    public String getExamples() {
        return examples;
    }
    
    public void setExamples(String examples) {
        this.examples = examples;
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
}
