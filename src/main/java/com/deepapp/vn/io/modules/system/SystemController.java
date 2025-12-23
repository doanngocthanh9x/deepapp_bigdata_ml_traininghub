package com.deepapp.vn.io.modules.system;

import com.deepapp.vn.io.infrastructure.cpp.CppWorkerManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * System management endpoints
 */
@RestController
@RequestMapping("/api/system")
public class SystemController {

    @Autowired
    private CppWorkerManager cppWorkerManager;

    /**
     * Get system health status
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> health() {
        Map<String, Object> response = new HashMap<>();
        response.put("status", "healthy");
        response.put("service", "deepapp_main");
        response.put("cppWorker", cppWorkerManager.getWorkerInfo());
        response.put("cppWorkerRunning", cppWorkerManager.isWorkerRunning());
        return ResponseEntity.ok(response);
    }

    /**
     * Get C++ worker status
     */
    @GetMapping("/cpp/status")
    public ResponseEntity<Map<String, Object>> getCppWorkerStatus() {
        Map<String, Object> response = new HashMap<>();
        response.put("running", cppWorkerManager.isWorkerRunning());
        response.put("info", cppWorkerManager.getWorkerInfo());
        return ResponseEntity.ok(response);
    }

    /**
     * Restart C++ worker
     */
    @PostMapping("/cpp/restart")
    public ResponseEntity<Map<String, Object>> restartCppWorker() {
        try {
            cppWorkerManager.restartWorker();
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "C++ worker restarted successfully");
            response.put("status", cppWorkerManager.getWorkerInfo());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("error", e.getMessage());
            return ResponseEntity.status(500).body(error);
        }
    }
}
