package com.deepapp.vn.io.workers;

import com.deepapp.hub.EventChunk;
import com.deepapp.vn.io.infrastructure.cache.RequestResponseCache;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * Python Worker Client
 * Calls Python workers via gRPC
 */
@Service
public class PythonWorkerClient extends BaseWorkerClient {

    private static final Logger logger = LoggerFactory.getLogger(PythonWorkerClient.class);

    @Value("${workers.python.targetId:python-worker}")
    private String pythonTargetId;

    private final ObjectMapper objectMapper = new ObjectMapper();

    public PythonWorkerClient(
            @Value("${workers.python.host:localhost}") String host,
            @Value("${workers.python.port:50051}") int port) {
        super("java-python-client", host, port);
        logger.info("PythonWorkerClient created with host={}, port={}", host, port);
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        super.afterPropertiesSet();
        logger.info("Python Worker Client initialized - target: {}", pythonTargetId);
    }

    @Override
    protected String getWorkerTargetId() {
        return pythonTargetId;
    }

    /**
     * Call a specific Python worker task
     * @param taskId The worker task ID (e.g., "AAA0_0101_W")
     * @param eventType The event type (e.g., "vietocr", "extract_text_from_bboxes")
     * @param taskData The task data
     */
    public CompletableFuture<String> callWorker(
            String taskId, String eventType, String taskData) {
        logger.info("Calling Python worker: taskId={}, eventType={}", taskId, eventType);

        // Send to python-worker with taskId in metadata
        byte[] payload = taskData.getBytes(java.nio.charset.StandardCharsets.UTF_8);

        // Debug: Log payload size
        logger.info("Request payload size: {} bytes ({} KB)", payload.length, payload.length / 1024.0);

        // If payload is too large (> 50KB), use SQLite cache to avoid gRPC buffer overflow
        String actualPayloadStr = taskData;  // Default: send original payload
        final Map<String, String> metadata = new HashMap<>();
        metadata.put("taskId", taskId);

        if (payload.length > 50 * 1024) {
            // Payload too large - store in SQLite cache
            logger.info("Payload too large ({} KB) - storing in SQLite cache", payload.length / 1024.0);

            try {
                // Store original payload in cache
                Map<String, Object> cacheData = new HashMap<>();
                cacheData.put("payload", taskData);
                cacheData.put("taskId", taskId);
                cacheData.put("eventType", eventType);

                String cacheId = RequestResponseCache.storeData(cacheData, 300); // 5 min TTL

                if (cacheId != null) {
                    // Send only cache ID through gRPC
                    Map<String, Object> cacheRef = new HashMap<>();
                    cacheRef.put("cached_request", true);
                    cacheRef.put("cache_id", cacheId);

                    actualPayloadStr = objectMapper.writeValueAsString(cacheRef);
                    metadata.put("cached_request", "true");
                    logger.info("Stored request in cache with ID: {}", cacheId);
                } else {
                    logger.error("Failed to store payload in cache - sending directly (may fail)");
                }
            } catch (Exception e) {
                logger.error("Error caching request - sending directly", e);
            }
        }

        byte[] finalPayload = actualPayloadStr.getBytes(java.nio.charset.StandardCharsets.UTF_8);
        logger.info("Final gRPC payload size: {} bytes ({} KB)", finalPayload.length, finalPayload.length / 1024.0);

        // Send to python-worker, not taskId directly
        return sendEventAndWaitForResponse(pythonTargetId, eventType, finalPayload, metadata, 120000)
                .thenApply(event -> {
                    byte[] responsePayload = event.getPayload().toByteArray();
                    logger.info("Response payload size: {} bytes ({} KB)", responsePayload.length, responsePayload.length / 1024.0);
                    return event.getPayload().toStringUtf8();
                });
    }

    /**
     * Call AAA0_0101_W Python OCR worker
     */
    public CompletableFuture<String> callAAA0_0101(
            String eventType, String data) {
        return callWorker("AAA0_0101_W", eventType, data);
    }

    @Override
    protected void handleWorkerEvent(EventChunk event) {
        logger.info("Python Worker event: {} - {}",
                   event.getEventType(),
                   event.getPayload().toStringUtf8().substring(0, Math.min(200, event.getPayload().toStringUtf8().length())));
    }
}