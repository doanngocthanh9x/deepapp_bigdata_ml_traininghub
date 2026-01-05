package com.deepapp.vn.io.AA.A0.AAA0_0300.controller;

import com.deepapp.vn.io.AA.A0.AAA0_0300.model.InferenceRequest;
import com.deepapp.vn.io.AA.A0.AAA0_0300.model.InferenceResponse;
import com.deepapp.vn.io.AA.A0.AAA0_0300.service.LLMInferenceService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * AAA0_0300 - LLM Inference Controller
 * 
 * Provides REST API for Vietnamese Language Model inference
 * Supports both Python (llama-cpp-python) and C++ (llama.cpp) workers
 */
@RestController
@RequestMapping("/AA/A0/AAA0_0300")
@Tag(name = "AAA0_0300 - LLM Inference", description = "Vietnamese LLM inference with llama.cpp")
public class AAA0_0300Controller {

    private static final Logger logger = LoggerFactory.getLogger(AAA0_0300Controller.class);

    @Autowired
    private LLMInferenceService llmInferenceService;

    /**
     * Health check endpoint
     */
    @GetMapping
    @Operation(summary = "Health check", description = "Check if LLM inference service is available")
    public ResponseEntity<Map<String, Object>> healthCheck() {
        Map<String, Object> response = new HashMap<>();
        response.put("service", "AAA0_0300 - LLM Inference");
        response.put("status", "active");
        response.put("workers", List.of("python", "cpp"));
        response.put("models", List.of("vinallama-7b-chat", "vietcuna-7b", "phobert-base"));
        response.put("timestamp", System.currentTimeMillis());
        
        logger.info("Health check requested");
        return ResponseEntity.ok(response);
    }

    /**
     * Run LLM inference
     */
    @PostMapping("/inference")
    @Operation(
        summary = "Run LLM Inference",
        description = "Generate text using Vietnamese LLM model"
    )
    public ResponseEntity<InferenceResponse> runInference(@RequestBody InferenceRequest request) {
        logger.info("🤖 Inference request received - Worker: {}, Model: {}", 
            request.getWorkerType(), request.getModelName());
        
        try {
            // Validate request
            if (request.getPrompt() == null || request.getPrompt().trim().isEmpty()) {
                return ResponseEntity.badRequest()
                    .body(InferenceResponse.error("Prompt cannot be empty"));
            }

            // Route to appropriate worker
            InferenceResponse response;
            if ("cpp".equalsIgnoreCase(request.getWorkerType())) {
                response = llmInferenceService.inferenceWithCppWorker(request);
            } else {
                response = llmInferenceService.inferenceWithPythonWorker(request);
            }

            logger.info("✅ Inference completed successfully");
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            logger.error("❌ Inference failed: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError()
                .body(InferenceResponse.error("Inference failed: " + e.getMessage()));
        }
    }

    /**
     * Get inference statistics
     */
    @GetMapping("/stats")
    @Operation(summary = "Get inference statistics", description = "Get usage statistics")
    public ResponseEntity<Map<String, Object>> getStats() {
        try {
            Map<String, Object> stats = llmInferenceService.getStatistics();
            return ResponseEntity.ok(stats);
        } catch (Exception e) {
            logger.error("Failed to get stats: {}", e.getMessage());
            return ResponseEntity.internalServerError()
                .body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * List available models
     */
    @GetMapping("/models")
    @Operation(summary = "List models", description = "Get list of available LLM models")
    public ResponseEntity<Map<String, Object>> listModels() {
        Map<String, Object> response = new HashMap<>();
        response.put("models", List.of(
            Map.of(
                "id", "vinallama-7b-chat",
                "name", "VinAllama 7B Chat",
                "description", "Vietnamese instruction-following model",
                "size", "7B parameters",
                "format", "GGUF",
                "quantization", "Q5_0"
            ),
            Map.of(
                "id", "vietcuna-7b",
                "name", "VietCuna 7B",
                "description", "Vietnamese conversational model",
                "size", "7B parameters",
                "format", "GGUF",
                "quantization", "Q5_K_M"
            ),
            Map.of(
                "id", "phobert-base",
                "name", "PhoBERT Base",
                "description", "Vietnamese BERT model",
                "size", "135M parameters",
                "format", "GGUF",
                "quantization", "F16"
            )
        ));
        
        return ResponseEntity.ok(response);
    }

    /**
     * Clear inference cache
     */
    @PostMapping("/cache/clear")
    @Operation(summary = "Clear cache", description = "Clear inference response cache")
    public ResponseEntity<Map<String, Object>> clearCache() {
        try {
            llmInferenceService.clearCache();
            return ResponseEntity.ok(Map.of(
                "status", "success",
                "message", "Cache cleared successfully"
            ));
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                .body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Test endpoint for quick testing
     */
    @PostMapping("/test")
    @Operation(summary = "Test inference", description = "Quick test with predefined prompt")
    public ResponseEntity<InferenceResponse> testInference(
        @RequestParam(defaultValue = "python") String workerType
    ) {
        InferenceRequest request = new InferenceRequest();
        request.setPrompt("Xin chào! Bạn có thể giúp tôi không?");
        request.setWorkerType(workerType);
        request.setTemperature(0.1);
        request.setMaxTokens(50);
        request.setModelName("vinallama-7b-chat");
        
        return runInference(request);
    }
}
