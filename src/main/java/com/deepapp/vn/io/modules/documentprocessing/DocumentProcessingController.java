package com.deepapp.vn.io.modules.documentprocessing;

import com.deepapp.vn.io.modules.documentprocessing.DocumentProcessingService.DocumentResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * Document Processing REST Controller
 * Exposes endpoints for document processing operations
 */
@RestController
@RequestMapping("/api/documents")
public class DocumentProcessingController {

    @Autowired
    private DocumentProcessingService documentProcessingService;

    @GetMapping("/health")
    public ResponseEntity<Map<String, String>> health() {
        Map<String, String> response = new HashMap<>();
        response.put("status", "healthy");
        response.put("module", "document-processing");
        return ResponseEntity.ok(response);
    }

    /**
     * Process a document (OCR + NER)
     */
    @PostMapping("/process")
    public ResponseEntity<Map<String, Object>> processDocument(@RequestBody ProcessRequest request) {
        try {
            DocumentResult result = documentProcessingService
                    .processDocument(request.getImagePath())
                    .get();

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("text", result.getText());
            response.put("entities", result.getEntities());
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("error", e.getMessage());
            return ResponseEntity.status(500).body(error);
        }
    }

    /**
     * Process document with language
     */
    @PostMapping("/process/language")
    public ResponseEntity<Map<String, Object>> processDocumentWithLanguage(@RequestBody ProcessRequest request) {
        try {
            DocumentResult result = documentProcessingService
                    .processDocument(request.getImagePath(), request.getLanguage())
                    .get();

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("text", result.getText());
            response.put("entities", result.getEntities());
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("error", e.getMessage());
            return ResponseEntity.status(500).body(error);
        }
    }

    /**
     * Extract text only (OCR)
     */
    @PostMapping("/ocr")
    public ResponseEntity<Map<String, Object>> extractText(@RequestBody ProcessRequest request) {
        try {
            String text = documentProcessingService
                    .extractText(request.getImagePath())
                    .get();

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("text", text);
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("error", e.getMessage());
            return ResponseEntity.status(500).body(error);
        }
    }

    /**
     * Extract entities only (NER)
     */
    @PostMapping("/ner")
    public ResponseEntity<Map<String, Object>> extractEntities(@RequestBody NerRequest request) {
        try {
            String entities = documentProcessingService
                    .extractEntities(request.getText())
                    .get();

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("entities", entities);
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("error", e.getMessage());
            return ResponseEntity.status(500).body(error);
        }
    }

    // Request DTOs
    public static class ProcessRequest {
        private String imagePath;
        private String language;

        public String getImagePath() {
            return imagePath;
        }

        public void setImagePath(String imagePath) {
            this.imagePath = imagePath;
        }

        public String getLanguage() {
            return language;
        }

        public void setLanguage(String language) {
            this.language = language;
        }
    }

    public static class NerRequest {
        private String text;

        public String getText() {
            return text;
        }

        public void setText(String text) {
            this.text = text;
        }
    }
}
