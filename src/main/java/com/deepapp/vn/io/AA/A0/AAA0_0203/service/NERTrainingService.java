package com.deepapp.vn.io.AA.A0.AAA0_0203.service;

import com.deepapp.vn.io.AA.A0.AAA0_0203.model.NERTrainingRequest;
import com.deepapp.vn.io.AA.A0.AAA0_0203.model.NERTrainingResponse;
import com.deepapp.vn.io.workers.PythonWorkerClient;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

/**
 * Service for NER Training Data operations
 */
@Service
public class NERTrainingService {

    private static final Logger logger = LoggerFactory.getLogger(NERTrainingService.class);
    private static final String NER_TRAINING_WORKER_ID = "AAA0_0203_W";  // Python NER training worker task ID

    @Autowired
    private PythonWorkerClient pythonWorkerClient;

    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * Process NER training request
     */
    public NERTrainingResponse processRequest(NERTrainingRequest request) {
        try {
            String eventType = request.getEventType();
            Object payload = request.getPayload();

            logger.info("Processing NER training request: eventType={}", eventType);

            // Convert payload to JSON string
            String payloadJson = payload != null ?
                objectMapper.writeValueAsString(payload) : "{}";

            // Call NER training worker via gRPC
            String result = pythonWorkerClient.callWorker(NER_TRAINING_WORKER_ID, eventType, payloadJson).get();

            logger.info("NER training worker responded for event: {}", eventType);

            // Parse response from Python worker
            return parseWorkerResponse(result);

        } catch (Exception e) {
            logger.error("NER training processing failed", e);
            return NERTrainingResponse.error("NER training processing failed: " + e.getMessage());
        }
    }

    /**
     * List training data
     */
    public NERTrainingResponse listTrainingData() {
        try {
            logger.info("Listing NER training data");

            // Call worker to get file paths only
            String result = pythonWorkerClient.callWorker(NER_TRAINING_WORKER_ID, "list_training_data", "{}").get();
            NERTrainingResponse response = parseWorkerResponse(result);

            if ("success".equals(response.getStatus()) && response.getData() != null) {
                // Get metadata paths from Python worker response
                java.util.List<Map<String, Object>> trainingDatasets =
                    (java.util.List<Map<String, Object>>) response.getData().get("training_datasets");

                if (trainingDatasets != null && !trainingDatasets.isEmpty()) {
                    // Read each metadata file
                    java.util.List<Map<String, Object>> trainingData = new java.util.ArrayList<>();
                    int totalApprovedSamples = 0;

                    for (Map<String, Object> datasetInfo : trainingDatasets) {
                        String metadataPath = (String) datasetInfo.get("metadata_path");
                        String templateId = (String) datasetInfo.get("template_id");
                        Boolean isFolderFormat = (Boolean) datasetInfo.get("is_folder_format");

                        try {
                            // Read metadata file (small, only counts and dates)
                            String jsonContent = new String(java.nio.file.Files.readAllBytes(
                                java.nio.file.Paths.get(metadataPath)), java.nio.charset.StandardCharsets.UTF_8);

                            com.fasterxml.jackson.databind.JsonNode jsonNode = objectMapper.readTree(jsonContent);

                            int samplesCount = 0;
                            int approvedCount = 0;
                            String createdAt = "";

                            if (Boolean.TRUE.equals(isFolderFormat)) {
                                // New format: metadata.json only has counts
                                samplesCount = jsonNode.has("total_samples") ? jsonNode.get("total_samples").asInt() : 0;
                                approvedCount = jsonNode.has("approved_count") ? jsonNode.get("approved_count").asInt() : 0;
                                createdAt = jsonNode.has("created_at") ? jsonNode.get("created_at").asText() : "";
                            } else {
                                // Old format: full training data file - must count samples
                                if (jsonNode.has("samples") && jsonNode.get("samples").isArray()) {
                                    samplesCount = jsonNode.get("samples").size();

                                    // Count approved samples
                                    for (com.fasterxml.jackson.databind.JsonNode sample : jsonNode.get("samples")) {
                                        if (sample.has("approved") && sample.get("approved").asBoolean()) {
                                            approvedCount++;
                                        }
                                    }
                                }
                                createdAt = jsonNode.has("created_at") ? jsonNode.get("created_at").asText() : "";
                            }

                            totalApprovedSamples += approvedCount;

                            // Add metadata to result
                            Map<String, Object> metadata = new java.util.HashMap<>();
                            metadata.put("template_id", templateId);
                            metadata.put("samples_count", samplesCount);
                            metadata.put("approved_count", approvedCount);
                            metadata.put("created_at", createdAt);

                            trainingData.add(metadata);

                            logger.info("Read training dataset: {} ({} samples, {} approved)",
                                templateId, samplesCount, approvedCount);

                        } catch (Exception e) {
                            logger.warn("Failed to read metadata: {} - {}", metadataPath, e.getMessage());
                        }
                    }

                    // Return processed data
                    Map<String, Object> data = new java.util.HashMap<>();
                    data.put("training_data", trainingData);
                    data.put("count", trainingData.size());
                    data.put("total_approved_samples", totalApprovedSamples);
                    data.put("can_train", totalApprovedSamples >= 50);

                    return new NERTrainingResponse("success", null, data);
                }
            }

            return response;

        } catch (Exception e) {
            logger.error("Failed to list training data", e);
            return NERTrainingResponse.error("Failed to list training data: " + e.getMessage());
        }
    }

