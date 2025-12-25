package com.deepapp.vn.io.storage.service;

import com.deepapp.vn.io.storage.dto.PageDTO;
import com.deepapp.vn.io.storage.entity.DocumentEntity;
import com.deepapp.vn.io.storage.entity.PageEntity;
import com.deepapp.vn.io.storage.repository.DocumentRepository;
import com.deepapp.vn.io.storage.repository.PageRepository;
import com.deepapp.vn.io.storage.repository.TaskRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.File;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class DocumentManagementService {
    
    private static final Logger logger = LoggerFactory.getLogger(DocumentManagementService.class);
    
    private final DocumentRepository documentRepository;
    private final PageRepository pageRepository;
    private final TaskRepository taskRepository;
    
    @Value("${document.cleanup.enabled:true}")
    private boolean cleanupEnabled;
    
    @Value("${document.cleanup.retention-days:7}")
    private int retentionDays;
    
    @Value("${document.cleanup.cron:0 0 2 * * ?}")  // 2 AM daily
    private String cleanupCron;
    
    @Value("${document.storage.path:/tmp/deepapp/uploads}")
    private String storagePath;
    
    public DocumentManagementService(DocumentRepository documentRepository,
                                    PageRepository pageRepository,
                                    TaskRepository taskRepository) {
        this.documentRepository = documentRepository;
        this.pageRepository = pageRepository;
        this.taskRepository = taskRepository;
    }
    
    // ============================================================
    // DOCUMENT MANAGEMENT
    // ============================================================
    
    /**
     * Get all documents
     */
    public List<DocumentEntity> getAllDocuments() {
        return documentRepository.findAll();
    }
    
    /**
     * Get documents by status
     */
    public List<DocumentEntity> getDocumentsByStatus(String status) {
        return documentRepository.findByStatus(status);
    }
    
    /**
     * Get document by request ID
     */
    public Optional<DocumentEntity> getDocument(String requestId) {
        return documentRepository.findByRequestId(requestId);
    }
    
    /**
     * Get pages for a document
     */
    public List<PageDTO> getPages(String requestId) {
        List<PageEntity> pages = pageRepository.findByRequestIdOrderByPageNumberAsc(requestId);
        return pages.stream()
                .map(PageDTO::fromEntity)
                .collect(Collectors.toList());
    }
    
    /**
     * Get a specific page
     */
    public Optional<PageDTO> getPage(String requestId, int pageNumber) {
        return pageRepository.findByRequestIdAndPageNumber(requestId, pageNumber)
                .map(PageDTO::fromEntity);
    }
    
    /**
     * Delete document and all related data
     */
    @Transactional
    public boolean deleteDocument(String requestId) {
        try {
            Optional<DocumentEntity> docOpt = documentRepository.findByRequestId(requestId);
            if (docOpt.isEmpty()) {
                return false;
            }
            
            DocumentEntity doc = docOpt.get();
            
            // Delete physical file if exists
            if (doc.getFilePath() != null && !doc.getFilePath().isEmpty()) {
                File file = new File(doc.getFilePath());
                if (file.exists()) {
                    boolean deleted = file.delete();
                    logger.info("Deleted physical file: {} (success: {})", doc.getFilePath(), deleted);
                }
            }
            
            // Delete from database (cascade will delete pages and tasks)
            documentRepository.delete(doc);
            
            logger.info("Deleted document: {} ({})", requestId, doc.getFilename());
            return true;
            
        } catch (Exception e) {
            logger.error("Failed to delete document: {}", requestId, e);
            return false;
        }
    }
    
    /**
     * Delete multiple documents
     */
    @Transactional
    public int deleteDocuments(List<String> requestIds) {
        int deleted = 0;
        for (String requestId : requestIds) {
            if (deleteDocument(requestId)) {
                deleted++;
            }
        }
        return deleted;
    }
    
    /**
     * Clear all data
     */
    @Transactional
    public void clearAllData() {
        logger.warn("Clearing ALL document data!");
        pageRepository.deleteAll();
        taskRepository.deleteAll();
        documentRepository.deleteAll();
        logger.info("All document data cleared");
    }
    
    // ============================================================
    // STATISTICS
    // ============================================================
    
    /**
     * Get database statistics
     */
    public Map<String, Object> getStatistics() {
        Map<String, Object> stats = new HashMap<>();
        
        stats.put("total_documents", documentRepository.count());
        stats.put("total_pages", pageRepository.count());
        stats.put("total_tasks", taskRepository.count());
        
        stats.put("documents_processing", documentRepository.countByStatus("processing"));
        stats.put("documents_completed", documentRepository.countByStatus("completed"));
        stats.put("documents_failed", documentRepository.countByStatus("failed"));
        
        stats.put("tasks_running", taskRepository.countByStatus("running"));
        stats.put("tasks_completed", taskRepository.countByStatus("completed"));
        stats.put("tasks_failed", taskRepository.countByStatus("failed"));
        
        return stats;
    }
    
    /**
     * Get detailed statistics with status breakdown
     */
    public Map<String, Object> getDetailedStatistics() {
        Map<String, Object> stats = getStatistics();
        
        // Add status breakdown
        List<Object[]> statusStats = documentRepository.getStatusStatistics();
        Map<String, Long> statusMap = new HashMap<>();
        for (Object[] row : statusStats) {
            statusMap.put((String) row[0], (Long) row[1]);
        }
        stats.put("status_breakdown", statusMap);
        
        // Calculate storage size
        long totalSize = documentRepository.findAll().stream()
                .mapToLong(doc -> doc.getFileSize() != null ? doc.getFileSize() : 0)
                .sum();
        stats.put("total_storage_bytes", totalSize);
        stats.put("total_storage_mb", totalSize / (1024.0 * 1024.0));
        
        return stats;
    }
    
    // ============================================================
    // SCHEDULED CLEANUP
    // ============================================================
    
    /**
     * Scheduled cleanup of old documents
     * Runs daily at 2 AM by default (configurable via cron expression)
     */
    @Scheduled(cron = "${document.cleanup.cron:0 0 2 * * ?}")
    @Transactional
    public void scheduledCleanup() {
        if (!cleanupEnabled) {
            logger.debug("Scheduled cleanup is disabled");
            return;
        }
        
        logger.info("Starting scheduled document cleanup (retention: {} days)", retentionDays);
        
        try {
            int deleted = cleanupOldDocuments(retentionDays);
            logger.info("Scheduled cleanup completed: {} documents deleted", deleted);
        } catch (Exception e) {
            logger.error("Scheduled cleanup failed", e);
        }
    }
    
    /**
     * Cleanup documents older than specified days
     */
    @Transactional
    public int cleanupOldDocuments(int daysOld) {
        LocalDateTime cutoffDate = LocalDateTime.now().minusDays(daysOld);
        logger.info("Cleaning up documents created before: {}", cutoffDate);
        
        List<DocumentEntity> oldDocs = documentRepository.findOldDocuments(cutoffDate);
        logger.info("Found {} old documents to clean up", oldDocs.size());
        
        int deleted = 0;
        for (DocumentEntity doc : oldDocs) {
            try {
                // Delete physical file
                if (doc.getFilePath() != null && !doc.getFilePath().isEmpty()) {
                    File file = new File(doc.getFilePath());
                    if (file.exists()) {
                        file.delete();
                    }
                }
                
                // Delete from database
                documentRepository.delete(doc);
                deleted++;
                
            } catch (Exception e) {
                logger.error("Failed to delete old document: {}", doc.getRequestId(), e);
            }
        }
        
        logger.info("Cleanup completed: {}/{} documents deleted", deleted, oldDocs.size());
        return deleted;
    }
    
    /**
     * Cleanup by retention period (minutes, hours, days, months)
     */
    @Transactional
    public int cleanupByRetention(int amount, String unit) {
        LocalDateTime cutoffDate = switch (unit.toLowerCase()) {
            case "minutes", "minute" -> LocalDateTime.now().minusMinutes(amount);
            case "hours", "hour" -> LocalDateTime.now().minusHours(amount);
            case "days", "day" -> LocalDateTime.now().minusDays(amount);
            case "months", "month" -> LocalDateTime.now().minusMonths(amount);
            default -> throw new IllegalArgumentException("Invalid time unit: " + unit);
        };
        
        logger.info("Cleaning up documents older than {} {} (cutoff: {})", amount, unit, cutoffDate);
        return cleanupOldDocuments((int) java.time.Duration.between(cutoffDate, LocalDateTime.now()).toDays());
    }
    
    /**
     * Cleanup failed documents
     */
    @Transactional
    public int cleanupFailedDocuments() {
        List<DocumentEntity> failedDocs = documentRepository.findByStatus("failed");
        logger.info("Cleaning up {} failed documents", failedDocs.size());
        
        int deleted = 0;
        for (DocumentEntity doc : failedDocs) {
            if (deleteDocument(doc.getRequestId())) {
                deleted++;
            }
        }
        
        return deleted;
    }
}
