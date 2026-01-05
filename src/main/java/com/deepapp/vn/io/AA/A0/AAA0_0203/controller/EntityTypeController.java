package com.deepapp.vn.io.AA.A0.AAA0_0203.controller;

import com.deepapp.vn.io.AA.A0.AAA0_0203.model.NERTrainingResponse;
import com.deepapp.vn.io.AA.A0.AAA0_0203.service.NERTrainingDataService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Entity Type Management Controller
 * Handles CRUD operations for NER entity types
 */
@RestController
@RequestMapping("/AA/A0/AAA0_0203/entity-types")
@Tag(name = "Entity Types", description = "Dynamic entity type management for NER training")
public class EntityTypeController {

    private static final Logger logger = LoggerFactory.getLogger(EntityTypeController.class);

    @Autowired
    private NERTrainingDataService nerTrainingDataService;

    /**
     * Get entity types for a template (including global ones)
     */
    @GetMapping
    @Operation(summary = "Get entity types", description = "Get all entity types for a template or global entity types")
    public ResponseEntity<Map<String, Object>> getEntityTypes(
            @RequestParam(value = "template_id", required = false) String templateId) {
        
        logger.info("Getting entity types for template: {}", templateId);
        
        try {
            var entityTypes = nerTrainingDataService.getEntityTypes(templateId);
            
            // Convert to maps for proper JSON serialization
            List<Map<String, Object>> entityTypeMaps = entityTypes.stream()
                .map(this::convertToMap)
                .collect(java.util.stream.Collectors.toList());
            
            Map<String, Object> response = new HashMap<>();
            response.put("status", "success");
            response.put("entity_types", entityTypeMaps);
            response.put("count", entityTypeMaps.size());
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("Error getting entity types", e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("status", "error");
            errorResponse.put("message", e.getMessage());
            return ResponseEntity.internalServerError().body(errorResponse);
        }
    }

    /**
     * Get global entity types only
     */
    @GetMapping("/global")
    @Operation(summary = "Get global entity types", description = "Get all global entity types")
    public ResponseEntity<Map<String, Object>> getGlobalEntityTypes() {
        logger.info("Getting global entity types");
        
        try {
            var entityTypes = nerTrainingDataService.getGlobalEntityTypes();
            
            // Convert to maps for proper JSON serialization
            List<Map<String, Object>> entityTypeMaps = entityTypes.stream()
                .map(this::convertToMap)
                .collect(java.util.stream.Collectors.toList());
            
            Map<String, Object> response = new HashMap<>();
            response.put("status", "success");
            response.put("entity_types", entityTypeMaps);
            response.put("count", entityTypeMaps.size());
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("Error getting global entity types", e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("status", "error");
            errorResponse.put("message", e.getMessage());
            return ResponseEntity.internalServerError().body(errorResponse);
        }
    }

    /**
     * Create new entity type
     */
    @PostMapping
    @Operation(summary = "Create entity type", description = "Create a new entity type")
    public ResponseEntity<NERTrainingResponse> createEntityType(@RequestBody Map<String, Object> request) {
        logger.info("Creating entity type: {}", request.get("entity_code"));
        
        try {
            String templateId = (String) request.get("template_id");
            String entityCode = (String) request.get("entity_code");
            String displayLabel = (String) request.get("display_label");
            String description = (String) request.get("description");
            String color = (String) request.get("color");
            String icon = (String) request.get("icon");
            Integer displayOrder = request.get("display_order") != null ? 
                ((Number) request.get("display_order")).intValue() : null;
            String examples = (String) request.get("examples");
            
            NERTrainingResponse response = nerTrainingDataService.createEntityType(
                templateId, entityCode, displayLabel, description, color, icon, displayOrder, examples
            );
            
            if ("success".equals(response.getStatus())) {
                return ResponseEntity.ok(response);
            } else {
                return ResponseEntity.badRequest().body(response);
            }
            
        } catch (Exception e) {
            logger.error("Error creating entity type", e);
            return ResponseEntity.internalServerError()
                .body(NERTrainingResponse.error("Failed to create entity type: " + e.getMessage()));
        }
    }

    /**
     * Update entity type
     */
    @PutMapping("/{id}")
    @Operation(summary = "Update entity type", description = "Update an existing entity type")
    public ResponseEntity<NERTrainingResponse> updateEntityType(
            @PathVariable Long id,
            @RequestBody Map<String, Object> request) {
        
        logger.info("Updating entity type: {}", id);
        
        try {
            String entityCode = (String) request.get("entity_code");
            String displayLabel = (String) request.get("display_label");
            String description = (String) request.get("description");
            String color = (String) request.get("color");
            String icon = (String) request.get("icon");
            Integer displayOrder = request.get("display_order") != null ? 
                ((Number) request.get("display_order")).intValue() : null;
            Boolean active = (Boolean) request.get("active");
            String examples = (String) request.get("examples");
            
            NERTrainingResponse response = nerTrainingDataService.updateEntityType(
                id, entityCode, displayLabel, description, color, icon, displayOrder, active, examples
            );
            
            if ("success".equals(response.getStatus())) {
                return ResponseEntity.ok(response);
            } else {
                return ResponseEntity.badRequest().body(response);
            }
            
        } catch (Exception e) {
            logger.error("Error updating entity type", e);
            return ResponseEntity.internalServerError()
                .body(NERTrainingResponse.error("Failed to update entity type: " + e.getMessage()));
        }
    }

    /**
     * Delete entity type
     */
    @DeleteMapping("/{id}")
    @Operation(summary = "Delete entity type", description = "Delete (deactivate) an entity type")
    public ResponseEntity<NERTrainingResponse> deleteEntityType(@PathVariable Long id) {
        logger.info("Deleting entity type: {}", id);
        
        try {
            NERTrainingResponse response = nerTrainingDataService.deleteEntityType(id);
            
            if ("success".equals(response.getStatus())) {
                return ResponseEntity.ok(response);
            } else {
                return ResponseEntity.badRequest().body(response);
            }
            
        } catch (Exception e) {
            logger.error("Error deleting entity type", e);
            return ResponseEntity.internalServerError()
                .body(NERTrainingResponse.error("Failed to delete entity type: " + e.getMessage()));
        }
    }

    /**
     * Initialize default entity types
     */
    @PostMapping("/initialize")
    @Operation(summary = "Initialize default entity types", description = "Create default medical entity types")
    public ResponseEntity<NERTrainingResponse> initializeDefaultEntityTypes() {
        logger.info("Initializing default entity types");
        
        try {
            NERTrainingResponse response = nerTrainingDataService.initializeDefaultEntityTypes();
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("Error initializing entity types", e);
            return ResponseEntity.internalServerError()
                .body(NERTrainingResponse.error("Failed to initialize: " + e.getMessage()));
        }
    }
    
    /**
     * Convert NEREntityType to Map for JSON serialization
     */
    private Map<String, Object> convertToMap(com.deepapp.vn.io.AA.A0.AAA0_0203.entity.NEREntityType entityType) {
        Map<String, Object> map = new HashMap<>();
        map.put("id", entityType.getId());
        map.put("template_id", entityType.getTemplateId());
        map.put("entity_code", entityType.getEntityCode());
        map.put("display_label", entityType.getDisplayLabel());
        map.put("description", entityType.getDescription());
        map.put("color", entityType.getColor());
        map.put("icon", entityType.getIcon());
        map.put("display_order", entityType.getDisplayOrder());
        map.put("active", entityType.getActive());
        map.put("examples", entityType.getExamples());
        map.put("created_at", entityType.getCreatedAt() != null ? entityType.getCreatedAt().toString() : null);
        map.put("updated_at", entityType.getUpdatedAt() != null ? entityType.getUpdatedAt().toString() : null);
        return map;
    }
}