    /**
     * Validate NER data
     */
    public NERTrainingResponse validateData(String templateId) {
        try {
            logger.info("Validating NER data for template: {}", templateId);

            Map<String, Object> payload = new HashMap<>();
            payload.put("template_id", templateId);

            String payloadJson = objectMapper.writeValueAsString(payload);

            // Call worker to validate data
            String result = pythonWorkerClient.callWorker(NER_TRAINING_WORKER_ID, "validate_ner_data", payloadJson).get();

            return parseWorkerResponse(result);

        } catch (Exception e) {
            logger.error("Failed to validate NER data", e);
            return NERTrainingResponse.error("Failed to validate NER data: " + e.getMessage());
        }
    }

    /**
     * Export NER format
     */
    public NERTrainingResponse exportNerFormat(String templateId, String format) {
        try {
            logger.info("Exporting NER format for template: {} with format: {}", templateId, format);

            Map<String, Object> payload = new HashMap<>();
            payload.put("template_id", templateId);
            payload.put("format", format);

            String payloadJson = objectMapper.writeValueAsString(payload);

            // Call worker to export
            String result = pythonWorkerClient.callWorker(NER_TRAINING_WORKER_ID, "export_ner_format", payloadJson).get();

            return parseWorkerResponse(result);

        } catch (Exception e) {
            logger.error("Failed to export NER format", e);
            return NERTrainingResponse.error("Failed to export NER format: " + e.getMessage());
        }
    }

    /**
     * Get samples for a specific dataset - Uses SQLite cache to avoid gRPC buffer overflow
     */
    public NERTrainingResponse getDatasetSamples(String templateId, int page, int pageSize) {
        try {
            logger.info("Getting samples for dataset: {} (page: {}, pageSize: {})", templateId, page, pageSize);

            Map<String, Object> payload = new HashMap<>();
            payload.put("template_id", templateId);
            payload.put("page", page);
            payload.put("page_size", pageSize);

            String payloadJson = objectMapper.writeValueAsString(payload);

            // Call worker - Python stores data in SQLite and returns cache ID
            String result = pythonWorkerClient.callWorker(NER_TRAINING_WORKER_ID, "get_dataset_samples", payloadJson).get();
            logger.info("Worker response for get_dataset_samples received ({} bytes)", result.length());

            NERTrainingResponse response = parseWorkerResponse(result);

            // Check if response is cached in SQLite
            if ("success".equals(response.getStatus()) && response.getData() != null) {
                Boolean cached = (Boolean) response.getData().get("cached");

                if (Boolean.TRUE.equals(cached)) {
                    String responseId = (String) response.getData().get("response_id");
                    logger.info("Response cached in SQLite with ID: {}", responseId);

                    // Read actual data from SQLite cache
                    Map<String, Object> cachedData = readFromResponseCache(responseId);

                    if (cachedData != null) {
                        logger.info("Retrieved {} samples from cache",
                            cachedData.get("returned_count"));

                        // Return the actual data
                        return new NERTrainingResponse("success", null, cachedData);
                    } else {
                        logger.error("Failed to retrieve cached response: {}", responseId);
                        return NERTrainingResponse.error("Failed to retrieve cached response");
                    }
                } else {
                    // Direct response (not cached)
                    logger.info("Retrieved {} samples directly",
                        response.getData().get("returned_count"));
                }
            }

            return response;

        } catch (Exception e) {
            logger.error("Failed to get dataset samples", e);
            return NERTrainingResponse.error("Failed to get dataset samples: " + e.getMessage());
        }
    }

