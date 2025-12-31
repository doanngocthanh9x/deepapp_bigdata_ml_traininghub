package com.deepapp.vn.io.AA.A0.AAA0_0202.controller;

import com.deepapp.vn.io.AA.A0.AAA0_0202.model.RAGOcrRequest;
import com.deepapp.vn.io.AA.A0.AAA0_0202.model.RAGOcrResponse;
import com.deepapp.vn.io.AA.A0.AAA0_0202.service.RAGOcrService;
import com.deepapp.utils.PathUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/**
 * AAA0_0202 - OCR with RAG System Controller
 *
 * Provides OCR text extraction with RAG-based information retrieval
 * and document template configuration
 */
@RestController
@RequestMapping("/AA/A0/AAA0_0202")
@Tag(name = "AAA0_0202 - OCR with RAG System", description = "OCR with RAG-based document processing and template configuration")
public class AAA0_0202Controller {

    private static final Logger logger = LoggerFactory.getLogger(AAA0_0202Controller.class);

    @Autowired
    private RAGOcrService ragOcrService;

    /**
     * Health check endpoint
     */
    @GetMapping
    @Operation(summary = "Health check", description = "Check if RAG OCR service is available")
    public ResponseEntity<Object> healthCheck() {
        return ResponseEntity.ok()
            .body(java.util.Map.of(
                "service", "AAA0_0202 - OCR with RAG System",
                "status", "active",
                "features", java.util.List.of("ocr", "rag", "template_config"),
                "languages", java.util.List.of("vi", "en"),
                "timestamp", System.currentTimeMillis()
            ));
    }

