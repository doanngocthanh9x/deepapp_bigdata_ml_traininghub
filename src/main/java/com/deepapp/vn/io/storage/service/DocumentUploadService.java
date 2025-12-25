package com.deepapp.vn.io.storage.service;

import com.deepapp.vn.io.storage.entity.DocumentEntity;
import com.deepapp.vn.io.storage.entity.PageEntity;
import com.deepapp.vn.io.storage.entity.TaskEntity;
import com.deepapp.vn.io.storage.repository.DocumentRepository;
import com.deepapp.vn.io.storage.repository.PageRepository;
import com.deepapp.vn.io.storage.repository.TaskRepository;
import com.deepapp.vn.io.ZZ.A0.ZZA0_0100.service.DocumentProcessingService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.CompletableFuture;

/**
 * Document Upload Service - Centralizes all document upload and processing logic
 * Controls document storage, validation, and processing through storage layer
 */
@Service
public class DocumentUploadService {

    private static final Logger logger = LoggerFactory.getLogger(DocumentUploadService.class);

    private final DocumentRepository documentRepository;
    private final PageRepository pageRepository;
    private final TaskRepository taskRepository;
    private final DocumentManagementService documentManagementService;
    private final DocumentProcessingService documentProcessingService;

    @Value("${document.storage.path:/tmp/deepapp/uploads}")
    private String storagePath;

    @Value("${document.temp.path:/tmp/deepapp/temp}")
    private String tempPath;

    public DocumentUploadService(DocumentRepository documentRepository,
                                PageRepository pageRepository,
                                TaskRepository taskRepository,
                                DocumentManagementService documentManagementService,
                                DocumentProcessingService documentProcessingService) {
        this.documentRepository = documentRepository;
        this.pageRepository = pageRepository;
        this.taskRepository = taskRepository;
        this.documentManagementService = documentManagementService;
        this.documentProcessingService = documentProcessingService;
    }

