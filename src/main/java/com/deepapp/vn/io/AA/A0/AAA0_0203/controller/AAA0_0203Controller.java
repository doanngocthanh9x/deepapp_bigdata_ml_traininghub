package com.deepapp.vn.io.AA.A0.AAA0_0203.controller;

import com.deepapp.vn.io.AA.A0.AAA0_0203.model.NERTrainingRequest;
import com.deepapp.vn.io.AA.A0.AAA0_0203.model.NERTrainingResponse;
import com.deepapp.vn.io.AA.A0.AAA0_0203.service.NERTrainingService;
import com.deepapp.utils.PathUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

/**
 * AAA0_0203 - NER Training Data Preparation Controller
 *
 * Manages NER training data collection, validation, and export
 */
@RestController
@RequestMapping("/AA/A0/AAA0_0203")
@Tag(name = "AAA0_0203 - NER Training Data", description = "NER training data preparation and management")
public class AAA0_0203Controller {

    private static final Logger logger = LoggerFactory.getLogger(AAA0_0203Controller.class);

    @Autowired
    private NERTrainingService nerTrainingService;

    /**
     * Health check endpoint
     */
    @GetMapping
    @Operation(summary = "Health check", description = "Check if NER training service is available")
    public ResponseEntity<Object> healthCheck() {
        return ResponseEntity.ok()
            .body(java.util.Map.of(
                "service", "AAA0_0203 - NER Training Data Preparation",
                "status", "active",
                "features", java.util.List.of("training_data_collection", "ner_validation", "data_export"),
                "timestamp", System.currentTimeMillis()
            ));
    }

    /**
     * Process NER training request
     */
    @PostMapping("/process")
    @Operation(
        summary = "Process NER Training Request",
        description = "Handle various NER training data operations"
    )
    public ResponseEntity<NERTrainingResponse> processRequest(@RequestBody NERTrainingRequest request) {
        logger.info("NER training request received: eventType={}", request.getEventType());

        try {
            NERTrainingResponse response = nerTrainingService.processRequest(request);

            if ("success".equals(response.getStatus())) {
                logger.info("NER training operation successful: {}", request.getEventType());
                return ResponseEntity.ok(response);
            } else {
                logger.warn("NER training operation failed: {}", response.getMessage());
                return ResponseEntity.badRequest().body(response);
            }

        } catch (Exception e) {
            logger.error("NER training processing error", e);
            return ResponseEntity.internalServerError()
                .body(NERTrainingResponse.error("Internal error: " + e.getMessage()));
        }
    }

