package com.deepapp.vn.io.AA.A0.AAA0_0104.service;

import com.deepapp.vn.io.AA.A0.AAA0_0104.dto.FileDTO;
import com.deepapp.vn.io.AA.A0.AAA0_0104.entity.BBoxEntity;
import com.deepapp.vn.io.AA.A0.AAA0_0104.entity.FileEntity;
import com.deepapp.vn.io.AA.A0.AAA0_0104.entity.PageEntity;
import com.deepapp.vn.io.AA.A0.AAA0_0104.repository.FileRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.UUID;

/**
 * Service chính cho document processing workflow
 * Tích hợp upload, conversion, OCR và database storage
 */
@Service("aaa0DocumentProcessingService")
public class DocumentProcessingService {

    private static final Logger logger = LoggerFactory.getLogger(DocumentProcessingService.class);

    private final FileRepository fileRepository;
    private final FileService fileService;
    private final PageService pageService;
    private final BBoxService bboxService;

    // Storage directory for uploaded files
    private static final String UPLOAD_DIR = "uploads/aaa0_0104/";

    public DocumentProcessingService(FileRepository fileRepository,
                                   FileService fileService,
                                   PageService pageService,
                                   BBoxService bboxService) {
        this.fileRepository = fileRepository;
        this.fileService = fileService;
        this.pageService = pageService;
        this.bboxService = bboxService;

        // Ensure upload directory exists
        try {
            Path uploadPath = Paths.get(UPLOAD_DIR);
            if (!Files.exists(uploadPath)) {
                Files.createDirectories(uploadPath);
            }
        } catch (IOException e) {
            logger.error("Failed to create upload directory", e);
        }
    }

    /**
     * Xử lý upload và processing document hoàn chỉnh
     */
    @Transactional
    public CompletableFuture<DocumentProcessingResult> processDocument(MultipartFile file) {
        String requestId = UUID.randomUUID().toString();

        return CompletableFuture.supplyAsync(() -> {
            try {
                logger.info("Starting document processing for request: {}", requestId);

                // 1. Validate file
                validateFile(file);

                // 2. Save file to storage
                FileStorageResult fileStorage = saveFile(file, requestId);

                // 3. Create file record in SQLite
                FileEntity fileEntity = new FileEntity(
                    fileStorage.getOriginalFilename(),
                    fileStorage.getFilePath(),
                    fileStorage.getFileSize(),
                    fileStorage.getMimeType()
                );
                fileEntity.setStatus("processing");
                fileEntity.setRequestId(requestId); // Add requestId for tracking
                FileEntity savedFile = fileRepository.insert(fileEntity);

                // 4. Get document metadata (page count)
                int pageCount = getPageCount(fileStorage.getFilePath(), fileStorage.getMimeType());

                // 5. Update file with page count
                savedFile.setPageCount(pageCount);
                fileRepository.update(savedFile);

                // 6. Process pages
                processDocumentPages(requestId, savedFile.getId(), fileStorage, pageCount);

                // 7. Mark as completed
                fileRepository.updateStatus(savedFile.getId(), "completed");

                logger.info("Document processing completed for request: {}", requestId);
                return new DocumentProcessingResult(requestId, pageCount, "completed");

            } catch (Exception e) {
                logger.error("Document processing failed for request: {}", requestId, e);

                // Update status to failed
                try {
                    fileService.updateFileStatus(getFileIdByRequestId(requestId), "failed");
                } catch (Exception ex) {
                    logger.error("Failed to update file status to failed", ex);
                }

                throw new RuntimeException("Document processing failed: " + e.getMessage(), e);
            }
        });
    }

    /**
     * Xử lý pages và tạo records trong database
     */
    private void processDocumentPages(String requestId, int fileId, FileStorageResult fileStorage, int totalPages) throws Exception {

        for (int pageNum = 1; pageNum <= totalPages; pageNum++) {
            try {
                // For now, create a simple page record
                // In real implementation, this would call C++ worker to extract page image
                String pageImagePath = generatePageImagePath(fileId, pageNum);

                // Create page entity
                PageEntity pageEntity = new PageEntity(fileId, pageNum, pageImagePath, 0, 0); // width/height will be updated later
                pageService.createPage(pageEntity);

                // TODO: OCR processing và tạo bbox records
                // processOCRForPage(pageEntity.getId(), pageImagePath);

                logger.info("Processed page {}/{} for file ID: {}", pageNum, totalPages, fileId);

            } catch (Exception e) {
                logger.error("Failed to process page {} for request {}: {}", pageNum, requestId, e.getMessage());
                // Continue with other pages
            }
        }
    }

