package com.deepapp.vn.io.service;

import com.deepapp.vn.io.storage.service.DocumentUploadService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.model.File;
import com.google.api.services.drive.model.FileList;

import jakarta.annotation.PostConstruct;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

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

    @Value("${models.path:src/main/resources/models}")
    private String modelsPath;

    @Value("${google.drive.api.key:}")
    private String apiKey;

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

        // Download models once if not present
        downloadModelsIfNeeded();
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
     * Download models from Google Drive if not already present
     */
    private void downloadModelsIfNeeded() {
        logger.info("Checking and downloading models from Google Drive...");
        try {
            downloadModels();
            logger.info("Models download check completed");
        } catch (Exception e) {
            logger.error("Error downloading models", e);
        }
    }

    /**
     * Check if directory is empty
     */
    private boolean isDirectoryEmpty(Path dir) throws IOException {
        if (!Files.isDirectory(dir)) {
            return true;
        }
        try (var stream = Files.list(dir)) {
            return stream.findFirst().isEmpty();
        }
    }

    /**
     * Download models using Google Drive API
     */
    private void downloadModels() throws IOException {
        if (apiKey == null || apiKey.isEmpty()) {
            logger.warn("Google Drive API key not configured, skipping model download");
            return;
        }

        String folderId = "1yX43t0JeR0Tie3ToWngDa7MkN5E9Rl_i";
        Drive drive = new Drive.Builder(new NetHttpTransport(), new JacksonFactory(), null)
            .setApplicationName("TempFileCleanupService")
            .build();

        try {
            HttpClient httpClient = HttpClient.newHttpClient();
            Path modelsDir = Paths.get(modelsPath);
            Files.createDirectories(modelsDir);

            downloadFromFolder(drive, httpClient, folderId, modelsDir);
        } catch (Exception e) {
            logger.error("Error downloading models", e);
            throw new IOException("Failed to download models", e);
        }
    }

    /**
     * Recursively download files from a Google Drive folder
     */
    private void downloadFromFolder(Drive drive, HttpClient httpClient, String folderId, Path currentDir) throws IOException {
        try {
            FileList result = drive.files().list()
                .setQ("'" + folderId + "' in parents")
                .setKey(apiKey)
                .execute();

            for (File file : result.getFiles()) {
                if ("application/vnd.google-apps.folder".equals(file.getMimeType())) {
                    // Create subdirectory and recurse
                    Path subDir = currentDir.resolve(file.getName());
                    Files.createDirectories(subDir);
                    logger.info("Processing folder: " + file.getName());
                    downloadFromFolder(drive, httpClient, file.getId(), subDir);
                } else {
                    // Download file only if it doesn't exist
                    Path localFile = currentDir.resolve(file.getName());
                    if (Files.exists(localFile)) {
                        logger.info("File already exists, skipping: " + file.getName());
                    } else {
                        String downloadUrl = "https://www.googleapis.com/drive/v3/files/" + file.getId() + "?alt=media&key=" + apiKey;
                        HttpRequest request = HttpRequest.newBuilder()
                            .uri(URI.create(downloadUrl))
                            .build();
                        HttpResponse<InputStream> response = httpClient.send(request, HttpResponse.BodyHandlers.ofInputStream());
                        if (response.statusCode() == 200) {
                            Files.copy(response.body(), localFile, StandardCopyOption.REPLACE_EXISTING);
                            logger.info("Downloaded model: " + file.getName());
                        } else {
                            logger.error("Failed to download file: " + file.getName() + ", status: " + response.statusCode());
                        }
                    }
                }
            }
        } catch (Exception e) {
            logger.error("Error downloading from folder: " + folderId, e);
            throw new IOException("Failed to download from folder", e);
        }
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