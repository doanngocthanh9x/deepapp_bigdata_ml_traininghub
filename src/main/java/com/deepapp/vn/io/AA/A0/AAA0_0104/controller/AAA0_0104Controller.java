package com.deepapp.vn.io.AA.A0.AAA0_0104.controller;

import com.deepapp.vn.io.AA.A0.AAA0_0104.dto.BBoxDTO;
import com.deepapp.vn.io.AA.A0.AAA0_0104.dto.FileDTO;
import com.deepapp.vn.io.AA.A0.AAA0_0104.dto.PageDTO;
import com.deepapp.vn.io.AA.A0.AAA0_0104.service.BBoxService;
import com.deepapp.vn.io.AA.A0.AAA0_0104.service.FileService;
import com.deepapp.vn.io.AA.A0.AAA0_0104.service.PageService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import com.deepapp.vn.io.AA.A0.AAA0_0104.service.DocumentProcessingService;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

/**
 * AAA0_0104 Controller - Document Processing với SQLite
 * Quản lý files, pages và bounding boxes
 */
@RestController
@RequestMapping("/AA/A0/AAA0_0104")
@Tag(name = "AAA0_0104 - Document Processing", description = "APIs for document processing with SQLite")
public class AAA0_0104Controller {

    private final FileService fileService;
    private final PageService pageService;
    private final BBoxService bboxService;
    private final DocumentProcessingService documentProcessingService;

    public AAA0_0104Controller(FileService fileService, PageService pageService,
                              BBoxService bboxService, @Qualifier("aaa0DocumentProcessingService") DocumentProcessingService documentProcessingService) {
        this.fileService = fileService;
        this.pageService = pageService;
        this.bboxService = bboxService;
        this.documentProcessingService = documentProcessingService;
    }

    // ==========================================
    // FILE MANAGEMENT APIs
    // ==========================================

    @GetMapping("/files")
    @Operation(summary = "Get all files")
    public ResponseEntity<List<FileDTO>> getAllFiles() {
        return ResponseEntity.ok(fileService.getAllFiles());
    }

    @GetMapping("/files/status/{status}")
    @Operation(summary = "Get files by status")
    public ResponseEntity<List<FileDTO>> getFilesByStatus(@PathVariable String status) {
        return ResponseEntity.ok(fileService.getFilesByStatus(status));
    }