    /**
     * Generate page image path
     */
    private String generatePageImagePath(int fileId, int pageNum) {
        return String.format("%s/pages/file_%d_page_%d.png", UPLOAD_DIR, fileId, pageNum);
    }

    /**
     * Xử lý OCR cho một page (TODO: implement)
     */
    private void processOCRForPage(int pageId, String imagePath) {
        try {
            // TODO: Implement OCR processing
            // 1. Load image từ imagePath
            // 2. Run YOLO detection
            // 3. Run VietOCR trên detected regions
            // 4. Create bbox entities

            logger.info("OCR processing for page {}: {}", pageId, imagePath);

            // Temporary: Create sample bbox for testing
            String coordinatesJson = "{\"x\":100,\"y\":200,\"width\":300,\"height\":400}";
            BBoxEntity sampleBBox = new BBoxEntity(
                pageId, coordinatesJson, "text", 0.95
            );
            bboxService.createBBox(sampleBBox);

        } catch (Exception e) {
            logger.error("OCR processing failed for page {}: {}", pageId, e.getMessage());
        }
    }

    /**
     * Validate uploaded file
     */
    private void validateFile(MultipartFile file) {
        if (file.isEmpty()) {
            throw new IllegalArgumentException("File is empty");
        }

        String contentType = file.getContentType();
        if (contentType == null ||
            (!contentType.equals("application/pdf") &&
             !contentType.startsWith("image/"))) {
            throw new IllegalArgumentException("Only PDF and image files are supported");
        }

        // Check file size (max 50MB)
        if (file.getSize() > 50 * 1024 * 1024) {
            throw new IllegalArgumentException("File size must be less than 50MB");
        }
    }

    /**
     * Save file to storage
     */
    private FileStorageResult saveFile(MultipartFile file, String requestId) throws IOException {
        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null) {
            originalFilename = "uploaded_file";
        }

        // Generate unique filename
        String extension = getFileExtension(originalFilename);
        String uniqueFilename = requestId + "_" + System.currentTimeMillis() + extension;
        Path filePath = Paths.get(UPLOAD_DIR, uniqueFilename);

        // Ensure directory exists
        Files.createDirectories(filePath.getParent());

        // Save file
        Files.copy(file.getInputStream(), filePath);

        return new FileStorageResult(originalFilename, filePath.toString(), file.getSize(), file.getContentType());
    }

    /**
     * Get page count from file
     */
    private int getPageCount(String filePath, String mimeType) {
        try {
            if ("application/pdf".equals(mimeType)) {
                // For PDF files, use a simple estimation or external library
                // For now, return 1 as placeholder
                return 1;
            } else if (mimeType.startsWith("image/")) {
                // Images have 1 page
                return 1;
            }
        } catch (Exception e) {
            logger.warn("Failed to get page count for file: {}", filePath, e);
        }
        return 1; // Default
    }

    /**
     * Get file extension
     */
    private String getFileExtension(String filename) {
        int lastDotIndex = filename.lastIndexOf('.');
        return lastDotIndex > 0 ? filename.substring(lastDotIndex) : "";
    }

    /**
     * Get file ID by request ID
     */
    private int getFileIdByRequestId(String requestId) {
        return fileService.getFileByRequestId(requestId)
                .map(FileDTO::getId)
                .orElseThrow(() -> new RuntimeException("File not found for request ID: " + requestId));
    }

    /**
     * Result class cho document processing
     */
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

    /**
     * File storage result
     */
    private static class FileStorageResult {
        private final String originalFilename;
        private final String filePath;
        private final long fileSize;
        private final String mimeType;

        public FileStorageResult(String originalFilename, String filePath, long fileSize, String mimeType) {
            this.originalFilename = originalFilename;
            this.filePath = filePath;
            this.fileSize = fileSize;
            this.mimeType = mimeType;
        }

        public String getOriginalFilename() { return originalFilename; }
        public String getFilePath() { return filePath; }
        public long getFileSize() { return fileSize; }
        public String getMimeType() { return mimeType; }
    }
}