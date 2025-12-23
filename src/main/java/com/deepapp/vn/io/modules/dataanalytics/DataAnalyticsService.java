package com.deepapp.vn.io.modules.dataanalytics;

import com.deepapp.vn.io.workers.OcrWorkerClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Data Analytics Service
 * Another example module demonstrating different use of workers
 */
@Service
public class DataAnalyticsService {

    private static final Logger logger = LoggerFactory.getLogger(DataAnalyticsService.class);

    @Autowired
    private OcrWorkerClient ocrWorkerClient;

    /**
     * Batch process multiple images
     */
    public CompletableFuture<List<BatchResult>> batchProcessImages(List<String> imagePaths) {
        logger.info("Starting batch processing for {} images", imagePaths.size());

        List<CompletableFuture<BatchResult>> futures = new ArrayList<>();

        for (String imagePath : imagePaths) {
            CompletableFuture<BatchResult> future = ocrWorkerClient.performOcr(imagePath)
                    .thenApply(text -> new BatchResult(imagePath, text, true, null))
                    .exceptionally(ex -> new BatchResult(imagePath, null, false, ex.getMessage()));
            futures.add(future);
        }

        return CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
                .thenApply(v -> {
                    List<BatchResult> results = new ArrayList<>();
                    for (CompletableFuture<BatchResult> future : futures) {
                        results.add(future.join());
                    }
                    return results;
                });
    }

    /**
     * Analyze image and return statistics
     */
    public CompletableFuture<AnalysisResult> analyzeImage(String imagePath) {
        logger.info("Analyzing image: {}", imagePath);

        return ocrWorkerClient.performOcr(imagePath)
                .thenApply(text -> {
                    AnalysisResult result = new AnalysisResult();
                    result.setImagePath(imagePath);
                    result.setTextLength(text.length());
                    result.setWordCount(text.split("\\s+").length);
                    result.setCharacterCount(text.length());
                    return result;
                });
    }

    public static class BatchResult {
        private final String imagePath;
        private final String text;
        private final boolean success;
        private final String error;

        public BatchResult(String imagePath, String text, boolean success, String error) {
            this.imagePath = imagePath;
            this.text = text;
            this.success = success;
            this.error = error;
        }

        public String getImagePath() {
            return imagePath;
        }

        public String getText() {
            return text;
        }

        public boolean isSuccess() {
            return success;
        }

        public String getError() {
            return error;
        }
    }

    public static class AnalysisResult {
        private String imagePath;
        private int textLength;
        private int wordCount;
        private int characterCount;

        public String getImagePath() {
            return imagePath;
        }

        public void setImagePath(String imagePath) {
            this.imagePath = imagePath;
        }

        public int getTextLength() {
            return textLength;
        }

        public void setTextLength(int textLength) {
            this.textLength = textLength;
        }

        public int getWordCount() {
            return wordCount;
        }

        public void setWordCount(int wordCount) {
            this.wordCount = wordCount;
        }

        public int getCharacterCount() {
            return characterCount;
        }

        public void setCharacterCount(int characterCount) {
            this.characterCount = characterCount;
        }
    }
}
