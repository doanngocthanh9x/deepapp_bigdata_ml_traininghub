package com.deepapp.vn.io.workers;

import com.deepapp.hub.EventChunk;
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

        // Create metadata with taskId for internal Python routing
        Map<String, String> metadata = new HashMap<>();
        metadata.put("taskId", taskId);

        // Send to python-worker, not taskId directly
        return sendEventAndWaitForResponse(pythonTargetId, eventType, payload, metadata, 30000)
                .thenApply(event -> event.getPayload().toStringUtf8());
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