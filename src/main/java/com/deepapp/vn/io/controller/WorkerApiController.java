package com.deepapp.vn.io.controller;

import com.deepapp.vn.io.workers.PythonWorkerClient;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

/**
 * General API controller for calling Python workers
 * Provides a unified interface to access any registered Python worker
 */
@RestController
@RequestMapping("/api/worker")
@Tag(name = "Worker API", description = "Unified API for calling Python workers")
public class WorkerApiController {

    private static final Logger logger = LoggerFactory.getLogger(WorkerApiController.class);

    @Autowired
    private PythonWorkerClient pythonWorkerClient;

    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * Call a Python worker with JSON payload
     */
    @PostMapping("/{workerName}")
    @Operation(
        summary = "Call Python Worker",
        description = "Send a task to a specific Python worker"
    )
    public ResponseEntity<Map<String, Object>> callWorker(
            @Parameter(description = "Worker name (e.g., AAA0_0203_W)")
            @PathVariable String workerName,
            @Parameter(description = "Request payload with event_type and payload")
            @RequestBody Map<String, Object> request) {

        try {
            String eventType = (String) request.get("event_type");
            Object payload = request.get("payload");

            if (eventType == null) {
                return ResponseEntity.badRequest()
                    .body(createErrorResponse("Missing event_type"));
            }

            logger.info("Calling worker: {} with event: {}", workerName, eventType);

            // Convert payload to JSON string
            String payloadJson = payload != null ?
                objectMapper.writeValueAsString(payload) : "{}";

            // Call Python worker
            CompletableFuture<String> future = pythonWorkerClient.callWorker(
                workerName, eventType, payloadJson);

            // Wait for response with timeout
            String responseJson = future.get(30, TimeUnit.SECONDS);

            // Parse response
            JsonNode responseNode = objectMapper.readTree(responseJson);

            // Convert to Map for ResponseEntity
            Map<String, Object> response = objectMapper.convertValue(responseNode, Map.class);

            logger.info("Worker {} responded successfully for event {}", workerName, eventType);
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            logger.error("Error calling worker {}: {}", workerName, e.getMessage(), e);

            // Check if it's a timeout
            if (e.getCause() instanceof java.util.concurrent.TimeoutException) {
                return ResponseEntity.status(HttpStatus.REQUEST_TIMEOUT)
                    .body(createErrorResponse("Worker call timed out"));
            }

            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(createErrorResponse("Worker call failed: " + e.getMessage()));
        }
    }

    /**
     * Health check for worker API
     */
    @GetMapping("/health")
    @Operation(summary = "Worker API Health Check")
    public ResponseEntity<Map<String, Object>> health() {
        Map<String, Object> response = new HashMap<>();
        response.put("status", "healthy");
        response.put("service", "worker_api");
        response.put("pythonWorkerClient", pythonWorkerClient != null ? "available" : "unavailable");
        return ResponseEntity.ok(response);
    }

    private Map<String, Object> createErrorResponse(String message) {
        Map<String, Object> error = new HashMap<>();
        error.put("status", "error");
        error.put("message", message);
        return error;
    }
}