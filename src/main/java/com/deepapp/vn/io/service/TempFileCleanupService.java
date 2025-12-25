package com.deepapp.vn.io.service;

import com.deepapp.vn.io.storage.service.DocumentUploadService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Service for managing temporary file cleanup across the application
 */
@Service
public class TempFileCleanupService {

    private static final Logger logger = LoggerFactory.getLogger(TempFileCleanupService.class);

    @Value("${document.temp.path:/tmp/deepapp/temp}")
    private String tempPath;

    @Value("${cpp.worker.images.path:/tmp/deepapp/images}")
    private String imagesTempPath;

    @Value("${cpp.worker.pages.path:/tmp/deepapp/pages}")
    private String pagesTempPath;

    @Value("${temp.cleanup.interval.hours:24}")
    private int cleanupIntervalHours;

    @Value("${temp.file.max.age.hours:48}")
    private int maxFileAgeHours;

    @Autowired
    private DocumentUploadService documentUploadService;

    @PostConstruct
    public void init() {
        logger.info("TempFileCleanupService initialized with:");
        logger.info("  - Temp path: {}", tempPath);
        logger.info("  - Images path: {}", imagesTempPath);
        logger.info("  - Pages path: {}", pagesTempPath);
        logger.info("  - Cleanup interval: {} hours", cleanupIntervalHours);
        logger.info("  - Max file age: {} hours", maxFileAgeHours);

        // Create directories on startup
        createDirectories();
    }

    /**
     * Create all necessary temp directories
     */
    private void createDirectories() {
        try {
            Files.createDirectories(Paths.get(tempPath));
            Files.createDirectories(Paths.get(tempPath, "documents"));
            Files.createDirectories(Paths.get(imagesTempPath));
            Files.createDirectories(Paths.get(pagesTempPath));
            logger.info("Created temp directories successfully");
        } catch (IOException e) {
            logger.error("Failed to create temp directories", e);
        }
    }

    /**
     * Scheduled cleanup of old temp files
     * Runs every cleanupIntervalHours
     */
    @Scheduled(fixedRateString = "#{${temp.cleanup.interval.hours:24} * 60 * 60 * 1000}")
    public void cleanupTempFiles() {
        logger.info("Starting scheduled temp file cleanup");

        try {
            // Cleanup document temp files
            documentUploadService.cleanupTempFiles(maxFileAgeHours);

            // Cleanup YOLO images
            cleanupDirectory(imagesTempPath, maxFileAgeHours);

            // Cleanup C++ pages (be more conservative with pages)
            cleanupDirectory(pagesTempPath, maxFileAgeHours * 2);

            logger.info("Temp file cleanup completed");

        } catch (Exception e) {
            logger.error("Error during temp file cleanup", e);
        }
    }

    /**
     * Cleanup files in a specific directory older than specified hours
     */
    private void cleanupDirectory(String dirPath, int hoursOld) {
        try {
            Path dir = Paths.get(dirPath);
            if (!Files.exists(dir)) {
                return;
            }

            long cutoffTime = System.currentTimeMillis() - (hoursOld * 60L * 60L * 1000L);

            Files.walk(dir)
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
                        logger.debug("Cleaned up old temp file: {}", path);
                    } catch (IOException e) {
                        logger.warn("Failed to delete temp file: {}", path, e);
                    }
                });

        } catch (IOException e) {
            logger.error("Error cleaning up directory: {}", dirPath, e);
        }
    }

    /**
     * Get temp directory usage statistics
     */
    public TempDirectoryStats getTempDirectoryStats() {
        TempDirectoryStats stats = new TempDirectoryStats();

        stats.setDocumentsTemp(getDirectoryStats(Paths.get(tempPath, "documents")));
        stats.setImagesTemp(getDirectoryStats(Paths.get(imagesTempPath)));
        stats.setPagesTemp(getDirectoryStats(Paths.get(pagesTempPath)));

        return stats;
    }

    private DirectoryStats getDirectoryStats(Path dir) {
        DirectoryStats stats = new DirectoryStats();
        stats.setPath(dir.toString());

        try {
            if (Files.exists(dir)) {
                long fileCount = 0;
                long totalSize = 0;
                
                for (Path path : Files.walk(dir).filter(Files::isRegularFile).toArray(Path[]::new)) {
                    fileCount++;
                    try {
                        totalSize += Files.size(path);
                    } catch (IOException e) {
                        // Skip files that can't be read
                    }
                }
                
                stats.setTotalSize(totalSize);
                // Set file count by incrementing
                for (int i = 0; i < fileCount; i++) {
                    stats.incrementFileCount();
                }
            }
        } catch (IOException e) {
            logger.warn("Error getting stats for directory: {}", dir, e);
        }

        return stats;
    }

    /**
     * Directory statistics
     */
    public static class DirectoryStats {
        private String path;
        private long fileCount;
        private long totalSize;

        public String getPath() { return path; }
        public void setPath(String path) { this.path = path; }

        public long getFileCount() { return fileCount; }
        public void incrementFileCount() { this.fileCount++; }

        public long getTotalSize() { return totalSize; }
        public void setTotalSize(long totalSize) { this.totalSize = totalSize; }

        public String getTotalSizeFormatted() {
            if (totalSize < 1024) return totalSize + " B";
            if (totalSize < 1024 * 1024) return (totalSize / 1024) + " KB";
            if (totalSize < 1024 * 1024 * 1024) return (totalSize / (1024 * 1024)) + " MB";
            return (totalSize / (1024 * 1024 * 1024)) + " GB";
        }
    }

    /**
     * Overall temp directory statistics
     */
    public static class TempDirectoryStats {
        private DirectoryStats documentsTemp;
        private DirectoryStats imagesTemp;
        private DirectoryStats pagesTemp;

        public DirectoryStats getDocumentsTemp() { return documentsTemp; }
        public void setDocumentsTemp(DirectoryStats documentsTemp) { this.documentsTemp = documentsTemp; }

        public DirectoryStats getImagesTemp() { return imagesTemp; }
        public void setImagesTemp(DirectoryStats imagesTemp) { this.imagesTemp = imagesTemp; }

        public DirectoryStats getPagesTemp() { return pagesTemp; }
        public void setPagesTemp(DirectoryStats pagesTemp) { this.pagesTemp = pagesTemp; }

        public long getTotalFiles() {
            return (documentsTemp != null ? documentsTemp.getFileCount() : 0) +
                   (imagesTemp != null ? imagesTemp.getFileCount() : 0) +
                   (pagesTemp != null ? pagesTemp.getFileCount() : 0);
        }

        public long getTotalSize() {
            return (documentsTemp != null ? documentsTemp.getTotalSize() : 0) +
                   (imagesTemp != null ? imagesTemp.getTotalSize() : 0) +
                   (pagesTemp != null ? pagesTemp.getTotalSize() : 0);
        }
    }
}