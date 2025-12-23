package com.deepapp.vn.io.workers;

import com.deepapp.hub.EventChunk;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * Example Worker Client 1 - OCR Worker
 * This demonstrates how to create a specific worker client for OCR tasks
 */
@Service
public class OcrWorkerClient extends BaseWorkerClient {

    private static final Logger logger = LoggerFactory.getLogger(OcrWorkerClient.class);

    @Value("${workers.ocr.targetId:cpp-ocr-worker}")
    private String ocrTargetId;

    public OcrWorkerClient(
            @Value("${workers.ocr.host:localhost}") String ocrHost,
            @Value("${workers.ocr.port:50052}") int ocrPort) {
        super("java-ocr-client", ocrHost, ocrPort);
        logger.info("OcrWorkerClient created with host={}, port={}", ocrHost, ocrPort);
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        super.afterPropertiesSet();
        logger.info("OCR Worker Client initialized - target: {}", ocrTargetId);
    }

    @Override
    protected String getWorkerTargetId() {
        return ocrTargetId;
    }

    /**
     * Perform OCR on an image
     */
    public java.util.concurrent.CompletableFuture<String> performOcr(String imagePath) {
        logger.info("Requesting OCR for image: {}", imagePath);
        return executeTask("ocr", imagePath);
    }

    /**
     * Perform OCR with language specification
     */
    public java.util.concurrent.CompletableFuture<String> performOcr(String imagePath, String language) {
        String taskData = String.format("{\"imagePath\":\"%s\",\"language\":\"%s\"}", imagePath, language);
        return executeTask("ocr", taskData);
    }

    @Override
    protected void handleWorkerEvent(EventChunk event) {
        logger.info("OCR Worker event: {} - {}", event.getEventType(), event.getPayload().toStringUtf8());
    }
}