    /**
     * Read cached response from SQLite database
     */
    private Map<String, Object> readFromResponseCache(String responseId) {
        try {
            String dbPath = "/tmp/grpc_response_cache.db";
            String url = "jdbc:sqlite:" + dbPath;

            try (java.sql.Connection conn = java.sql.DriverManager.getConnection(url);
                 java.sql.PreparedStatement stmt = conn.prepareStatement(
                     "SELECT data, expires_at FROM response_cache WHERE response_id = ?")) {

                stmt.setString(1, responseId);

                try (java.sql.ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) {
                        String jsonData = rs.getString("data");
                        double expiresAt = rs.getDouble("expires_at");

                        // Check if expired
                        if (System.currentTimeMillis() / 1000.0 > expiresAt) {
                            logger.warn("Cached response expired: {}", responseId);
                            return null;
                        }

                        // Parse JSON data
                        return objectMapper.readValue(jsonData, Map.class);
                    }
                }
            }

            logger.warn("Cached response not found: {}", responseId);
            return null;

        } catch (Exception e) {
            logger.error("Failed to read from response cache: {}", e.getMessage());
            return null;
        }
    }

    /**
     * Bulk delete datasets
     */
    public NERTrainingResponse bulkDeleteDatasets(java.util.List<String> templateIds) {
        try {
            logger.info("Bulk deleting datasets: {}", templateIds);

            Map<String, Object> payload = new HashMap<>();
            payload.put("template_ids", templateIds);

            String payloadJson = objectMapper.writeValueAsString(payload);

            // Call NER training worker via gRPC
            String result = pythonWorkerClient.callWorker(NER_TRAINING_WORKER_ID, "bulk_delete", payloadJson).get();

            return parseWorkerResponse(result);

        } catch (Exception e) {
            logger.error("Failed to bulk delete datasets", e);
            return NERTrainingResponse.error("Failed to bulk delete datasets: " + e.getMessage());
        }
    }

    /**
     * Bulk export datasets
     */
    public NERTrainingResponse bulkExportDatasets(java.util.List<String> templateIds, String format) {
        try {
            logger.info("Bulk exporting datasets: {} with format: {}", templateIds, format);

            Map<String, Object> payload = new HashMap<>();
            payload.put("template_ids", templateIds);
            payload.put("format", format);

            String payloadJson = objectMapper.writeValueAsString(payload);

            // Call NER training worker via gRPC
            String result = pythonWorkerClient.callWorker(NER_TRAINING_WORKER_ID, "bulk_export", payloadJson).get();

            NERTrainingResponse response = parseWorkerResponse(result);

            // Convert content to byte array for file download
            if ("success".equals(response.getStatus()) && response.getData() != null) {
                Object content = response.getData().get("content");
                if (content instanceof String) {
                    response.getData().put("content", ((String) content).getBytes());
                }
            }

            return response;

        } catch (Exception e) {
            logger.error("Failed to bulk export datasets", e);
            return NERTrainingResponse.error("Failed to bulk export datasets: " + e.getMessage());
        }
    }

    /**
     * Bulk delete samples within a dataset
     */
    public NERTrainingResponse bulkDeleteSamples(String templateId, java.util.List<String> sampleIds) {
        try {
            logger.info("Bulk deleting samples for dataset {}: {}", templateId, sampleIds);

            Map<String, Object> payload = new HashMap<>();
            payload.put("template_id", templateId);
            payload.put("sample_ids", sampleIds);

            String payloadJson = objectMapper.writeValueAsString(payload);

            // Call NER training worker via gRPC
            String result = pythonWorkerClient.callWorker(NER_TRAINING_WORKER_ID, "bulk_delete_samples", payloadJson).get();
            logger.info("Worker response for bulk_delete_samples: {}", result);

            return parseWorkerResponse(result);

        } catch (Exception e) {
            logger.error("Failed to bulk delete samples", e);
            return NERTrainingResponse.error("Failed to bulk delete samples: " + e.getMessage());
        }
    }

    /**
     * Start NER model training
     */
    public NERTrainingResponse startTraining(Map<String, Object> request) {
        try {
            logger.info("Starting NER model training");

            String payloadJson = objectMapper.writeValueAsString(request);

            // Call NER training worker via gRPC
            String result = pythonWorkerClient.callWorker(NER_TRAINING_WORKER_ID, "start_training", payloadJson).get();
            logger.info("Worker response for start_training: {}", result);

            return parseWorkerResponse(result);

        } catch (Exception e) {
            logger.error("Failed to start training", e);
            return NERTrainingResponse.error("Failed to start training: " + e.getMessage());
        }
    }

    /**
     * Get training status
     */
    public NERTrainingResponse getTrainingStatus(String trainingId) {
        try {
            logger.info("Getting training status for: {}", trainingId);

            Map<String, Object> payload = new HashMap<>();
            payload.put("training_id", trainingId);

            String payloadJson = objectMapper.writeValueAsString(payload);

            // Call NER training worker via gRPC
            String result = pythonWorkerClient.callWorker(NER_TRAINING_WORKER_ID, "get_training_status", payloadJson).get();
            logger.info("Worker response for get_training_status: {}", result);

            return parseWorkerResponse(result);

        } catch (Exception e) {
            logger.error("Failed to get training status", e);
            return NERTrainingResponse.error("Failed to get training status: " + e.getMessage());
        }
    }

    /**
     * List trained models
     */
    public NERTrainingResponse listTrainedModels() {
        try {
            logger.info("Listing trained NER models");

            // Call NER training worker via gRPC
            String result = pythonWorkerClient.callWorker(NER_TRAINING_WORKER_ID, "list_trained_models", "{}").get();
            logger.info("Worker response for list_trained_models: {}", result);

            return parseWorkerResponse(result);

        } catch (Exception e) {
            logger.error("Failed to list trained models", e);
            return NERTrainingResponse.error("Failed to list trained models: " + e.getMessage());
        }
    }

    /**
     * Predict NER entities
     */
    public NERTrainingResponse predictNER(Map<String, Object> request) {
        try {
            logger.info("Predicting NER entities");

            String payloadJson = objectMapper.writeValueAsString(request);

            // Call NER training worker via gRPC
            String result = pythonWorkerClient.callWorker(NER_TRAINING_WORKER_ID, "predict_ner", payloadJson).get();
            logger.info("Worker response for predict_ner: {}", result);

            return parseWorkerResponse(result);

        } catch (Exception e) {
            logger.error("Failed to predict NER", e);
            return NERTrainingResponse.error("Failed to predict NER: " + e.getMessage());
        }
    }

    /**
     * Evaluate model
     */
    public NERTrainingResponse evaluateModel(Map<String, Object> request) {
        try {
            logger.info("Evaluating NER model");

            String payloadJson = objectMapper.writeValueAsString(request);

            // Call NER training worker via gRPC
            String result = pythonWorkerClient.callWorker(NER_TRAINING_WORKER_ID, "evaluate_model", payloadJson).get();
            logger.info("Worker response for evaluate_model: {}", result);

            return parseWorkerResponse(result);

        } catch (Exception e) {
            logger.error("Failed to evaluate model", e);
            return NERTrainingResponse.error("Failed to evaluate model: " + e.getMessage());
        }
    }

    /**
     * Parse worker response
     */
    private NERTrainingResponse parseWorkerResponse(String result) {
        try {
            logger.debug("Parsing NER training worker response: {}", result);
            
            // Use JsonNode for more flexible parsing like RAGOcrService
            com.fasterxml.jackson.databind.JsonNode jsonNode = objectMapper.readTree(result);

            // Check for error first
            if (jsonNode.has("error")) {
                return NERTrainingResponse.error(jsonNode.get("error").asText());
            }

            // Extract status
            String status = jsonNode.has("status") ? jsonNode.get("status").asText() : "success";
            
            // Extract data
            com.fasterxml.jackson.databind.JsonNode dataNode = jsonNode.has("data") ? jsonNode.get("data") : jsonNode;
            
            String message = null;
            Map<String, Object> data = null;

            if ("error".equals(status)) {
                // For error responses, data contains the error message
                if (dataNode.isTextual()) {
                    message = dataNode.asText();
                } else {
                    message = dataNode.toString();
                }
            } else {
                // For success responses, convert data to map
                if (dataNode.isObject()) {
                    data = objectMapper.convertValue(dataNode, Map.class);
                    // Check if there's a message in the data map
                    if (data.containsKey("message")) {
                        message = (String) data.get("message");
                    }
                }
            }

            return new NERTrainingResponse(status, message, data);

        } catch (Exception e) {
            logger.error("Failed to parse worker response: {}", result, e);
            return NERTrainingResponse.error("Failed to parse worker response: " + e.getMessage());
        }
    }
}