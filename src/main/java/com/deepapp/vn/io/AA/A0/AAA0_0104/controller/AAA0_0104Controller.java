package com.deepapp.vn.io.AA.A0.AAA0_0104.controller;

import com.deepapp.vn.io.storage.dto.PageDTO;
import com.deepapp.vn.io.storage.entity.DocumentEntity;
import com.deepapp.vn.io.storage.service.DocumentManagementService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/AA/A0/AAA0_0104")
@Component("aaa0_0104Controller")
@Tag(name = "AAA0_0104 - Document Management", description = "APIs for managing processed documents")
public class AAA0_0104Controller {
    
    private final DocumentManagementService documentService;
    
    public AAA0_0104Controller(DocumentManagementService documentService) {
        this.documentService = documentService;
    }
    
    @GetMapping
    @Operation(summary = "Get all documents")
    public ResponseEntity<List<DocumentEntity>> getAllDocuments() {
        return ResponseEntity.ok(documentService.getAllDocuments());
    }
    
    @GetMapping("/status/{status}")
    @Operation(summary = "Get documents by status")
    public ResponseEntity<List<DocumentEntity>> getDocumentsByStatus(@PathVariable String status) {
        return ResponseEntity.ok(documentService.getDocumentsByStatus(status));
    }
    
    @GetMapping("/{requestId}")
    @Operation(summary = "Get document details")
    public ResponseEntity<DocumentEntity> getDocument(@PathVariable String requestId) {
        return documentService.getDocument(requestId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
    
    @GetMapping("/{requestId}/pages")
    @Operation(summary = "Get all pages for a document")
    public ResponseEntity<List<PageDTO>> getPages(@PathVariable String requestId) {
        return ResponseEntity.ok(documentService.getPages(requestId));
    }
    
    @GetMapping("/{requestId}/pages/{pageNumber}")
    @Operation(summary = "Get specific page")
    public ResponseEntity<PageDTO> getPage(
            @PathVariable String requestId,
            @PathVariable int pageNumber) {
        return documentService.getPage(requestId, pageNumber)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
    
    @DeleteMapping("/{requestId}")
    @Operation(summary = "Delete a document")
    public ResponseEntity<Map<String, Object>> deleteDocument(@PathVariable String requestId) {
        boolean deleted = documentService.deleteDocument(requestId);
        if (deleted) {
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Document deleted successfully",
                    "requestId", requestId
            ));
        } else {
            return ResponseEntity.notFound().build();
        }
    }
    
    @PostMapping("/delete-batch")
    @Operation(summary = "Delete multiple documents")
    public ResponseEntity<Map<String, Object>> deleteDocuments(@RequestBody List<String> requestIds) {
        int deleted = documentService.deleteDocuments(requestIds);
        return ResponseEntity.ok(Map.of(
                "success", true,
                "deleted", deleted,
                "total", requestIds.size()
        ));
    }
    
    @GetMapping("/statistics")
    @Operation(summary = "Get database statistics")
    public ResponseEntity<Map<String, Object>> getStatistics() {
        return ResponseEntity.ok(documentService.getStatistics());
    }
    
    @GetMapping("/statistics/detailed")
    @Operation(summary = "Get detailed statistics")
    public ResponseEntity<Map<String, Object>> getDetailedStatistics() {
        return ResponseEntity.ok(documentService.getDetailedStatistics());
    }
    
    @PostMapping("/cleanup")
    @Operation(summary = "Manual cleanup old documents")
    public ResponseEntity<Map<String, Object>> cleanupOldDocuments(
            @RequestParam(defaultValue = "7") int days) {
        int deleted = documentService.cleanupOldDocuments(days);
        return ResponseEntity.ok(Map.of(
                "success", true,
                "deleted", deleted,
                "retention_days", days
        ));
    }
    
    @PostMapping("/cleanup/retention")
    @Operation(summary = "Cleanup by retention period")
    public ResponseEntity<Map<String, Object>> cleanupByRetention(
            @RequestParam int amount,
            @RequestParam String unit) {
        try {
            int deleted = documentService.cleanupByRetention(amount, unit);
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "deleted", deleted,
                    "retention", amount + " " + unit
            ));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "error", e.getMessage()
            ));
        }
    }
    
    @PostMapping("/cleanup/failed")
    @Operation(summary = "Cleanup failed documents")
    public ResponseEntity<Map<String, Object>> cleanupFailedDocuments() {
        int deleted = documentService.cleanupFailedDocuments();
        return ResponseEntity.ok(Map.of(
                "success", true,
                "deleted", deleted
        ));
    }
    
    @DeleteMapping("/clear-all")
    @Operation(summary = "Clear all data (DANGEROUS!)")
    public ResponseEntity<Map<String, Object>> clearAllData() {
        documentService.clearAllData();
        return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "All data cleared"
        ));
    }
}