    /**
     * Process NER training with file upload (Multipart file upload)
     */
    @PostMapping(value = "/process", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(
        summary = "Process NER Training with File Upload",
        description = "Upload training data file and process NER training operations"
    )
    public ResponseEntity<NERTrainingResponse> processRequestWithFile(
            @RequestParam("file") MultipartFile file,
            @RequestParam("event_type") String eventType,
            @RequestParam(value = "template_id", required = false) String templateId,
            @RequestParam(value = "payload", required = false) String payloadJson) {

        logger.info("NER training file processing request received: filename={}, eventType={}, template={}",
                   file.getOriginalFilename(), eventType, templateId);

        try {
            // Save file to test directory using PathUtils
            String testDir = PathUtils.getTestDataPath();
            java.nio.file.Path testPath = java.nio.file.Paths.get(testDir);
            java.nio.file.Files.createDirectories(testPath);

            String filename = "training_" + System.currentTimeMillis() + "_" +
                            java.util.UUID.randomUUID().toString().substring(0, 8) +
                            getFileExtension(file.getOriginalFilename());
            java.nio.file.Path filePath = testPath.resolve(filename);
            java.nio.file.Files.write(filePath, file.getBytes());

            String trainingDataPath = filename; // Relative path for Python worker

            // Create request with file path
            NERTrainingRequest request = new NERTrainingRequest();
            request.setEventType(eventType);

            // Build payload with file path
            Map<String, Object> payload = new HashMap<>();
            payload.put("training_data_path", trainingDataPath);
            if (templateId != null && !templateId.isEmpty()) {
                payload.put("template_id", templateId);
            }
            if (payloadJson != null && !payloadJson.isEmpty()) {
                try {
                    Map<String, Object> additionalPayload = new com.fasterxml.jackson.databind.ObjectMapper()
                        .readValue(payloadJson, Map.class);
                    payload.putAll(additionalPayload);
                } catch (Exception e) {
                    logger.warn("Failed to parse additional payload JSON: {}", e.getMessage());
                }
            }

            request.setPayload(payload);

            NERTrainingResponse response = nerTrainingService.processRequest(request);

            if ("success".equals(response.getStatus())) {
                logger.info("NER training file processing successful: {}", eventType);
                return ResponseEntity.ok(response);
            } else {
                logger.warn("NER training file processing failed: {}", response.getMessage());
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
            }

        } catch (Exception e) {
            logger.error("NER training file processing error", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(NERTrainingResponse.error("Internal error: " + e.getMessage()));
        }
    }

    /**
     * List training data
     */
    @GetMapping("/training-data")
    @Operation(summary = "List Training Data", description = "Get list of available training datasets")
    public ResponseEntity<NERTrainingResponse> listTrainingData() {
        logger.info("Listing NER training data");

        try {
            NERTrainingResponse response = nerTrainingService.listTrainingData();
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            logger.error("Error listing training data", e);
            return ResponseEntity.internalServerError()
                .body(NERTrainingResponse.error("Failed to list training data: " + e.getMessage()));
        }
    }

    /**
     * Validate NER data
     */
    @PostMapping("/validate/{templateId}")
    @Operation(summary = "Validate NER Data", description = "Validate quality of training data for a template")
    public ResponseEntity<NERTrainingResponse> validateData(@PathVariable String templateId) {
        logger.info("Validating NER data for template: {}", templateId);

        try {
            NERTrainingResponse response = nerTrainingService.validateData(templateId);
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            logger.error("Error validating NER data", e);
            return ResponseEntity.internalServerError()
                .body(NERTrainingResponse.error("Failed to validate NER data: " + e.getMessage()));
        }
    }

    /**
     * Export NER format
     */
    @PostMapping("/export/{templateId}")
    @Operation(summary = "Export NER Format", description = "Export training data in NER format")
    public ResponseEntity<NERTrainingResponse> exportNerFormat(
            @PathVariable String templateId,
            @RequestParam(defaultValue = "json") String format) {
        logger.info("Exporting NER format for template: {} with format: {}", templateId, format);

        try {
            NERTrainingResponse response = nerTrainingService.exportNerFormat(templateId, format);
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            logger.error("Error exporting NER format", e);
            return ResponseEntity.internalServerError()
                .body(NERTrainingResponse.error("Failed to export NER format: " + e.getMessage()));
        }
    }

    /**
     * Get samples for a specific dataset
     */
    @GetMapping("/samples/{templateId}")
    @Operation(summary = "Get Dataset Samples", description = "Get detailed samples for a specific training dataset with pagination")
    public ResponseEntity<NERTrainingResponse> getDatasetSamples(
            @PathVariable String templateId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int page_size) {
        logger.info("Getting samples for dataset: {} (page: {}, page_size: {})", templateId, page, page_size);

        try {
            NERTrainingResponse response = nerTrainingService.getDatasetSamples(templateId, page, page_size);
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            logger.error("Error getting dataset samples", e);
            return ResponseEntity.internalServerError()
                .body(NERTrainingResponse.error("Failed to get dataset samples: " + e.getMessage()));
        }
    }

    /**
     * Bulk delete datasets
     */
    @PostMapping("/bulk-delete")
    @Operation(summary = "Bulk Delete Datasets", description = "Delete multiple training datasets")
    public ResponseEntity<NERTrainingResponse> bulkDeleteDatasets(@RequestBody java.util.Map<String, java.util.List<String>> request) {
        java.util.List<String> templateIds = request.get("template_ids");
        logger.info("Bulk deleting datasets: {}", templateIds);

        try {
            NERTrainingResponse response = nerTrainingService.bulkDeleteDatasets(templateIds);
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            logger.error("Error bulk deleting datasets", e);
            return ResponseEntity.internalServerError()
                .body(NERTrainingResponse.error("Failed to bulk delete datasets: " + e.getMessage()));
        }
    }

    /**
     * Bulk delete samples within a dataset
     */
    @PostMapping("/bulk-delete-samples")
    @Operation(summary = "Bulk Delete Samples", description = "Delete multiple samples within a specific dataset")
    public ResponseEntity<NERTrainingResponse> bulkDeleteSamples(@RequestBody java.util.Map<String, Object> request) {
        String templateId = (String) request.get("template_id");
        @SuppressWarnings("unchecked")
        java.util.List<String> sampleIds = (java.util.List<String>) request.get("sample_ids");
        logger.info("Bulk deleting samples for dataset {}: {}", templateId, sampleIds);

        try {
            NERTrainingResponse response = nerTrainingService.bulkDeleteSamples(templateId, sampleIds);
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            logger.error("Error bulk deleting samples", e);
            return ResponseEntity.internalServerError()
                .body(NERTrainingResponse.error("Failed to bulk delete samples: " + e.getMessage()));
        }
    }

    /**
     * Start NER model training
     */
    @PostMapping("/train")
    @Operation(summary = "Start NER Training", description = "Start training a NER model with specified configuration")
    public ResponseEntity<NERTrainingResponse> startTraining(@RequestBody java.util.Map<String, Object> request) {
        logger.info("Starting NER model training");

        try {
            NERTrainingResponse response = nerTrainingService.startTraining(request);
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            logger.error("Error starting training", e);
            return ResponseEntity.internalServerError()
                .body(NERTrainingResponse.error("Failed to start training: " + e.getMessage()));
        }
    }

    /**
     * Get training status
     */
    @GetMapping("/training/{trainingId}/status")
    @Operation(summary = "Get Training Status", description = "Get the status of a training job")
    public ResponseEntity<NERTrainingResponse> getTrainingStatus(@PathVariable String trainingId) {
        logger.info("Getting training status for: {}", trainingId);

        try {
            NERTrainingResponse response = nerTrainingService.getTrainingStatus(trainingId);
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            logger.error("Error getting training status", e);
            return ResponseEntity.internalServerError()
                .body(NERTrainingResponse.error("Failed to get training status: " + e.getMessage()));
        }
    }

    /**
     * List trained models
     */
    @GetMapping("/models")
    @Operation(summary = "List Trained Models", description = "Get list of all trained NER models")
    public ResponseEntity<NERTrainingResponse> listTrainedModels() {
        logger.info("Listing trained NER models");

        try {
            NERTrainingResponse response = nerTrainingService.listTrainedModels();
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            logger.error("Error listing trained models", e);
            return ResponseEntity.internalServerError()
                .body(NERTrainingResponse.error("Failed to list trained models: " + e.getMessage()));
        }
    }

    /**
     * Predict NER entities
     */
    @PostMapping("/predict")
    @Operation(summary = "Predict NER", description = "Use a trained model to predict NER entities in text")
    public ResponseEntity<NERTrainingResponse> predictNER(@RequestBody java.util.Map<String, Object> request) {
        logger.info("Predicting NER entities");

        try {
            NERTrainingResponse response = nerTrainingService.predictNER(request);
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            logger.error("Error predicting NER", e);
            return ResponseEntity.internalServerError()
                .body(NERTrainingResponse.error("Failed to predict NER: " + e.getMessage()));
        }
    }

    /**
     * Evaluate model
     */
    @PostMapping("/evaluate")
    @Operation(summary = "Evaluate Model", description = "Evaluate a trained NER model with test data")
    public ResponseEntity<NERTrainingResponse> evaluateModel(@RequestBody java.util.Map<String, Object> request) {
        logger.info("Evaluating NER model");

        try {
            NERTrainingResponse response = nerTrainingService.evaluateModel(request);
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            logger.error("Error evaluating model", e);
            return ResponseEntity.internalServerError()
                .body(NERTrainingResponse.error("Failed to evaluate model: " + e.getMessage()));
        }
    }

    /**
     * Helper method to extract file extension
     */
    private String getFileExtension(String filename) {
        if (filename == null || !filename.contains(".")) {
            return ".json"; // Default extension for training data
        }
        return filename.substring(filename.lastIndexOf("."));
    }
}