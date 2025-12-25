package com.deepapp.vn.io.ZZ.A0.ZZA0_0100.controller;

import com.deepapp.vn.io.ZZ.A0.ZZA0_0100.model.DocumentRequest;
import com.deepapp.vn.io.ZZ.A0.ZZA0_0100.service.DocumentProcessingService;
import com.deepapp.vn.io.ZZ.A0.ZZA0_0100.service.DocumentStreamRegistry;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Controller for document processing (TIFF, TIF, PDF)
 * Endpoint: /ZZ/A0/ZZA0_0100
 */
@RestController
@RequestMapping("/ZZ/A0/ZZA0_0100")
public class ZZA0_0100Controller {

    @Autowired
    private DocumentProcessingService documentProcessingService;
    
    @Autowired
    private DocumentStreamRegistry streamRegistry;
    
    private final ExecutorService executorService = Executors.newCachedThreadPool();

    /**
     * Health check endpoint
     */
    @GetMapping
    public Map<String, Object> healthCheck() {
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("service", "Document Processing Service");
        response.put("endpoint", "/ZZ/A0/ZZA0_0100");
        response.put("supportedFormats", new String[]{"tiff", "tif", "pdf"});
        response.put("usage", Map.of(
            "upload", "POST /ZZ/A0/ZZA0_0100 with multipart file - returns metadata only",
            "getPage", "POST /ZZ/A0/ZZA0_0100/page?pageNumber=1 - returns single page (fast)",
            "fullProcess", "POST /ZZ/A0/ZZA0_0100/process - returns all pages (slow)"
        ));
        return response;
    }

