package com.deepapp.vn.io.ZZ.A0.ZZA0_0100.service;

import com.deepapp.vn.io.workers.CppWorkerClient;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.CompletableFuture;

/**
 * Service for processing documents (TIFF, TIF, PDF) via C++ worker
 */
@Service
public class DocumentProcessingService {

    private static final Logger logger = LoggerFactory.getLogger(DocumentProcessingService.class);
    private static final String WORKER_ID = "ZZA0_0100_W";
    private static final String UPLOAD_DIR = "/tmp/deepapp/uploads";
    private static final String OUTPUT_DIR = "/tmp/deepapp/outputs";

    @Autowired
    private CppWorkerClient cppWorkerClient;

    private final ObjectMapper objectMapper = new ObjectMapper();

    public DocumentProcessingService() {
        // Ensure upload and output directories exist
        try {
            Files.createDirectories(Paths.get(UPLOAD_DIR));
            Files.createDirectories(Paths.get(OUTPUT_DIR));
            logger.info("Initialized directories: {} and {}", UPLOAD_DIR, OUTPUT_DIR);
        } catch (IOException e) {
            logger.error("Failed to create directories", e);
        }
    }

    /**
     * Process document and extract all pages - FAST MODE
     * Only returns metadata, individual pages must be requested separately
     */
    public Map<String, Object> processDocument(byte[] fileData, String filename, String options)
            throws Exception {

        logger.info("Processing document (FAST): {} (size: {} bytes)", filename, fileData.length);

        // FAST MODE: Only get metadata, don't extract full page data
        Map<String, Object> request = new HashMap<>();
        request.put("filename", filename);
        request.put("data", ""); // Don't send data for fast response
        request.put("action", "get_info");

        String requestJson = objectMapper.writeValueAsString(request);

        // Call C++ worker for metadata only
        CompletableFuture<String> future = cppWorkerClient.callWorker(
                WORKER_ID,
                "document_info",
                requestJson);

        String responseJson = future.get();
        logger.info("Fast metadata response received in {}ms", System.currentTimeMillis());

        @SuppressWarnings("unchecked")
        Map<String, Object> response = objectMapper.readValue(responseJson, Map.class);

        // Build fast result - only metadata
        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("filename", filename);
        result.put("fileSize", fileData.length);
        result.put("pageCount", response.get("pageCount"));
        result.put("format", response.get("format"));
        result.put("timestamp", System.currentTimeMillis());
        result.put("mode", "FAST");
        result.put("message",
                "Document metadata retrieved. Use /ZZ/A0/ZZA0_0100/page?pageNumber=N to get individual pages");

        return result;
    }

    /**
     * Get a specific page from the document
     * For large files (>3MB), use filePath method to avoid message size limits
     */
    public Map<String, Object> getSpecificPage(byte[] fileData, String filename, int pageNumber)
            throws Exception {

        // For large files, save to temp and use filePath method
        if (fileData.length > 3 * 1024 * 1024) { // > 3MB
            String tempPath = saveUploadedFile(fileData, filename, "temp_page_" + System.currentTimeMillis());
            return getSpecificPageByPath(tempPath, filename, pageNumber);
        }

        logger.info("Getting page {} from document: {}", pageNumber, filename);

        // Prepare request for specific page
        Map<String, Object> request = new HashMap<>();
        request.put("filename", filename);
        request.put("data", Base64.getEncoder().encodeToString(fileData));
        request.put("action", "get_page");
        request.put("pageNumber", pageNumber);

        String requestJson = objectMapper.writeValueAsString(request);

        // Call C++ worker
        CompletableFuture<String> future = cppWorkerClient.callWorker(
                WORKER_ID,
                "get_page",
                requestJson);

        String responseJson = future.get();
        logger.info("C++ Worker page response: {}", responseJson);

        // Parse response
        @SuppressWarnings("unchecked")
        Map<String, Object> response = objectMapper.readValue(responseJson, Map.class);

        // Build result
        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("filename", filename);
        result.put("pageNumber", pageNumber);
        result.put("timestamp", System.currentTimeMillis());
        result.put("worker", WORKER_ID);
        result.put("pageData", response);

        return result;
    }

    /**
     * Get a specific page using filePath (avoids gRPC message size limits)
     */
    public Map<String, Object> getSpecificPageByPath(String filePath, String filename, int pageNumber)
            throws Exception {

        logger.info("Getting page {} from document: {} via filePath", pageNumber, filename);

        // Prepare request with filePath
        Map<String, Object> request = new HashMap<>();
        request.put("filename", filename);
        request.put("filePath", filePath);  // Send path instead of data
        request.put("action", "get_page");
        request.put("pageNumber", pageNumber);

        String requestJson = objectMapper.writeValueAsString(request);

        // Call C++ worker
        CompletableFuture<String> future = cppWorkerClient.callWorker(
                WORKER_ID,
                "get_page",
                requestJson);

        String responseJson = future.get();
        logger.info("C++ Worker page response (via filePath): {}", responseJson);

        // Parse response
        @SuppressWarnings("unchecked")
        Map<String, Object> response = objectMapper.readValue(responseJson, Map.class);

        // Build result
        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("filename", filename);
        result.put("pageNumber", pageNumber);
        result.put("timestamp", System.currentTimeMillis());
        result.put("worker", WORKER_ID);
        result.put("pageData", response);

        return result;
    }