    @GetMapping("/files/{id}")
    @Operation(summary = "Get file by ID")
    public ResponseEntity<FileDTO> getFileById(@PathVariable int id) {
        return fileService.getFileById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/files/{id}")
    @Operation(summary = "Delete file by ID")
    public ResponseEntity<Map<String, Object>> deleteFile(@PathVariable int id) {
        boolean deleted = fileService.deleteFile(id);
        if (deleted) {
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "File deleted successfully",
                    "fileId", id
            ));
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    @PostMapping("/files/delete-batch")
    @Operation(summary = "Delete multiple files")
    public ResponseEntity<Map<String, Object>> deleteFiles(@RequestBody List<Integer> ids) {
        int deleted = fileService.deleteFiles(ids);
        return ResponseEntity.ok(Map.of(
                "success", true,
                "deleted", deleted,
                "total", ids.size()
        ));
    }

    // ==========================================
    // PAGE MANAGEMENT APIs
    // ==========================================

    @GetMapping("/files/{fileId}/pages")
    @Operation(summary = "Get all pages of a file")
    public ResponseEntity<List<PageDTO>> getPagesByFileId(@PathVariable int fileId) {
        return ResponseEntity.ok(pageService.getPagesByFileId(fileId));
    }

    @GetMapping("/files/{fileId}/pages/{pageNumber}")
    @Operation(summary = "Get specific page of a file")
    public ResponseEntity<PageDTO> getPageByFileIdAndPageNumber(
            @PathVariable int fileId,
            @PathVariable int pageNumber) {
        return pageService.getPageByFileIdAndPageNumber(fileId, pageNumber)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/pages/{id}")
    @Operation(summary = "Get page by ID")
    public ResponseEntity<PageDTO> getPageById(@PathVariable int id) {
        return pageService.getPageById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    // ==========================================
    // BBOX MANAGEMENT APIs
    // ==========================================

    @GetMapping("/pages/{pageId}/bboxes")
    @Operation(summary = "Get all bboxes of a page")
    public ResponseEntity<List<BBoxDTO>> getBBoxesByPageId(@PathVariable int pageId) {
        return ResponseEntity.ok(bboxService.getBBoxesByPageId(pageId));
    }

    @GetMapping("/files/{fileId}/bboxes")
    @Operation(summary = "Get all bboxes of a file")
    public ResponseEntity<List<BBoxDTO>> getBBoxesByFileId(@PathVariable int fileId) {
        return ResponseEntity.ok(bboxService.getBBoxesByFileId(fileId));
    }

    @GetMapping("/bboxes/class/{class}")
    @Operation(summary = "Get bboxes by class type")
    public ResponseEntity<List<BBoxDTO>> getBBoxesByClass(@PathVariable("class") String clazz) {
        return ResponseEntity.ok(bboxService.getBBoxesByClass(clazz));
    }

    @GetMapping("/bboxes/confidence/{confidence}")
    @Operation(summary = "Get bboxes with confidence greater than threshold")
    public ResponseEntity<List<BBoxDTO>> getBBoxesByConfidence(@PathVariable double confidence) {
        return ResponseEntity.ok(bboxService.getBBoxesByConfidenceGreaterThan(confidence));
    }

    @GetMapping("/bboxes/{id}")
    @Operation(summary = "Get bbox by ID")
    public ResponseEntity<BBoxDTO> getBBoxById(@PathVariable int id) {
        return bboxService.getBBoxById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    // ==========================================
    // STATISTICS APIs
    // ==========================================

    @GetMapping("/stats")
    @Operation(summary = "Get database statistics")
    public ResponseEntity<Map<String, Object>> getStatistics() {
        Map<String, Object> stats = new java.util.HashMap<>();
        stats.putAll(fileService.getStatistics());
        stats.putAll(pageService.getStatistics());
        stats.putAll(bboxService.getStatistics());
        return ResponseEntity.ok(stats);
    }

    @GetMapping("/statistics/files")
    @Operation(summary = "Get file statistics")
    public ResponseEntity<Map<String, Object>> getFileStatistics() {
        return ResponseEntity.ok(fileService.getStatistics());
    }

    @GetMapping("/statistics/pages")
    @Operation(summary = "Get page statistics")
    public ResponseEntity<Map<String, Object>> getPageStatistics() {
        return ResponseEntity.ok(pageService.getStatistics());
    }

    @GetMapping("/statistics/bboxes")
    @Operation(summary = "Get bbox statistics")
    public ResponseEntity<Map<String, Object>> getBBoxStatistics() {
        return ResponseEntity.ok(bboxService.getStatistics());
    }

    // ==========================================
    // UTILITY APIs
    // ==========================================

    @GetMapping("/health")
    @Operation(summary = "Health check")
    public ResponseEntity<Map<String, Object>> healthCheck() {
        return ResponseEntity.ok(Map.of(
                "status", "healthy",
                "service", "AAA0_0104 Document Processing",
                "database", "SQLite"
        ));
    }

    // ==========================================
    // DOCUMENT UPLOAD AND PROCESSING APIs
    // ==========================================

    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Upload and process document (PDF/TIFF/Image)")
    public ResponseEntity<SseEmitter> uploadDocument(@RequestParam("file") MultipartFile file) {
        SseEmitter emitter = new SseEmitter(300000L); // 5 minutes timeout

        // Configure emitter for async processing
        emitter.onCompletion(() -> System.out.println("SSE completed"));
        emitter.onTimeout(() -> {
            System.out.println("SSE timeout");
            emitter.complete();
        });
        emitter.onError((throwable) -> {
            System.out.println("SSE error: " + throwable.getMessage());
            emitter.completeWithError(throwable);
        });

        // Start async document processing
        CompletableFuture<DocumentProcessingService.DocumentProcessingResult> future =
            documentProcessingService.processDocument(file);

        // Send completion event
        future.thenAccept(result -> {
            sendSseEvent(emitter, "complete", Map.of(
                "requestId", result.getRequestId(),
                "pageCount", result.getPageCount(),
                "status", result.getStatus(),
                "message", "Document processing completed successfully"
            ));
            emitter.complete();
        }).exceptionally(throwable -> {
            sendSseEvent(emitter, "error", Map.of(
                "error", "Document processing failed: " + throwable.getMessage()
            ));
            emitter.complete();
            return null;
        });

        return ResponseEntity.ok()
                .header("Content-Type", "text/event-stream")
                .header("Cache-Control", "no-cache")
                .body(emitter);
    }

    /**
     * Send SSE event helper
     */
    private void sendSseEvent(SseEmitter emitter, String eventType, Object data) {
        try {
            emitter.send(SseEmitter.event().name(eventType).data(data));
        } catch (Exception e) {
            System.err.println("Failed to send SSE event: " + e.getMessage());
        }
    }
}