    /**
     * Upload and process document (file upload) - FAST METADATA ONLY
     * Returns only metadata (page count, format), no page data
     * Client can then request individual pages using /page endpoint
     */
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Map<String, Object>> uploadDocument(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "options", required = false) String options) {
        
        try {
            // Validate file
            if (file.isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "error", "File is empty"
                ));
            }

            String filename = file.getOriginalFilename();
            String fileExtension = getFileExtension(filename);

            // Validate file type
            if (!isValidFileType(fileExtension)) {
                return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "error", "Unsupported file format. Only TIFF, TIF, PDF are supported",
                    "filename", filename
                ));
            }

            // Get metadata only (FAST) - no page data
            Map<String, Object> result = documentProcessingService.getDocumentInfo(
                file.getBytes(), 
                filename
            );

            return ResponseEntity.ok(result);

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                "success", false,
                "error", e.getMessage()
            ));
        }
    }

    /**
     * Stream document pages using C++ → Java gRPC streaming (NEW SMART DESIGN)
     * C++ worker actively pushes each page to Java via gRPC events
     * Java forwards to client via SSE - No polling needed!
     */
    @PostMapping(value = "/stream", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public SseEmitter streamDocumentPages(@RequestParam("file") MultipartFile file) {
        
        // Create SSE emitter with 5 minute timeout
        SseEmitter emitter = new SseEmitter(300_000L);
        
        // Generate unique request ID
        String requestId = UUID.randomUUID().toString();
        
        // Register emitter to receive C++ events
        streamRegistry.registerStream(requestId, emitter);
        
        executorService.execute(() -> {
            try {
                String filename = file.getOriginalFilename();
                byte[] fileData = file.getBytes();
                
                // Send initial status
                emitter.send(SseEmitter.event()
                    .name("status")
                    .data(Map.of(
                        "message", "C++ worker processing...",
                        "requestId", requestId,
                        "filename", filename
                    )));
                
                // Trigger C++ processing - C++ will stream pages back via gRPC!
                documentProcessingService.processDocumentStreaming(
                    fileData, 
                    filename,
                    requestId
                );
                
                // C++ sends: metadata → page1 → page2 → ... → complete
                // CppWorkerClient receives & forwards to this emitter automatically
                
            } catch (Exception e) {
                try {
                    emitter.send(SseEmitter.event()
                        .name("error")
                        .data(Map.of("error", e.getMessage())));
                } catch (Exception ignored) {}
                emitter.completeWithError(e);
                streamRegistry.removeStream(requestId);
            }
        });
        
        return emitter;
    }

    /**
     * OLD METHOD - Stream document pages (Java pulls each page)
     * Uses Server-Sent Events to stream each page as it's processed
     */
    @PostMapping(value = "/stream-old", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public SseEmitter streamDocumentPagesOld(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "startPage", defaultValue = "1") int startPage,
            @RequestParam(value = "maxPages", defaultValue = "10") int maxPages) {
        
        // Create SSE emitter with 5 minute timeout
        SseEmitter emitter = new SseEmitter(300_000L);
        
        executorService.execute(() -> {
            try {
                String filename = file.getOriginalFilename();
                byte[] fileData = file.getBytes();
                
                // Send initial metadata
                Map<String, Object> metadata = documentProcessingService.getDocumentInfo(fileData, filename);
                emitter.send(SseEmitter.event()
                    .name("metadata")
                    .data(metadata));
                
                int totalPages = (int) metadata.getOrDefault("pageCount", 0);
                int endPage = Math.min(startPage + maxPages - 1, totalPages);
                
                // Process and stream each page
                for (int pageNum = startPage; pageNum <= endPage; pageNum++) {
                    try {
                        // Get single page
                        Map<String, Object> pageData = documentProcessingService.getSpecificPage(
                            fileData, filename, pageNum
                        );
                        
                        // Send page immediately
                        emitter.send(SseEmitter.event()
                            .name("page")
                            .data(pageData));
                        
                    } catch (Exception e) {
                        emitter.send(SseEmitter.event()
                            .name("error")
                            .data(Map.of(
                                "pageNumber", pageNum,
                                "error", e.getMessage()
                            )));
                    }
                }
                
                // Send completion event
                emitter.send(SseEmitter.event()
                    .name("complete")
                    .data(Map.of(
                        "success", true,
                        "totalPagesProcessed", endPage - startPage + 1,
                        "filename", filename
                    )));
                
                emitter.complete();
                
            } catch (Exception e) {
                try {
                    emitter.send(SseEmitter.event()
                        .name("error")
                        .data(Map.of("error", e.getMessage())));
                } catch (Exception ignored) {}
                emitter.completeWithError(e);
            }
        });
        
        return emitter;
    }

    /**     * Process document with base64 encoded data
     */
    @PostMapping(value = "/process", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Map<String, Object>> processDocument(@RequestBody DocumentRequest request) {
        try {
            // Validate request
            if (request.getData() == null || request.getData().isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "error", "Document data is required"
                ));
            }

            if (request.getFilename() == null || request.getFilename().isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "error", "Filename is required"
                ));
            }

            String fileExtension = getFileExtension(request.getFilename());
            if (!isValidFileType(fileExtension)) {
                return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "error", "Unsupported file format. Only TIFF, TIF, PDF are supported",
                    "filename", request.getFilename()
                ));
            }

            // Decode base64 and process
            byte[] fileBytes = java.util.Base64.getDecoder().decode(request.getData());
            Map<String, Object> result = documentProcessingService.processDocument(
                fileBytes,
                request.getFilename(),
                request.getOptions()
            );

            return ResponseEntity.ok(result);

        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "error", "Invalid base64 data: " + e.getMessage()
            ));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                "success", false,
                "error", e.getMessage()
            ));
        }
    }

    /**
     * Get specific page from a document
     */
    @PostMapping("/page")
    public ResponseEntity<Map<String, Object>> getPage(
            @RequestParam("file") MultipartFile file,
            @RequestParam("pageNumber") int pageNumber) {
        
        try {
            if (file.isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "error", "File is empty"
                ));
            }

            String filename = file.getOriginalFilename();
            String fileExtension = getFileExtension(filename);

            if (!isValidFileType(fileExtension)) {
                return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "error", "Unsupported file format"
                ));
            }

            Map<String, Object> result = documentProcessingService.getSpecificPage(
                file.getBytes(),
                filename,
                pageNumber
            );

            return ResponseEntity.ok(result);

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                "success", false,
                "error", e.getMessage()
            ));
        }
    }

    /**
     * Helper method to extract file extension
     */
    private String getFileExtension(String filename) {
        if (filename == null || !filename.contains(".")) {
            return "";
        }
        return filename.substring(filename.lastIndexOf(".") + 1).toLowerCase();
    }

    /**
     * Helper method to validate file type
     */
    private boolean isValidFileType(String extension) {
        return extension.equals("tiff") || 
               extension.equals("tif") || 
               extension.equals("pdf");
    }
}
