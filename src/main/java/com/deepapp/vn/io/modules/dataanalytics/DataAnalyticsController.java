package com.deepapp.vn.io.modules.dataanalytics;

import com.deepapp.vn.io.modules.dataanalytics.DataAnalyticsService.BatchResult;
import com.deepapp.vn.io.modules.dataanalytics.DataAnalyticsService.AnalysisResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Data Analytics REST Controller
 * Exposes endpoints for data analytics operations
 */
@RestController
@RequestMapping("/api/analytics")
public class DataAnalyticsController {

    @Autowired
    private DataAnalyticsService dataAnalyticsService;

    @GetMapping("/health")
    public ResponseEntity<Map<String, String>> health() {
        Map<String, String> response = new HashMap<>();
        response.put("status", "healthy");
        response.put("module", "data-analytics");
        return ResponseEntity.ok(response);
    }

    /**
     * Batch process multiple images
     */
    @PostMapping("/batch")
    public ResponseEntity<Map<String, Object>> batchProcess(@RequestBody BatchRequest request) {
        try {
            List<BatchResult> results = dataAnalyticsService
                    .batchProcessImages(request.getImagePaths())
                    .get();

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("results", results);
            response.put("totalProcessed", results.size());
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("error", e.getMessage());
            return ResponseEntity.status(500).body(error);
        }
    }

    /**
     * Analyze a single image
     */
    @PostMapping("/analyze")
    public ResponseEntity<Map<String, Object>> analyzeImage(@RequestBody AnalyzeRequest request) {
        try {
            AnalysisResult result = dataAnalyticsService
                    .analyzeImage(request.getImagePath())
                    .get();

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("analysis", result);
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("error", e.getMessage());
            return ResponseEntity.status(500).body(error);
        }
    }

    // Request DTOs
    public static class BatchRequest {
        private List<String> imagePaths;

        public List<String> getImagePaths() {
            return imagePaths;
        }

        public void setImagePaths(List<String> imagePaths) {
            this.imagePaths = imagePaths;
        }
    }

    public static class AnalyzeRequest {
        private String imagePath;

        public String getImagePath() {
            return imagePath;
        }

        public void setImagePath(String imagePath) {
            this.imagePath = imagePath;
        }
    }
}