    /**
     * Validate uploaded file
     */
    public ValidationResult validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            return ValidationResult.failure("File is empty");
        }

        String filename = file.getOriginalFilename();
        if (filename == null || filename.trim().isEmpty()) {
            return ValidationResult.failure("Invalid filename");
        }

        String fileExtension = getFileExtension(filename).toLowerCase();
        if (!isValidFileType(fileExtension)) {
            return ValidationResult.failure("Unsupported file format. Only TIFF, TIF, PDF are supported");
        }

        // Check file size (max 100MB)
        long maxSize = 100 * 1024 * 1024; // 100MB
        if (file.getSize() > maxSize) {
            return ValidationResult.failure("File size exceeds maximum limit of 100MB");
        }

        return ValidationResult.success(filename, fileExtension, file.getSize());
    }

    /**
     * Save uploaded file to storage
     */
    public FileStorageResult saveFile(MultipartFile file, String requestId) throws IOException {
        String filename = file.getOriginalFilename();
        String fileExtension = getFileExtension(filename);

        // Create directory structure: /tmp/deepapp/uploads/{requestId}/
        Path uploadDir = Paths.get(storagePath, requestId);
        Files.createDirectories(uploadDir);

        // Save file
        String storedFilename = "document." + fileExtension;
        Path filePath = uploadDir.resolve(storedFilename);
        Files.write(filePath, file.getBytes());

        return new FileStorageResult(
            filePath.toString(),
            filename,
            fileExtension,
            file.getSize(),
            uploadDir.toString()
        );
    }

    /**
     * Create document record in database
     */
    @Transactional
    public DocumentEntity createDocumentRecord(String requestId, FileStorageResult fileStorage,
                                             String status, Integer pageCount) {
        DocumentEntity document = new DocumentEntity();
        document.setRequestId(requestId);
        document.setFilename(fileStorage.getOriginalFilename());
        document.setFilePath(fileStorage.getFilePath());
        document.setFormat(fileStorage.getFormat());
        document.setFileSize(fileStorage.getFileSize());
        document.setStatus(status);
        document.setPageCount(pageCount);
        document.setCreatedAt(LocalDateTime.now());
        document.setUpdatedAt(LocalDateTime.now());

        return documentRepository.save(document);
    }

    /**
     * Create task record for processing
     */
    @Transactional
    public TaskEntity createTaskRecord(String requestId, String taskType, Integer totalPages) {
        // First get the document
        Optional<DocumentEntity> documentOpt = documentRepository.findByRequestId(requestId);
        if (documentOpt.isEmpty()) {
            throw new IllegalArgumentException("Document not found for requestId: " + requestId);
        }

        TaskEntity task = new TaskEntity();
        task.setDocument(documentOpt.get());
        task.setTaskType(taskType);
        task.setStatus("processing");
        task.setTotalPages(totalPages);
        task.setProcessedPages(0);
        task.setStartedAt(LocalDateTime.now());

        return taskRepository.save(task);
    }

    /**
     * Update document status
     */
    @Transactional
    public void updateDocumentStatus(String requestId, String status, String errorMessage) {
        Optional<DocumentEntity> documentOpt = documentRepository.findByRequestId(requestId);
        if (documentOpt.isPresent()) {
            DocumentEntity document = documentOpt.get();
            document.setStatus(status);
            document.setErrorMessage(errorMessage);
            document.setUpdatedAt(LocalDateTime.now());
            documentRepository.save(document);
        }
    }

    /**
     * Update task progress
     */
    @Transactional
    public void updateTaskProgress(String requestId, int processedPages, String status) {
        // Find document first, then find associated task
        Optional<DocumentEntity> documentOpt = documentRepository.findByRequestId(requestId);
        if (documentOpt.isPresent()) {
            // Find task by document
            Optional<TaskEntity> taskOpt = taskRepository.findAll().stream()
                .filter(task -> task.getDocument() != null &&
                               task.getDocument().getRequestId().equals(requestId))
                .findFirst();

            if (taskOpt.isPresent()) {
                TaskEntity task = taskOpt.get();
                task.setProcessedPages(processedPages);
                task.setStatus(status);
                if ("completed".equals(status) || "failed".equals(status)) {
                    task.setCompletedAt(LocalDateTime.now());
                }
                taskRepository.save(task);
            }
        }
    }

    /**
     * Save page data
     */
    @Transactional
    public void savePageData(String requestId, int pageNumber, Map<String, Object> pageData) {
        Optional<DocumentEntity> documentOpt = documentRepository.findByRequestId(requestId);
        if (documentOpt.isEmpty()) {
            logger.warn("Document not found for requestId: {}", requestId);
            return;
        }

        DocumentEntity document = documentOpt.get();

        PageEntity page = new PageEntity();
        page.setDocument(document);
        page.setRequestId(requestId);
        page.setPageNumber(pageNumber);
        page.setWidth(((Number) pageData.getOrDefault("width", 0)).intValue());
        page.setHeight(((Number) pageData.getOrDefault("height", 0)).intValue());
        page.setDpi(((Number) pageData.getOrDefault("dpi", 150)).intValue());
        page.setFormat((String) pageData.getOrDefault("format", "PNG"));
        page.setImagePath((String) pageData.getOrDefault("imagePath", ""));
        page.setImageData((String) pageData.getOrDefault("imageData", null));
        page.setText((String) pageData.getOrDefault("text", ""));
        page.setStatus((String) pageData.getOrDefault("status", "completed"));
        page.setCreatedAt(LocalDateTime.now());

        pageRepository.save(page);
    }

    /**
     * Process document upload with full control through storage layer
     */
    public CompletableFuture<DocumentProcessingResult> processDocumentUpload(
            MultipartFile file, SseEmitter emitter, String requestId) {

        return CompletableFuture.supplyAsync(() -> {
            try {
                // 1. Validate file
                ValidationResult validation = validateFile(file);
                if (!validation.isValid()) {
                    throw new IllegalArgumentException(validation.getErrorMessage());
                }

                // 2. Save file to storage
                FileStorageResult fileStorage = saveFile(file, requestId);

                // 3. Send initial status via SSE
                sendSseEvent(emitter, "status", Map.of(
                    "message", "File uploaded successfully, starting processing...",
                    "requestId", requestId,
                    "filename", fileStorage.getOriginalFilename(),
                    "fileSize", fileStorage.getFileSize()
                ));

                // 4. Create document record
                DocumentEntity document = createDocumentRecord(requestId, fileStorage, "processing", null);

                // 5. Get document metadata (page count, etc.) - Read from disk to ensure data integrity
                byte[] fileData = Files.readAllBytes(Paths.get(fileStorage.getFilePath()));
                Map<String, Object> metadata = getDocumentMetadata(fileData, fileStorage.getOriginalFilename());
                Integer pageCount = ((Number) metadata.getOrDefault("pageCount", 0)).intValue();

                // 6. Update document with page count
                document.setPageCount(pageCount);
                documentRepository.save(document);

                // 7. Create task record
                createTaskRecord(requestId, "document_processing", pageCount);

                // 8. Send metadata via SSE
                sendSseEvent(emitter, "metadata", Map.of(
                    "requestId", requestId,
                    "filename", fileStorage.getOriginalFilename(),
                    "format", fileStorage.getFormat(),
                    "pageCount", pageCount,
                    "fileSize", fileStorage.getFileSize()
                ));

                // 9. Process pages via C++ worker - Use fileData from disk
                processDocumentPages(requestId, fileData, fileStorage, emitter, pageCount);

                // 10. Mark as completed
                updateDocumentStatus(requestId, "completed", null);
                updateTaskProgress(requestId, pageCount, "completed");

                sendSseEvent(emitter, "complete", Map.of(
                    "requestId", requestId,
                    "totalPages", pageCount,
                    "message", "Document processing completed successfully"
                ));

                return new DocumentProcessingResult(requestId, pageCount, "completed");

            } catch (Exception e) {
                logger.error("Error processing document upload: {}", e.getMessage(), e);

                // Update status to failed
                updateDocumentStatus(requestId, "failed", e.getMessage());
                updateTaskProgress(requestId, 0, "failed");

                sendSseEvent(emitter, "error", Map.of(
                    "requestId", requestId,
                    "error", e.getMessage()
                ));

                throw new RuntimeException("Document processing failed: " + e.getMessage(), e);
            }
        });
    }

    /**
     * Get document metadata (page count, format, etc.)
     */
    private Map<String, Object> getDocumentMetadata(byte[] fileData, String filename) {
        try {
            // Call real processing service to get metadata via C++ worker
            return documentProcessingService.getDocumentInfo(fileData, filename);
        } catch (Exception e) {
            logger.error("Failed to get document metadata from C++ worker: {}", e.getMessage(), e);
            // Fallback to mock data if C++ worker fails
            Map<String, Object> metadata = new HashMap<>();
            metadata.put("pageCount", 0);
            metadata.put("format", getFileExtension(filename));
            metadata.put("dpi", 150);
            metadata.put("error", "Failed to get metadata: " + e.getMessage());
            return metadata;
        }
    }

    /**
     * Process document pages (handled by C++ worker via DocumentProcessingService)
     */
    private void processDocumentPages(String requestId, byte[] fileData,
                                    FileStorageResult fileStorage, SseEmitter emitter, int totalPages) throws IOException {
        logger.info("Processing {} pages for requestId: {}", totalPages, requestId);

        for (int i = 1; i <= totalPages; i++) {
            try {
                // Get page data from C++ worker via DocumentProcessingService
                Map<String, Object> pageResponse = documentProcessingService.getSpecificPage(fileData, fileStorage.getOriginalFilename(), i);

                // Extract page data from response - C++ worker response is nested in pageData
                @SuppressWarnings("unchecked")
                Map<String, Object> pageData = (Map<String, Object>) pageResponse.get("pageData");
                if (pageData == null) {
                    pageData = new HashMap<>(pageResponse);
                }

                // Remove worker metadata, keep only page data
                pageData.remove("worker");
                pageData.remove("status");
                pageData.remove("filename");
                pageData.remove("pageNumber");
                pageData.remove("timestamp");

                // Use the actual file path from C++ worker instead of constructing our own
                // C++ worker saves to /tmp/deepapp/pages/{filename}/page_{i}.png
                String actualImagePath = (String) pageData.get("filePath");
                if (actualImagePath != null && !actualImagePath.isEmpty()) {
                    pageData.put("imagePath", actualImagePath);
                } else {
                    // Fallback to our constructed path
                    pageData.put("imagePath", fileStorage.getDirectory() + "/page_" + i + ".png");
                }

                pageData.put("status", "completed");

                // Save page data to database
                savePageData(requestId, i, pageData);

                // Send page data via SSE with actual image data
                sendSseEvent(emitter, "page", Map.of(
                    "requestId", requestId,
                    "pageNumber", i,
                    "totalPages", totalPages,
                    "pageData", pageData
                ));

                // Update progress
                updateTaskProgress(requestId, i, "processing");

                logger.info("Processed page {}/{} for requestId: {}", i, totalPages, requestId);

            } catch (Exception e) {
                logger.error("Failed to process page {} for requestId {}: {}", i, requestId, e.getMessage(), e);

                // Send error event
                sendSseEvent(emitter, "error", Map.of(
                    "requestId", requestId,
                    "pageNumber", i,
                    "error", e.getMessage()
                ));

                // Continue processing other pages
            }
        }
    }

    /**
     * Send SSE event
     */
    private void sendSseEvent(SseEmitter emitter, String eventType, Object data) {
        try {
            emitter.send(SseEmitter.event().name(eventType).data(data));
        } catch (IOException e) {
            logger.warn("Failed to send SSE event: {}", e.getMessage());
        }
    }

    /**
     * Utility methods
     */
    private String getFileExtension(String filename) {
        if (filename == null) return "";
        int lastDot = filename.lastIndexOf('.');
        return lastDot > 0 ? filename.substring(lastDot + 1) : "";
    }

    private boolean isValidFileType(String extension) {
        return extension.equalsIgnoreCase("pdf") ||
               extension.equalsIgnoreCase("tiff") ||
               extension.equalsIgnoreCase("tif");
    }

    /**
     * Save file to temporary location and return file path (for path-based processing)
     */
    public String saveFileToTemp(MultipartFile file) throws IOException {
        // Validate file first
        ValidationResult validation = validateFile(file);
        if (!validation.isValid()) {
            throw new IllegalArgumentException(validation.getErrorMessage());
        }

        // Create temp directory for documents
        Path tempDir = Paths.get(tempPath, "documents");
        Files.createDirectories(tempDir);

        // Generate unique filename
        String originalFilename = file.getOriginalFilename();
        String fileExtension = getFileExtension(originalFilename);
        String uniqueFilename = UUID.randomUUID().toString() + "." + fileExtension;
        Path tempFilePath = tempDir.resolve(uniqueFilename);

        // Save file to temp location
        Files.write(tempFilePath, file.getBytes());

        logger.info("Saved file to temp location: {}", tempFilePath.toString());
        return tempFilePath.toString();
    }

    /**
     * Clean up temporary files older than specified hours
     */
    public void cleanupTempFiles(int hoursOld) {
        try {
            Path tempDir = Paths.get(tempPath);
            if (!Files.exists(tempDir)) {
                return;
            }

            long cutoffTime = System.currentTimeMillis() - (hoursOld * 60 * 60 * 1000);

            Files.walk(tempDir)
                .filter(Files::isRegularFile)
                .filter(path -> {
                    try {
                        return Files.getLastModifiedTime(path).toMillis() < cutoffTime;
                    } catch (IOException e) {
                        return false;
                    }
                })
                .forEach(path -> {
                    try {
                        Files.delete(path);
                        logger.info("Cleaned up old temp file: {}", path);
                    } catch (IOException e) {
                        logger.warn("Failed to delete temp file: {}", path, e);
                    }
                });

        } catch (IOException e) {
            logger.error("Error during temp file cleanup", e);
        }
    }

    /**
     * Get unified temp directory structure info
     */
    public Map<String, String> getTempDirectoryInfo() {
        return Map.of(
            "temp_base", tempPath,
            "temp_documents", tempPath + "/documents",
            "temp_images", tempPath + "/images",
            "storage_base", storagePath,
            "pages_output", "/tmp/deepapp/pages"
        );
    }

    /**
     * Result classes
     */
    public static class ValidationResult {
        private final boolean valid;
        private final String errorMessage;
        private final String filename;
        private final String format;
        private final long fileSize;

        private ValidationResult(boolean valid, String errorMessage, String filename, String format, long fileSize) {
            this.valid = valid;
            this.errorMessage = errorMessage;
            this.filename = filename;
            this.format = format;
            this.fileSize = fileSize;
        }

        public static ValidationResult success(String filename, String format, long fileSize) {
            return new ValidationResult(true, null, filename, format, fileSize);
        }

        public static ValidationResult failure(String errorMessage) {
            return new ValidationResult(false, errorMessage, null, null, 0);
        }

        public boolean isValid() { return valid; }
        public String getErrorMessage() { return errorMessage; }
        public String getFilename() { return filename; }
        public String getFormat() { return format; }
        public long getFileSize() { return fileSize; }
    }

    public static class FileStorageResult {
        private final String filePath;
        private final String originalFilename;
        private final String format;
        private final long fileSize;
        private final String directory;

        public FileStorageResult(String filePath, String originalFilename, String format, long fileSize, String directory) {
            this.filePath = filePath;
            this.originalFilename = originalFilename;
            this.format = format;
            this.fileSize = fileSize;
            this.directory = directory;
        }

        public String getFilePath() { return filePath; }
        public String getOriginalFilename() { return originalFilename; }
        public String getFormat() { return format; }
        public long getFileSize() { return fileSize; }
        public String getDirectory() { return directory; }
    }

    public static class DocumentProcessingResult {
        private final String requestId;
        private final int pageCount;
        private final String status;

        public DocumentProcessingResult(String requestId, int pageCount, String status) {
            this.requestId = requestId;
            this.pageCount = pageCount;
            this.status = status;
        }

        public String getRequestId() { return requestId; }
        public int getPageCount() { return pageCount; }
        public String getStatus() { return status; }
    }
}