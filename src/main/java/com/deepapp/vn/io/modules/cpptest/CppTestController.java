package com.deepapp.vn.io.modules.cpptest;

import com.deepapp.vn.io.workers.CppWorkerClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * Test Controller for C++ Workers
 */
@RestController
@RequestMapping("/api/cpp")
public class CppTestController {

    @Autowired
    private CppWorkerClient cppWorkerClient;

    @GetMapping("/health")
    public ResponseEntity<Map<String, String>> health() {
        Map<String, String> response = new HashMap<>();
        response.put("status", "healthy");
        response.put("module", "cpp-test");
        return ResponseEntity.ok(response);
    }

    /**
     * Call any C++ worker
     */
    @PostMapping("/call")
    public ResponseEntity<Map<String, Object>> callWorker(@RequestBody WorkerRequest request) {
        try {
            String result = cppWorkerClient
                    .callWorker(request.getTaskId(), request.getEventType(), request.getData())
                    .get();

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("result", result);
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("error", e.getMessage());
            return ResponseEntity.status(500).body(error);
        }
    }

    /**
     * Call AAA0_0100_W worker
     */
    @PostMapping("/aaa0_0100")
    public ResponseEntity<Map<String, Object>> callAAA0_0100(@RequestBody SimpleRequest request) {
        try {
            String result = cppWorkerClient
                    .callAAA0_0100(request.getEventType(), request.getData())
                    .get();

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("worker", "AAA0_0100_W");
            response.put("result", result);
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("error", e.getMessage());
            return ResponseEntity.status(500).body(error);
        }
    }

    /**
     * Call AAA0_0200_W worker
     */
    @PostMapping("/aaa0_0200")
    public ResponseEntity<Map<String, Object>> callAAA0_0200(@RequestBody SimpleRequest request) {
        try {
            String result = cppWorkerClient
                    .callAAA0_0200(request.getEventType(), request.getData())
                    .get();

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("worker", "AAA0_0200_W");
            response.put("result", result);
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("error", e.getMessage());
            return ResponseEntity.status(500).body(error);
        }
    }

    // DTOs
    public static class WorkerRequest {
        private String taskId;
        private String eventType;
        private String data;

        public String getTaskId() {
            return taskId;
        }

        public void setTaskId(String taskId) {
            this.taskId = taskId;
        }

        public String getEventType() {
            return eventType;
        }

        public void setEventType(String eventType) {
            this.eventType = eventType;
        }

        public String getData() {
            return data;
        }

        public void setData(String data) {
            this.data = data;
        }
    }

    public static class SimpleRequest {
        private String eventType;
        private String data;

        public String getEventType() {
            return eventType;
        }

        public void setEventType(String eventType) {
            this.eventType = eventType;
        }

        public String getData() {
            return data;
        }

        public void setData(String data) {
            this.data = data;
        }
    }
}
