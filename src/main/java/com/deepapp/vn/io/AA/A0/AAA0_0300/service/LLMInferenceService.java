package com.deepapp.vn.io.AA.A0.AAA0_0300.service;

import com.deepapp.vn.io.AA.A0.AAA0_0300.model.InferenceRequest;
import com.deepapp.vn.io.AA.A0.AAA0_0300.model.InferenceResponse;
import com.deepapp.vn.io.workers.PythonWorkerClient;
import com.deepapp.vn.io.workers.CppWorkerClient;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

@Service
public class LLMInferenceService {

    private static final Logger logger = LoggerFactory.getLogger(LLMInferenceService.class);
    
    private static final String PYTHON_WORKER_ID = "AAA0_0300_W";
    private static final String CPP_WORKER_ID = "AAA0_0300_CPP_W";

    @Autowired
    private PythonWorkerClient pythonWorkerClient;

    @Autowired
    private CppWorkerClient cppWorkerClient;

    private final ObjectMapper objectMapper = new ObjectMapper();

    // Statistics tracking
    private final AtomicInteger totalInferences = new AtomicInteger(0);
    private final AtomicLong totalInferenceTime = new AtomicLong(0);
    private final AtomicInteger totalTokens = new AtomicInteger(0);
    private final Map<String, AtomicInteger> workerUsage = new ConcurrentHashMap<>();

    public LLMInferenceService() {
        workerUsage.put("python", new AtomicInteger(0));
        workerUsage.put("cpp", new AtomicInteger(0));
    }

    /**
     * Run inference using Python worker (llama-cpp-python)
     */
    public InferenceResponse inferenceWithPythonWorker(InferenceRequest request) {
        logger.info("🐍 Using Python worker for inference");
        
        long startTime = System.currentTimeMillis();
        
        try {
            // Prepare payload
            Map<String, Object> payload = new HashMap<>();
            payload.put("prompt", request.getPrompt());
            payload.put("temperature", request.getTemperature());
            payload.put("max_tokens", request.getMaxTokens());
            payload.put("model_name", request.getModelName());
            
            if (request.getChatHistory() != null) {
                payload.put("chat_history", request.getChatHistory());
            }

            String payloadJson = objectMapper.writeValueAsString(payload);

            // Call Python worker via gRPC
            String workerResponse = pythonWorkerClient.callWorker(
                PYTHON_WORKER_ID,
                "inference",
                payloadJson
            ).get(); // Block and wait for response

            // Parse response
            @SuppressWarnings("unchecked")
            Map<String, Object> result = objectMapper.readValue(workerResponse, Map.class);
            
            long inferenceTime = System.currentTimeMillis() - startTime;
            
            InferenceResponse response = new InferenceResponse();
            response.setStatus((String) result.get("status"));
            response.setResponse((String) result.get("response"));
            response.setTokens((Integer) result.getOrDefault("tokens", 0));
            response.setInferenceTime(inferenceTime);
            response.setWorkerType("python");
            response.setModelName(request.getModelName());

            // Update statistics
            updateStatistics(inferenceTime, response.getTokens(), "python");

            return response;

        } catch (Exception e) {
            logger.error("Python worker inference failed: {}", e.getMessage(), e);
            return InferenceResponse.error("Python worker error: " + e.getMessage());
        }
    }

    /**
     * Run inference using C++ worker (llama.cpp native)
     */
    public InferenceResponse inferenceWithCppWorker(InferenceRequest request) {
        logger.info("⚡ Using C++ worker for inference");
        
        long startTime = System.currentTimeMillis();
        
        try {
            // Prepare payload
            Map<String, Object> payload = new HashMap<>();
            payload.put("prompt", request.getPrompt());
            payload.put("temperature", request.getTemperature());
            payload.put("max_tokens", request.getMaxTokens());
            payload.put("model_name", request.getModelName());

            String payloadJson = objectMapper.writeValueAsString(payload);

            // Call C++ worker via gRPC
            String workerResponse = cppWorkerClient.callWorker(
                CPP_WORKER_ID,
                "inference",
                payloadJson
            ).get(); // Block and wait for response

            // Parse response
            @SuppressWarnings("unchecked")
            Map<String, Object> result = objectMapper.readValue(workerResponse, Map.class);
            
            long inferenceTime = System.currentTimeMillis() - startTime;
            
            InferenceResponse response = new InferenceResponse();
            response.setStatus((String) result.get("status"));
            response.setResponse((String) result.get("response"));
            response.setTokens((Integer) result.getOrDefault("tokens", 0));
            response.setInferenceTime(inferenceTime);
            response.setWorkerType("cpp");
            response.setModelName(request.getModelName());

            // Update statistics
            updateStatistics(inferenceTime, response.getTokens(), "cpp");

            return response;

        } catch (Exception e) {
            logger.error("C++ worker inference failed: {}", e.getMessage(), e);
            return InferenceResponse.error("C++ worker error: " + e.getMessage());
        }
    }

    /**
     * Update inference statistics
     */
    private void updateStatistics(long inferenceTime, int tokens, String workerType) {
        totalInferences.incrementAndGet();
        totalInferenceTime.addAndGet(inferenceTime);
        totalTokens.addAndGet(tokens);
        workerUsage.get(workerType).incrementAndGet();
    }

    /**
     * Get inference statistics
     */
    public Map<String, Object> getStatistics() {
        int totalCount = totalInferences.get();
        long totalTime = totalInferenceTime.get();
        int totalToks = totalTokens.get();

        Map<String, Object> stats = new HashMap<>();
        stats.put("totalInferences", totalCount);
        stats.put("avgInferenceTime", totalCount > 0 ? (double) totalTime / totalCount / 1000.0 : 0.0);
        stats.put("totalTokens", totalToks);
        stats.put("avgTokensPerRequest", totalCount > 0 ? (double) totalToks / totalCount : 0.0);
        
        Map<String, Integer> workerStats = new HashMap<>();
        workerUsage.forEach((worker, count) -> workerStats.put(worker, count.get()));
        stats.put("workerUsage", workerStats);

        return stats;
    }

    /**
     * Clear response cache (if needed in the future)
     */
    public void clearCache() {
        // Cache clearing logic can be added here if needed
        logger.info("Cache clear requested (no-op for now)");
    }
}