    /**
     * Process OCR with RAG system (JSON request)
     */
    @PostMapping("/process")
    @Operation(
        summary = "Process Document with RAG",
        description = "Extract text from document and apply RAG-based information retrieval"
    )
    public ResponseEntity<RAGOcrResponse> processDocument(@RequestBody RAGOcrRequest request) {
        logger.info("RAG OCR processing request received: template={}, query={}",
                   request.getTemplateId(), request.getQuery());

        try {
            RAGOcrResponse response = ragOcrService.processRAGOcr(request);

            if (response.isSuccess()) {
                logger.info("RAG OCR successful: extracted {} fields, answer confidence={}",
                          response.getExtractedFields() != null ? response.getExtractedFields().size() : 0,
                          response.getAnswerConfidence());
                return ResponseEntity.ok(response);
            } else {
                logger.warn("RAG OCR failed: {}", response.getError());
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
            }

        } catch (Exception e) {
            logger.error("RAG OCR processing error", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(RAGOcrResponse.error("Internal error: " + e.getMessage()));
        }
    }

    /**
     * Process OCR with RAG system (Multipart file upload)
     */
    @PostMapping(value = "/process", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(
        summary = "Process Document with RAG (File Upload)",
        description = "Upload and process document with RAG-based information retrieval"
    )
    public ResponseEntity<RAGOcrResponse> processDocumentWithFile(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "template_id", required = false) String templateId,
            @RequestParam(value = "query", required = false) String query,
            @RequestParam(value = "language", defaultValue = "vi") String language) {

        logger.info("RAG OCR file processing request received: filename={}, template={}, query={}",
                   file.getOriginalFilename(), templateId, query);

        try {
            // Save file to test directory using PathUtils
            String testDir = PathUtils.getTestDataPath();
            java.nio.file.Path testPath = java.nio.file.Paths.get(testDir);
            java.nio.file.Files.createDirectories(testPath);
            
            String filename = "upload_" + System.currentTimeMillis() + "_" + 
                            java.util.UUID.randomUUID().toString().substring(0, 8) + 
                            getFileExtension(file.getOriginalFilename());
            java.nio.file.Path filePath = testPath.resolve(filename);
            java.nio.file.Files.write(filePath, file.getBytes());
            
            String imagePath = filename; // Relative path for Python worker

            // Create request
            RAGOcrRequest request = new RAGOcrRequest();
            request.setImagePath(imagePath);
            request.setTemplateId(templateId);
            request.setQuery(query);
            request.setLanguage(language);

            RAGOcrResponse response = ragOcrService.processRAGOcr(request);

            if (response.isSuccess()) {
                logger.info("RAG OCR file processing successful: extracted {} fields, answer confidence={}",
                          response.getExtractedFields() != null ? response.getExtractedFields().size() : 0,
                          response.getAnswerConfidence());
                return ResponseEntity.ok(response);
            } else {
                logger.warn("RAG OCR file processing failed: {}", response.getError());
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
            }

        } catch (Exception e) {
            logger.error("RAG OCR file processing error", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(RAGOcrResponse.error("Internal error: " + e.getMessage()));
        }
    }

    /**
     * Query extracted information
     */
    @PostMapping("/query")
    @Operation(
        summary = "Query Document Information",
        description = "Query specific information from previously processed document"
    )
    public ResponseEntity<RAGOcrResponse> queryDocument(@RequestBody RAGOcrRequest request) {
        if (request.getQuery() == null || request.getQuery().trim().isEmpty()) {
            return ResponseEntity.badRequest()
                .body(RAGOcrResponse.error("Query is required"));
        }

        logger.info("Document query received: {}", request.getQuery());

        try {
            RAGOcrResponse response = ragOcrService.queryDocument(request);

            if (response.isSuccess()) {
                logger.info("Query successful: confidence={}", response.getAnswerConfidence());
                return ResponseEntity.ok(response);
            } else {
                logger.warn("Query failed: {}", response.getError());
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
            }

        } catch (Exception e) {
            logger.error("Query processing error", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(RAGOcrResponse.error("Internal error: " + e.getMessage()));
        }
    }

    /**
     * Get available document templates
     */
    @GetMapping("/templates")
    @Operation(summary = "List Templates", description = "Get list of available document templates")
    public ResponseEntity<Object> listTemplates() {
        try {
            var templates = ragOcrService.getAvailableTemplates();
            return ResponseEntity.ok()
                .body(java.util.Map.of(
                    "templates", templates,
                    "count", templates.size()
                ));
        } catch (Exception e) {
            logger.error("Error listing templates", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(java.util.Map.of("error", e.getMessage()));
        }
    }

    /**
     * Save document template configuration
     */
    @PostMapping("/templates")
    @Operation(
        summary = "Save Template",
        description = "Save or update document template configuration"
    )
    public ResponseEntity<Object> saveTemplate(@RequestBody Object templateConfig) {
        try {
            boolean success = ragOcrService.saveTemplate(templateConfig);
            if (success) {
                return ResponseEntity.ok()
                    .body(java.util.Map.of("message", "Template saved successfully"));
            } else {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(java.util.Map.of("error", "Failed to save template"));
            }
        } catch (Exception e) {
            logger.error("Error saving template", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(java.util.Map.of("error", e.getMessage()));
        }
    }

    /**
     * Delete document template
     */
    @DeleteMapping("/templates/{templateId}")
    @Operation(summary = "Delete Template", description = "Delete a document template")
    public ResponseEntity<Object> deleteTemplate(@PathVariable String templateId) {
        try {
            boolean success = ragOcrService.deleteTemplate(templateId);
            if (success) {
                return ResponseEntity.ok()
                    .body(java.util.Map.of("message", "Template deleted successfully"));
            } else {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(java.util.Map.of("error", "Template not found"));
            }
        } catch (Exception e) {
            logger.error("Error deleting template", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(java.util.Map.of("error", e.getMessage()));
        }
    }

    /**
     * Get file extension from filename
     */
    private String getFileExtension(String filename) {
        if (filename == null || filename.lastIndexOf('.') == -1) {
            return ".png"; // Default extension
        }
        return filename.substring(filename.lastIndexOf('.'));
    }
}