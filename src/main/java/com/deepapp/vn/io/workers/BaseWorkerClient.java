package com.deepapp.vn.io.workers;

import com.deepapp.hub.EventChunk;
import com.deepapp.vn.io.infrastructure.grpc.BaseGrpcClientService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;
import java.util.concurrent.CompletableFuture;

/**
 * Abstract base class for all C++ worker clients.
 * Provides common functionality for communicating with C++ workers.
 */
public abstract class BaseWorkerClient extends BaseGrpcClientService {

    private static final Logger logger = LoggerFactory.getLogger(BaseWorkerClient.class);

    public BaseWorkerClient(String workerId, String serverHost, int serverPort) {
        super(workerId, serverHost, serverPort);
    }

    /**
     * Send a task to the C++ worker and wait for result
     */
    public CompletableFuture<String> executeTask(String taskType, String taskData) {
        return executeTask(taskType, taskData, 30000); // 30 second default timeout
    }

    /**
     * Send a task to the C++ worker with custom timeout
     */
    public CompletableFuture<String> executeTask(String taskType, String taskData, long timeoutMs) {
        byte[] payload = taskData.getBytes(StandardCharsets.UTF_8);
        
        return sendEventAndWaitForResponse(getWorkerTargetId(), taskType, payload, timeoutMs)
                .thenApply(event -> event.getPayload().toStringUtf8());
    }

    /**
     * Send a fire-and-forget task to the worker
     */
    public void sendTaskAsync(String taskType, String taskData) {
        byte[] payload = taskData.getBytes(StandardCharsets.UTF_8);
        sendEvent(getWorkerTargetId(), taskType, payload);
    }

    /**
     * Get the target worker ID for this client
     */
    protected abstract String getWorkerTargetId();

    @Override
    protected void handleIncomingEvent(EventChunk event) {
        logger.info("Worker '{}' received event '{}' from '{}'",
                getClientId(), event.getEventType(), event.getSenderId());
        
        // Custom handling can be implemented by subclasses
        handleWorkerEvent(event);
    }

    /**
     * Override this to handle specific worker events
     */
    protected void handleWorkerEvent(EventChunk event) {
        // Default implementation - log only
        logger.debug("Unhandled worker event: {}", event.getEventType());
    }
}
