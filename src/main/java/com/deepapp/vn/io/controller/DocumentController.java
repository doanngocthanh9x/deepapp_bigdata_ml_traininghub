package com.deepapp.vn.io.controller;

import com.deepapp.vn.io.storage.dto.PageDTO;
import com.deepapp.vn.io.storage.entity.DocumentEntity;
import com.deepapp.vn.io.storage.service.DocumentManagementService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * REST API for document management
 */
@RestController
@RequestMapping("/api/management/documents")
public class DocumentController {

    private static final Logger logger = LoggerFactory.getLogger(DocumentController.class);

    @Autowired
    private DocumentManagementService documentManagementService;

    /**
     * Get all documents
     */
    @GetMapping
    public ResponseEntity<List<Map<String, Object>>> getAllDocuments() {
        try {
            List<DocumentEntity> documents = documentManagementService.getAllDocuments();

            List<Map<String, Object>> result = documents.stream()
                .map(this::convertDocumentToMap)
                .collect(Collectors.toList());

            return ResponseEntity.ok(result);
        } catch (Exception e) {
            logger.error("Error getting documents", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Get document statistics
     */
    @GetMapping("/statistics/detailed")
    public ResponseEntity<Map<String, Object>> getDetailedStatistics() {
        try {
            List<DocumentEntity> allDocs = documentManagementService.getAllDocuments();

            long totalDocuments = allDocs.size();
            long completedDocs = allDocs.stream().filter(d -> "completed".equals(d.getStatus())).count();
            long processingDocs = allDocs.stream().filter(d -> "processing".equals(d.getStatus())).count();
            long failedDocs = allDocs.stream().filter(d -> "failed".equals(d.getStatus())).count();

            Map<String, Long> statusBreakdown = new HashMap<>();
            statusBreakdown.put("completed", completedDocs);
            statusBreakdown.put("processing", processingDocs);
            statusBreakdown.put("failed", failedDocs);

            long totalStorageSize = allDocs.stream()
                .mapToLong(DocumentEntity::getFileSize)
                .sum();

            Map<String, Object> result = new HashMap<>();
            result.put("totalDocuments", totalDocuments);
            result.put("statusBreakdown", statusBreakdown);
            result.put("totalStorageSize", totalStorageSize);

            return ResponseEntity.ok(result);
        } catch (Exception e) {
            logger.error("Error getting statistics", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Get pages for a document
     */
    @GetMapping("/{requestId}/pages")
    public ResponseEntity<List<Map<String, Object>>> getDocumentPages(@PathVariable String requestId) {
        try {
            List<PageDTO> pages = documentManagementService.getPages(requestId);

            if (pages.isEmpty()) {
                return ResponseEntity.notFound().build();
            }

            List<Map<String, Object>> result = pages.stream()
                .map(this::convertPageToMap)
                .collect(Collectors.toList());

            return ResponseEntity.ok(result);
        } catch (Exception e) {
            logger.error("Error getting pages for requestId: " + requestId, e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Delete document
     */
    @DeleteMapping("/{requestId}")
    public ResponseEntity<Map<String, Object>> deleteDocument(@PathVariable String requestId) {
        try {
            boolean deleted = documentManagementService.deleteDocument(requestId);

            if (deleted) {
                return ResponseEntity.ok(Map.of("success", true, "message", "Document deleted"));
            } else {
                return ResponseEntity.notFound().build();
            }
        } catch (Exception e) {
            logger.error("Error deleting document: " + requestId, e);
            return ResponseEntity.internalServerError().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Delete multiple documents
     */
    @PostMapping("/delete-batch")
    public ResponseEntity<Map<String, Object>> deleteBatch(@RequestBody List<String> requestIds) {
        try {
            int deletedCount = 0;
            for (String requestId : requestIds) {
                if (documentManagementService.deleteDocument(requestId)) {
                    deletedCount++;
                }
            }

            return ResponseEntity.ok(Map.of(
                "success", true,
                "deletedCount", deletedCount,
                "message", "Deleted " + deletedCount + " documents"
            ));
        } catch (Exception e) {
            logger.error("Error deleting batch documents", e);
            return ResponseEntity.internalServerError().body(Map.of("error", e.getMessage()));
        }
    }

    private Map<String, Object> convertDocumentToMap(DocumentEntity doc) {
        Map<String, Object> map = new HashMap<>();
        map.put("requestId", doc.getRequestId());
        map.put("filename", doc.getFilename());
        map.put("format", doc.getFormat());
        map.put("pageCount", doc.getPageCount());
        map.put("fileSize", doc.getFileSize());
        map.put("status", doc.getStatus());
        map.put("createdAt", doc.getCreatedAt());
        map.put("updatedAt", doc.getUpdatedAt());
        return map;
    }

    private Map<String, Object> convertPageToMap(PageDTO page) {
        Map<String, Object> map = new HashMap<>();
        map.put("pageNumber", page.getPageNumber());
        map.put("width", page.getWidth());
        map.put("height", page.getHeight());
        map.put("dpi", page.getDpi());
        map.put("format", page.getFormat());
        map.put("imagePath", page.getImagePath());
        map.put("text", page.getText());
        map.put("status", page.getStatus());
        // Note: filePath, worker, timestamp are not available in PageDTO
        return map;
    }
}