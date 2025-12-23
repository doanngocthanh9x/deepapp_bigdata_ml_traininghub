package com.deepapp.vn.io.modules.demo;

import com.deepapp.vn.io.workers.CppWorkerClient;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

/**
 * Demo Controller - Java + C++ Integration
 */
@RestController
@RequestMapping("/api/demo")
@Tag(name = "Demo", description = "Demo endpoints for testing Java ↔ C++ communication")
public class DemoController {

    private static final Logger logger = LoggerFactory.getLogger(DemoController.class);

    @Autowired
    private CppWorkerClient cppWorkerClient;

    @Operation(summary = "Health check", description = "Check if demo module is running")
    @ApiResponse(responseCode = "200", description = "Module is healthy")
    @GetMapping("/health")
    public ResponseEntity<Map<String, String>> health() {
        Map<String, String> response = new HashMap<>();
        response.put("status", "healthy");
        response.put("module", "demo");
        return ResponseEntity.ok(response);
    }

    /**
     * Demo: Java calls C++ worker, processes result, and returns to client
     * Flow: Client -> Java -> C++ -> Java -> Client
     */
    @Operation(
        summary = "Calculate with C++ worker",
        description = "Performs calculation using C++ worker via gRPC"
    )
    @ApiResponse(responseCode = "200", description = "Calculation successful")
    @PostMapping("/calculate")
    public ResponseEntity<Map<String, Object>> calculate(
        @Parameter(description = "Calculation request with operation and values")
        @RequestBody CalculateRequest request) {
        try {
            logger.info("=== Starting Java + C++ Integration Demo ===");
            logger.info("Input from client: {}", request.getValue());

            // Step 1: Java pre-processing
            int javaValue = request.getValue() * 2;
            logger.info("Step 1 - Java pre-processing: {} * 2 = {}", request.getValue(), javaValue);

            // Step 2: Call C++ worker AAA0_0100_W
            logger.info("Step 2 - Calling C++ worker AAA0_0100_W...");
            String cppResult = cppWorkerClient
                    .callAAA0_0100("process", String.valueOf(javaValue))
                    .get();
            logger.info("Step 2 - C++ result: {}", cppResult);
            
            Map<String, Object> cppData = parseJsonResponse(cppResult);

            // Step 3: Java post-processing
            int finalResult = javaValue + 100;
            logger.info("Step 3 - Java post-processing: {} + 100 = {}", javaValue, finalResult);

            // Build response
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("input", request.getValue());
            response.put("javaPreProcessing", javaValue);
            response.put("cppResponse", cppData);
            response.put("javaPostProcessing", finalResult);
            response.put("flow", "Client -> Java -> C++ -> Java -> Client");

            logger.info("=== Demo Complete ===");
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            logger.error("Error in demo: {}", e.getMessage(), e);
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("error", e.getMessage());
            return ResponseEntity.status(500).body(error);
        }
    }

    /**
     * Demo: Echo test with C++ worker
     */
    @PostMapping("/echo")
    public ResponseEntity<Map<String, Object>> echo(@RequestBody EchoRequest request) {
        try {
            logger.info("Echo test: sending '{}' to C++ worker", request.getMessage());

            // Call C++ worker
            String cppResult = cppWorkerClient
                    .callAAA0_0100("echo", request.getMessage())
                    .get();
            
            logger.info("C++ response: {}", cppResult);

            // Parse C++ JSON response
            Map<String, Object> cppData = parseJsonResponse(cppResult);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("input", request.getMessage());
            response.put("cppResponse", cppData);
            response.put("javaTimestamp", System.currentTimeMillis());

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            logger.error("Error in echo: {}", e.getMessage(), e);
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("error", e.getMessage());
            return ResponseEntity.status(500).body(error);
        }
    }

    /**
     * Demo: Transform data with both Java and C++
     */
    @PostMapping("/transform")
    public ResponseEntity<Map<String, Object>> transform(@RequestBody TransformRequest request) {
        try {
            logger.info("Transform test: '{}'", request.getData());

            // Java transformation
            String javaTransformed = "JAVA_PREFIX_" + request.getData().toUpperCase();
            logger.info("Java transformed: {}", javaTransformed);

            // C++ transformation
            String cppResult = cppWorkerClient
                    .callAAA0_0100("transform", javaTransformed)
                    .get();
            logger.info("C++ result: {}", cppResult);
            
            Map<String, Object> cppData = parseJsonResponse(cppResult);
            String cppTransformed = cppData.getOrDefault("data", cppResult).toString();

            // Final Java processing
            String finalResult = cppTransformed + "_JAVA_SUFFIX";

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("original", request.getData());
            response.put("javaStep", javaTransformed);
            response.put("cppResponse", cppData);
            response.put("final", finalResult);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            logger.error("Error in transform: {}", e.getMessage(), e);
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("error", e.getMessage());
            return ResponseEntity.status(500).body(error);
        }
    }

    /**
     * Parse JSON string to Map (simple parser without Jackson)
     */
    private Map<String, Object> parseJsonResponse(String json) {
        Map<String, Object> result = new HashMap<>();
        result.put("rawResponse", json);
        // For demo, just return raw JSON - client can parse it
        return result;
    }

    // DTOs
    public static class CalculateRequest {
        private int value;

        public int getValue() {
            return value;
        }

        public void setValue(int value) {
            this.value = value;
        }
    }

    public static class EchoRequest {
        private String message;

        public String getMessage() {
            return message;
        }

        public void setMessage(String message) {
            this.message = message;
        }
    }

    public static class TransformRequest {
        private String data;

        public String getData() {
            return data;
        }

        public void setData(String data) {
            this.data = data;
        }
    }
}