    /**
     * Get document metadata (page count, format info, etc.) - FAST
     * Uses filePath instead of sending file data to avoid message size limits
     */
    public Map<String, Object> getDocumentInfo(byte[] fileData, String filename)
            throws Exception {
        // For large files, save to temp and use filePath method
        if (fileData.length > 3 * 1024 * 1024) { // > 3MB
            String tempPath = saveUploadedFile(fileData, filename, "temp_" + System.currentTimeMillis());
            return getDocumentInfoByPath(tempPath, filename, fileData.length);
        }

        long startTime = System.currentTimeMillis();
        logger.info("Getting metadata for document: {} (FAST mode with real parsing)", filename);

        Map<String, Object> request = new HashMap<>();
        request.put("filename", filename);
        request.put("data", Base64.getEncoder().encodeToString(fileData));
        request.put("action", "get_info");

        String requestJson = objectMapper.writeValueAsString(request);

        CompletableFuture<String> future = cppWorkerClient.callWorker(
                WORKER_ID,
                "document_info",
                requestJson);

        String responseJson = future.get();
        long duration = System.currentTimeMillis() - startTime;
        logger.info("Metadata retrieved in {}ms", duration);

        @SuppressWarnings("unchecked")
        Map<String, Object> response = objectMapper.readValue(responseJson, Map.class);

        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("filename", filename);
        result.put("fileSize", fileData.length);
        result.put("pageCount", response.get("pageCount"));
        result.put("format", response.get("format"));
        result.put("processingTime", duration + "ms");
        result.put("message", "Metadata retrieved - use POST /ZZ/A0/ZZA0_0100/page to get individual pages");

        return result;
    }

    /**
     * Get document metadata using filePath (avoids gRPC message size limits)
     */
    public Map<String, Object> getDocumentInfoByPath(String filePath, String filename, long fileSize)
            throws Exception {

        long startTime = System.currentTimeMillis();
        logger.info("Getting metadata for document: {} via filePath (no size limit)", filename);

        Map<String, Object> request = new HashMap<>();
        request.put("filename", filename);
        request.put("filePath", filePath);  // Send path instead of data
        request.put("action", "get_info");

        String requestJson = objectMapper.writeValueAsString(request);

        CompletableFuture<String> future = cppWorkerClient.callWorker(
                WORKER_ID,
                "document_info",
                requestJson);

        String responseJson = future.get();
        long duration = System.currentTimeMillis() - startTime;
        logger.info("Metadata retrieved in {}ms via filePath", duration);

        @SuppressWarnings("unchecked")
        Map<String, Object> response = objectMapper.readValue(responseJson, Map.class);

        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("filename", filename);
        result.put("fileSize", fileSize);
        result.put("pageCount", response.get("pageCount"));
        result.put("format", response.get("format"));
        result.put("processingTime", duration + "ms");
        result.put("message", "Metadata retrieved - use POST /ZZ/A0/ZZA0_0100/page to get individual pages");

        return result;
    }

    /**
     * Process document with streaming from C++ → Java via gRPC
     * C++ worker will send events back: metadata, page1, page2, ..., complete
     * 
     * NEW APPROACH: Save file to disk, send path to C++ worker
     * - No size limit (no base64 encoding)
     * - Faster processing (no encode/decode)
     * - Simpler implementation
     */
    public void processDocumentStreaming(byte[] fileData, String filename, String requestId)
            throws Exception {

        logger.info("Processing document: {} ({} bytes, requestId: {})",
                filename, fileData.length, requestId);

        // Save file to upload directory
        String uploadPath = saveUploadedFile(fileData, filename, requestId);
        logger.info("File saved to: {}", uploadPath);

        // Send file path to C++ worker (no size limit!)
        Map<String, Object> request = new HashMap<>();
        request.put("filename", filename);
        request.put("filePath", uploadPath); // Send path instead of data!
        request.put("fileSize", fileData.length);
        request.put("requestId", requestId);
        request.put("clientId", "java-cpp-client");
        request.put("action", "stream_pages");
        request.put("outputDir", OUTPUT_DIR); // Where C++ should save results

        String requestJson = objectMapper.writeValueAsString(request);

        // Send to C++ - don't wait for response
        // C++ will read file from path and stream pages back via gRPC events
        CompletableFuture<String> future = cppWorkerClient.callWorker(
                WORKER_ID,
                "process_document",
                requestJson);

        // Get acknowledgment (C++ responds immediately, then streams pages)
        String ack = future.get();
        logger.info("C++ streaming initiated: {}", ack);
    }

    /**
     * Save uploaded file to disk
     * Returns absolute path to saved file
     */
    private String saveUploadedFile(byte[] fileData, String filename, String requestId)
            throws IOException {

        // Create unique filename: requestId_originalFilename
        String safeFilename = requestId + "_" + filename.replaceAll("[^a-zA-Z0-9._-]", "_");
        Path filePath = Paths.get(UPLOAD_DIR, safeFilename);

        // Write file
        Files.write(filePath, fileData);

        return filePath.toAbsolutePath().toString();
    }

    /**
     * Get specific page from document
     */
    public Map<String, Object> getSpecificPage2(byte[] fileData, String filename, int pageNumber)
            throws Exception {

        logger.info("Extracting page {} from: {}", pageNumber, filename);

        Map<String, Object> request = new HashMap<>();
        request.put("filename", filename);
        request.put("data", Base64.getEncoder().encodeToString(fileData));
        request.put("pageNumber", pageNumber);
        request.put("action", "get_page");

        String requestJson = objectMapper.writeValueAsString(request);

        CompletableFuture<String> future = cppWorkerClient.callWorker(
                WORKER_ID,
                "get_page",
                requestJson);

        String responseJson = future.get();

        @SuppressWarnings("unchecked")
        Map<String, Object> response = objectMapper.readValue(responseJson, Map.class);

        return response;
    }
}
