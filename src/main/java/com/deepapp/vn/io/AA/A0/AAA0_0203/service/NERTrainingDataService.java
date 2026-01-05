package com.deepapp.vn.io.AA.A0.AAA0_0203.service;

import com.deepapp.vn.io.AA.A0.AAA0_0203.entity.NERAnnotation;
import com.deepapp.vn.io.AA.A0.AAA0_0203.entity.NERSample;
import com.deepapp.vn.io.AA.A0.AAA0_0203.entity.NERTrainingDataset;
import com.deepapp.vn.io.AA.A0.AAA0_0203.entity.NEREntityType;
import com.deepapp.vn.io.AA.A0.AAA0_0203.model.NERTrainingRequest;
import com.deepapp.vn.io.AA.A0.AAA0_0203.model.NERTrainingResponse;
import com.deepapp.vn.io.AA.A0.AAA0_0203.repository.NERAnnotationRepository;
import com.deepapp.vn.io.AA.A0.AAA0_0203.repository.NERSampleRepository;
import com.deepapp.vn.io.AA.A0.AAA0_0203.repository.NERTrainingDatasetRepository;
import com.deepapp.vn.io.AA.A0.AAA0_0203.repository.NEREntityTypeRepository;
import com.deepapp.vn.io.workers.PythonWorkerClient;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

/**
 * NER Training Data Service - Database-first approach
 * 
 * Architecture:
 * - Java handles all data storage and retrieval (H2 database)
 * - Python worker only handles AI/ML tasks (training, prediction)
 * - Uses Spring Cache for performance
 */
@Service
@Transactional
public class NERTrainingDataService {

    private static final Logger logger = LoggerFactory.getLogger(NERTrainingDataService.class);
    private static final String NER_TRAINING_WORKER_ID = "AAA0_0203_W";

    @Autowired
    private NERTrainingDatasetRepository datasetRepository;

    @Autowired
    private NERSampleRepository sampleRepository;

    @Autowired
    private NERAnnotationRepository annotationRepository;

    @Autowired
    private NEREntityTypeRepository entityTypeRepository;

    @Autowired
    private PythonWorkerClient pythonWorkerClient;

    private final ObjectMapper objectMapper = new ObjectMapper();

    // ==================== Dataset Operations ====================

    /**
     * List all training datasets
     */
    @Cacheable(value = "trainingDatasets", unless = "#result == null")
    public NERTrainingResponse listTrainingData() {
        try {
            logger.info("Listing training datasets from database");

            List<NERTrainingDataset> datasets = datasetRepository.findByActiveTrue();
            Long totalApprovedSamples = datasetRepository.countTotalApprovedSamples();

            List<Map<String, Object>> trainingData = datasets.stream().map(dataset -> {
                Map<String, Object> data = new HashMap<>();
                data.put("template_id", dataset.getTemplateId());
                data.put("filename", dataset.getFilename());
                data.put("samples_count", dataset.getTotalSamples());
                data.put("approved_count", dataset.getApprovedCount());
                data.put("created_at", dataset.getCreatedAt().toString());
                data.put("filepath", "database://" + dataset.getTemplateId());
                return data;
            }).collect(Collectors.toList());

            Map<String, Object> responseData = new HashMap<>();
            responseData.put("training_data", trainingData);
            responseData.put("count", trainingData.size());
            responseData.put("total_approved_samples", totalApprovedSamples != null ? totalApprovedSamples : 0);
            responseData.put("can_train", totalApprovedSamples != null && totalApprovedSamples >= 50);

            logger.info("Listed {} datasets with {} total approved samples", trainingData.size(), totalApprovedSamples);

            return NERTrainingResponse.success(null, responseData);

        } catch (Exception e) {
            logger.error("Failed to list training data", e);
            return NERTrainingResponse.error("Failed to list training data: " + e.getMessage());
        }
    }

    /**
     * Get or create dataset
     */
    @CacheEvict(value = "trainingDatasets", allEntries = true)
    public NERTrainingDataset getOrCreateDataset(String templateId) {
        return datasetRepository.findByTemplateId(templateId)
            .orElseGet(() -> {
                NERTrainingDataset dataset = new NERTrainingDataset(templateId);
                return datasetRepository.save(dataset);
            });
    }

    /**
     * Delete dataset and all its samples
     */
    @CacheEvict(value = {"trainingDatasets", "datasetSamples"}, allEntries = true)
    public NERTrainingResponse deleteDataset(String templateId) {
        try {
            NERTrainingDataset dataset = datasetRepository.findByTemplateId(templateId)
                .orElseThrow(() -> new RuntimeException("Dataset not found: " + templateId));

            // Delete all samples (annotations will be cascaded)
            sampleRepository.deleteByDataset(dataset);
            
            // Delete dataset
            datasetRepository.delete(dataset);

            logger.info("Deleted dataset: {}", templateId);

            return NERTrainingResponse.success("Dataset deleted successfully");

        } catch (Exception e) {
            logger.error("Failed to delete dataset", e);
            return NERTrainingResponse.error("Failed to delete dataset: " + e.getMessage());
        }
    }

    // ==================== Sample Operations ====================

    /**
     * Get samples for a dataset with pagination
     */
    @Cacheable(value = "datasetSamples", key = "#templateId + '_' + #page + '_' + #pageSize", unless = "#result == null")
    public NERTrainingResponse getDatasetSamples(String templateId, int page, int pageSize) {
        try {
            logger.info("Getting samples for dataset: {} (page={}, size={})", templateId, page, pageSize);

            NERTrainingDataset dataset = datasetRepository.findByTemplateId(templateId)
                .orElseThrow(() -> new RuntimeException("Dataset not found: " + templateId));

            Pageable pageable = PageRequest.of(page, pageSize, Sort.by("createdAt").descending());
            Page<NERSample> samplesPage = sampleRepository.findByDataset(dataset, pageable);

            List<Map<String, Object>> samples = samplesPage.getContent().stream().map(sample -> {
                Map<String, Object> sampleData = new HashMap<>();
                sampleData.put("id", sample.getSampleId());
                sampleData.put("text", sample.getText());
                sampleData.put("approved", sample.getApproved());
                sampleData.put("created_at", sample.getCreatedAt().toString());
                
                // Convert annotations
                List<Map<String, Object>> annotations = sample.getAnnotations().stream().map(ann -> {
                    Map<String, Object> annData = new HashMap<>();
                    annData.put("start", ann.getStartPos());
                    annData.put("end", ann.getEndPos());
                    annData.put("text", ann.getText());
                    annData.put("label", ann.getEntityType());
                    return annData;
                }).collect(Collectors.toList());
                
                sampleData.put("annotations", annotations);
                return sampleData;
            }).collect(Collectors.toList());

            Map<String, Object> responseData = new HashMap<>();
            responseData.put("template_id", templateId);
            responseData.put("samples", samples);
            responseData.put("total_samples", samplesPage.getTotalElements());
            responseData.put("page", page);
            responseData.put("page_size", pageSize);
            responseData.put("total_pages", samplesPage.getTotalPages());
            responseData.put("has_more", samplesPage.hasNext());

            return NERTrainingResponse.success(null, responseData);

        } catch (Exception e) {
            logger.error("Failed to get dataset samples", e);
            return NERTrainingResponse.error("Failed to get samples: " + e.getMessage());
        }
    }

    /**
     * Add new sample to dataset
     */
    @CacheEvict(value = {"trainingDatasets", "datasetSamples"}, allEntries = true)
    public NERTrainingResponse addSample(String templateId, String text, List<Map<String, Object>> annotations) {
        try {
            NERTrainingDataset dataset = getOrCreateDataset(templateId);

            // Create sample
            String sampleId = UUID.randomUUID().toString();
            NERSample sample = new NERSample(sampleId, dataset, text);
            
            // Auto-approve if has annotations
            if (annotations != null && !annotations.isEmpty()) {
                sample.setApproved(true);
                dataset.incrementApprovedCount();
            }

            // Add annotations
            if (annotations != null) {
                for (Map<String, Object> annData : annotations) {
                    Integer start = (Integer) annData.get("start");
                    Integer end = (Integer) annData.get("end");
                    String annText = (String) annData.get("text");
                    String label = (String) annData.get("label");

                    NERAnnotation annotation = new NERAnnotation(sample, start, end, annText, label);
                    sample.addAnnotation(annotation);
                }
            }

            // Save
            sampleRepository.save(sample);
            dataset.incrementTotalSamples();
            datasetRepository.save(dataset);

            logger.info("Added sample to dataset: {}", templateId);

            return NERTrainingResponse.success("Sample added successfully");

        } catch (Exception e) {
            logger.error("Failed to add sample", e);
            return NERTrainingResponse.error("Failed to add sample: " + e.getMessage());
        }
    }

    /**
     * Update sample annotations
     */
    @Transactional
    @CacheEvict(value = "datasetSamples", allEntries = true)
    public NERTrainingResponse updateSampleAnnotations(String templateId, String sampleId, 
                                                        String text, List<Map<String, Object>> annotations) {
        try {
            if (sampleId == null || sampleId.trim().isEmpty()) {
                return NERTrainingResponse.error("Sample ID is required");
            }
            
            NERSample sample = sampleRepository.findBySampleId(sampleId)
                .orElseThrow(() -> new RuntimeException("Sample not found: " + sampleId));

            // Verify sample belongs to the specified dataset
            if (templateId != null && !sample.getDataset().getTemplateId().equals(templateId)) {
                return NERTrainingResponse.error("Sample does not belong to dataset: " + templateId);
            }

            // Update text if changed
            if (text != null && !text.equals(sample.getText())) {
                sample.setText(text);
            }

            // Clear and rebuild annotations
            sample.clearAnnotations();
            
            if (annotations != null && !annotations.isEmpty()) {
                for (Map<String, Object> annData : annotations) {
                    try {
                        Integer start = (Integer) annData.get("start");
                        Integer end = (Integer) annData.get("end");
                        String annText = (String) annData.get("text");
                        String label = (String) annData.get("label");
                        
                        if (start == null || end == null || annText == null || label == null) {
                            logger.warn("Skipping invalid annotation: missing required fields");
                            continue;
                        }

                        NERAnnotation annotation = new NERAnnotation(sample, start, end, annText, label);
                        sample.addAnnotation(annotation);
                    } catch (Exception e) {
                        logger.warn("Failed to add annotation: {}", e.getMessage());
                    }
                }
            }

            sampleRepository.save(sample);

            logger.info("Updated annotations for sample {}: {} annotations", sampleId, sample.getAnnotations().size());

            Map<String, Object> result = new HashMap<>();
            result.put("sample_id", sampleId);
            result.put("annotation_count", sample.getAnnotations().size());
            result.put("text_length", sample.getText().length());

            return NERTrainingResponse.success("Annotations updated successfully", result);

        } catch (Exception e) {
            logger.error("Failed to update annotations", e);
            return NERTrainingResponse.error("Failed to update annotations: " + e.getMessage());
        }
    }

    /**
     * Approve/reject sample
     */
    @Transactional
    @CacheEvict(value = {"trainingDatasets", "datasetSamples"}, allEntries = true)
    public NERTrainingResponse setSampleApproval(String sampleId, boolean approved) {
        try {
            if (sampleId == null || sampleId.trim().isEmpty()) {
                return NERTrainingResponse.error("Sample ID is required");
            }
            
            NERSample sample = sampleRepository.findBySampleId(sampleId)
                .orElseThrow(() -> new RuntimeException("Sample not found: " + sampleId));

            boolean wasApproved = sample.getApproved();
            
            // No change needed
            if (wasApproved == approved) {
                logger.debug("Sample {} approval status unchanged: {}", sampleId, approved);
                Map<String, Object> result = new HashMap<>();
                result.put("sample_id", sampleId);
                result.put("approved", approved);
                result.put("changed", false);
                return NERTrainingResponse.success("Approval status unchanged", result);
            }
            
            sample.setApproved(approved);
            sampleRepository.save(sample);

            // Update dataset counts
            NERTrainingDataset dataset = sample.getDataset();
            if (dataset != null) {
                if (approved && !wasApproved) {
                    dataset.incrementApprovedCount();
                } else if (!approved && wasApproved) {
                    dataset.decrementApprovedCount();
                }
                datasetRepository.save(dataset);
            } else {
                logger.warn("Sample {} has no associated dataset", sampleId);
            }

            logger.info("Sample {} approval changed from {} to {}", sampleId, wasApproved, approved);

            Map<String, Object> result = new HashMap<>();
            result.put("sample_id", sampleId);
            result.put("approved", approved);
            result.put("changed", true);
            result.put("previous_status", wasApproved);
            if (dataset != null) {
                result.put("dataset_approved_count", dataset.getApprovedCount());
                result.put("dataset_total_samples", dataset.getTotalSamples());
            }

            return NERTrainingResponse.success(approved ? "Sample approved" : "Sample rejected", result);

        } catch (Exception e) {
            logger.error("Failed to set sample approval", e);
            return NERTrainingResponse.error("Failed to update approval: " + e.getMessage());
        }
    }

    /**
     * Bulk approve/reject samples in a single transaction (avoids SQLite locking issues)
     */
    @Transactional
    @CacheEvict(value = {"trainingDatasets", "datasetSamples"}, allEntries = true)
    public NERTrainingResponse bulkApproveSamples(List<String> sampleIds, boolean approved) {
        logger.info("Bulk {} {} samples", approved ? "approving" : "rejecting", sampleIds.size());
        
        if (sampleIds == null || sampleIds.isEmpty()) {
            return NERTrainingResponse.error("No sample IDs provided");
        }

        try {
            int successCount = 0;
            int unchangedCount = 0;
            int notFoundCount = 0;
            
            for (String sampleId : sampleIds) {
                Optional<NERSample> sampleOpt = sampleRepository.findBySampleId(sampleId);
                
                if (!sampleOpt.isPresent()) {
                    notFoundCount++;
                    logger.warn("Sample not found: {}", sampleId);
                    continue;
                }
                
                NERSample sample = sampleOpt.get();
                boolean wasApproved = Boolean.TRUE.equals(sample.getApproved());
                
                if (wasApproved == approved) {
                    unchangedCount++;
                    continue;
                }
                
                // Update sample
                sample.setApproved(approved);
                sampleRepository.save(sample);
                
                // Update dataset counts
                NERTrainingDataset dataset = sample.getDataset();
                if (dataset != null) {
                    if (approved && !wasApproved) {
                        dataset.incrementApprovedCount();
                    } else if (!approved && wasApproved) {
                        dataset.decrementApprovedCount();
                    }
                    datasetRepository.save(dataset);
                }
                
                successCount++;
            }
            
            Map<String, Object> result = new HashMap<>();
            result.put("requested_count", sampleIds.size());
            result.put("success_count", successCount);
            result.put("unchanged_count", unchangedCount);
            result.put("not_found_count", notFoundCount);
            result.put("approved", approved);
            
            String message = String.format(
                "Bulk %s: %d succeeded, %d unchanged, %d not found out of %d requested",
                approved ? "approval" : "rejection",
                successCount, unchangedCount, notFoundCount, sampleIds.size()
            );
            
            logger.info(message);
            return NERTrainingResponse.success(message, result);
            
        } catch (Exception e) {
            logger.error("Failed to bulk approve samples", e);
            return NERTrainingResponse.error("Failed to bulk approve: " + e.getMessage());
        }
    }

    /**
     * Delete samples
     */
    @Transactional
    @CacheEvict(value = {"trainingDatasets", "datasetSamples"}, allEntries = true)
    public NERTrainingResponse deleteSamples(String templateId, List<String> sampleIds) {
        try {
            NERTrainingDataset dataset = datasetRepository.findByTemplateId(templateId)
                .orElseThrow(() -> new RuntimeException("Dataset not found: " + templateId));

            if (sampleIds == null || sampleIds.isEmpty()) {
                return NERTrainingResponse.error("No sample IDs provided");
            }

            // Count existing samples before delete
            long existingCount = sampleIds.stream()
                .map(sampleRepository::findBySampleId)
                .filter(Optional::isPresent)
                .count();

            // Delete samples
            sampleRepository.deleteBySampleIdIn(sampleIds);

            // Recalculate counts from database to ensure accuracy
            long totalSamples = sampleRepository.countByDataset(dataset);
            long approvedCount = sampleRepository.countByDatasetAndApprovedTrue(dataset);
            
            dataset.setTotalSamples((int) totalSamples);
            dataset.setApprovedCount((int) approvedCount);
            datasetRepository.save(dataset);

            logger.info("Deleted {} samples (requested: {}) from dataset: {}", existingCount, sampleIds.size(), templateId);

            Map<String, Object> result = new HashMap<>();
            result.put("deleted_count", existingCount);
            result.put("requested_count", sampleIds.size());
            result.put("remaining_samples", totalSamples);
            result.put("remaining_approved", approvedCount);

            return NERTrainingResponse.success("Samples deleted successfully", result);

        } catch (Exception e) {
            logger.error("Failed to delete samples", e);
            return NERTrainingResponse.error("Failed to delete samples: " + e.getMessage());
        }
    }

    // ==================== Python Worker Integration (AI Tasks Only) ====================

    /**
     * Start NER model training (Python worker)
     */
    public NERTrainingResponse startTraining(Map<String, Object> config) {
        try {
            logger.info("Starting NER training with config: {}", config);

            // Get training data from database
            String templateId = (String) config.get("template_id");
            if (templateId != null) {
                NERTrainingDataset dataset = datasetRepository.findByTemplateId(templateId)
                    .orElseThrow(() -> new RuntimeException("Dataset not found: " + templateId));

                List<NERSample> approvedSamples = sampleRepository.findByDatasetAndApprovedTrue(dataset);
                
                // Convert to Python format
                List<Map<String, Object>> trainingSamples = approvedSamples.stream().map(sample -> {
                    Map<String, Object> sampleData = new HashMap<>();
                    sampleData.put("text", sample.getText());
                    sampleData.put("annotations", sample.getAnnotations().stream().map(ann -> {
                        Map<String, Object> annData = new HashMap<>();
                        annData.put("start", ann.getStartPos());
                        annData.put("end", ann.getEndPos());
                        annData.put("text", ann.getText());
                        annData.put("label", ann.getEntityType());
                        return annData;
                    }).collect(Collectors.toList()));
                    return sampleData;
                }).collect(Collectors.toList());

                config.put("training_samples", trainingSamples);
            }

            String payloadJson = objectMapper.writeValueAsString(config);

            // Call Python worker for training
            String result = pythonWorkerClient.callWorker(NER_TRAINING_WORKER_ID, "start_training", payloadJson).get();

            return parseWorkerResponse(result);

        } catch (Exception e) {
            logger.error("Failed to start training", e);
            return NERTrainingResponse.error("Failed to start training: " + e.getMessage());
        }
    }

    /**
     * Predict NER entities (Python worker)
     */
    public NERTrainingResponse predictNER(Map<String, Object> request) {
        try {
            logger.info("Predicting NER entities");

            String payloadJson = objectMapper.writeValueAsString(request);
            String result = pythonWorkerClient.callWorker(NER_TRAINING_WORKER_ID, "predict_ner", payloadJson).get();

            return parseWorkerResponse(result);

        } catch (Exception e) {
            logger.error("Failed to predict NER", e);
            return NERTrainingResponse.error("Failed to predict NER: " + e.getMessage());
        }
    }

    /**
     * Evaluate model (Python worker)
     */
    public NERTrainingResponse evaluateModel(Map<String, Object> request) {
        try {
            logger.info("Evaluating NER model");

            String payloadJson = objectMapper.writeValueAsString(request);
            String result = pythonWorkerClient.callWorker(NER_TRAINING_WORKER_ID, "evaluate_model", payloadJson).get();

            return parseWorkerResponse(result);

        } catch (Exception e) {
            logger.error("Failed to evaluate model", e);
            return NERTrainingResponse.error("Failed to evaluate model: " + e.getMessage());
        }
    }

    /**
     * List trained models (Python worker)
     */
    @Cacheable(value = "trainedModels", unless = "#result == null")
    public NERTrainingResponse listTrainedModels() {
        try {
            logger.info("Listing trained NER models");

            String result = pythonWorkerClient.callWorker(NER_TRAINING_WORKER_ID, "list_trained_models", "{}").get();
            return parseWorkerResponse(result);

        } catch (Exception e) {
            logger.error("Failed to list trained models", e);
            return NERTrainingResponse.error("Failed to list trained models: " + e.getMessage());
        }
    }

    // ==================== Helper Methods ====================

    private NERTrainingResponse parseWorkerResponse(String result) {
        try {
            com.fasterxml.jackson.databind.JsonNode jsonNode = objectMapper.readTree(result);

            if (jsonNode.has("error")) {
                return NERTrainingResponse.error(jsonNode.get("error").asText());
            }

            String status = jsonNode.has("status") ? jsonNode.get("status").asText() : "success";
            com.fasterxml.jackson.databind.JsonNode dataNode = jsonNode.has("data") ? jsonNode.get("data") : jsonNode;

            String message = null;
            Map<String, Object> data = null;

            if ("error".equals(status)) {
                message = dataNode.isTextual() ? dataNode.asText() : dataNode.toString();
            } else {
                if (dataNode.isObject()) {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> dataMap = objectMapper.convertValue(dataNode, Map.class);
                    data = dataMap;
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

    /**
     * Process generic request (delegates to Python worker for now)
     * TODO: Gradually migrate specific operations to database-first approach
     */
    @CacheEvict(value = {"trainingDatasets", "datasetSamples"}, allEntries = true, 
                condition = "#request.eventType == 'add_training_samples' or #request.eventType == 'approve_sample' or #request.eventType == 'reject_sample'")
    public NERTrainingResponse processRequest(NERTrainingRequest request) {
        logger.info("Processing generic request: {}", request.getEventType());
        
        String eventType = request.getEventType();
        Map<String, Object> payload = request.getPayload();
        
        // Handle database-first operations directly in Java (no Python worker needed)
        if ("add_training_samples".equals(eventType)) {
            return addTrainingSamples(payload);
        }
        
        if ("approve_sample".equals(eventType)) {
            String sampleId = (String) payload.get("sample_id");
            if (sampleId == null) {
                return NERTrainingResponse.error("Missing sample_id");
            }
            return setSampleApproval(sampleId, true);
        }
        
        if ("reject_sample".equals(eventType)) {
            String sampleId = (String) payload.get("sample_id");
            if (sampleId == null) {
                return NERTrainingResponse.error("Missing sample_id");
            }
            return setSampleApproval(sampleId, false);
        }
        
        // Delegate AI operations to Python worker
        try {
            // Serialize request payload to JSON
            String payloadJson = objectMapper.writeValueAsString(payload);
            
            // Call Python worker
            String result = pythonWorkerClient.callWorker(
                "AAA0_0203_W",
                request.getEventType(),
                payloadJson
            ).get(); // Block and wait for response
            
            return parseWorkerResponse(result);
        } catch (Exception e) {
            logger.error("Error processing request", e);
            return NERTrainingResponse.error("Failed to process request: " + e.getMessage());
        }
    }

    /**
     * Add training samples from AAA0_0202 or other sources
     * Handles batch import of samples with annotations
     */
    @Transactional
    @CacheEvict(value = {"trainingDatasets", "datasetSamples"}, allEntries = true)
    public NERTrainingResponse addTrainingSamples(Map<String, Object> payload) {
        logger.info("Adding training samples from external source");
        
        try {
            String templateId = (String) payload.get("template_id");
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> samplesData = (List<Map<String, Object>>) payload.get("samples");
            String source = (String) payload.getOrDefault("source", "unknown");
            
            if (templateId == null || samplesData == null) {
                return NERTrainingResponse.error("template_id and samples are required");
            }
            
            // Get or create dataset
            NERTrainingDataset dataset = getOrCreateDataset(templateId);
            
            int addedCount = 0;
            int skippedCount = 0;
            List<String> errors = new ArrayList<>();
            
            for (Map<String, Object> sampleData : samplesData) {
                try {
                    String sampleId = (String) sampleData.get("id");
                    String text = (String) sampleData.get("text");
                    Boolean approved = (Boolean) sampleData.getOrDefault("approved", false);
                    
                    @SuppressWarnings("unchecked")
                    List<Map<String, Object>> annotationsData = 
                        (List<Map<String, Object>>) sampleData.get("annotations");
                    
                    if (text == null || text.trim().isEmpty()) {
                        skippedCount++;
                        continue;
                    }
                    
                    // Check if sample already exists
                    if (sampleRepository.findBySampleId(sampleId).isPresent()) {
                        logger.debug("Sample {} already exists, skipping", sampleId);
                        skippedCount++;
                        continue;
                    }
                    
                    // Create new sample
                    NERSample sample = new NERSample(sampleId, dataset, text);
                    sample.setApproved(approved);
                    
                    // Add annotations
                    if (annotationsData != null) {
                        for (Map<String, Object> annData : annotationsData) {
                            Integer start = (Integer) annData.get("start");
                            Integer end = (Integer) annData.get("end");
                            String annText = (String) annData.get("text");
                            String label = (String) annData.get("label");
                            
                            if (start != null && end != null && annText != null && label != null) {
                                NERAnnotation annotation = new NERAnnotation(sample, start, end, annText, label);
                                sample.addAnnotation(annotation);
                            }
                        }
                    }
                    
                    sampleRepository.save(sample);
                    dataset.incrementTotalSamples();
                    if (approved) {
                        dataset.incrementApprovedCount();
                    }
                    addedCount++;
                    
                } catch (Exception e) {
                    logger.error("Error adding sample: {}", e.getMessage());
                    errors.add(e.getMessage());
                    skippedCount++;
                }
            }
            
            // Save dataset with updated counts
            datasetRepository.save(dataset);
            
            Map<String, Object> result = new HashMap<>();
            result.put("template_id", templateId);
            result.put("added_count", addedCount);
            result.put("skipped_count", skippedCount);
            result.put("total_samples", dataset.getTotalSamples());
            result.put("source", source);
            
            if (!errors.isEmpty()) {
                result.put("errors", errors);
            }
            
            logger.info("Added {} samples to dataset {} (skipped: {})", addedCount, templateId, skippedCount);
            
            return NERTrainingResponse.success("Successfully added training samples", result);
            
        } catch (Exception e) {
            logger.error("Error adding training samples", e);
            return NERTrainingResponse.error("Failed to add training samples: " + e.getMessage());
        }
    }

    /**
     * Validate training data quality
     */
    public NERTrainingResponse validateData(String templateId) {
        logger.info("Validating data for template: {}", templateId);
        
        try {
            NERTrainingDataset dataset = datasetRepository.findByTemplateId(templateId)
                .orElseThrow(() -> new RuntimeException("Dataset not found: " + templateId));
            
            // Get statistics from database
            long totalSamples = sampleRepository.countByDataset(dataset);
            long approvedSamples = sampleRepository.countByDatasetAndApprovedTrue(dataset);
            
            Map<String, Object> validation = new HashMap<>();
            validation.put("template_id", templateId);
            validation.put("total_samples", totalSamples);
            validation.put("approved_samples", approvedSamples);
            validation.put("approval_rate", totalSamples > 0 ? (double) approvedSamples / totalSamples : 0.0);
            
            // Get entity type statistics
            List<Object[]> entityStats = annotationRepository.countByEntityTypeForDataset(templateId);
            Map<String, Long> entityCounts = new HashMap<>();
            for (Object[] stat : entityStats) {
                entityCounts.put((String) stat[0], (Long) stat[1]);
            }
            validation.put("entity_type_counts", entityCounts);
            
            // Validation checks
            List<String> issues = new ArrayList<>();
            if (totalSamples < 10) {
                issues.add("Insufficient samples (minimum 10 required)");
            }
            if (approvedSamples < 5) {
                issues.add("Insufficient approved samples (minimum 5 required)");
            }
            if (entityCounts.isEmpty()) {
                issues.add("No entity annotations found");
            }
            
            validation.put("issues", issues);
            validation.put("is_valid", issues.isEmpty());
            
            return NERTrainingResponse.success("Data validation completed", validation);
            
        } catch (Exception e) {
            logger.error("Error validating data", e);
            return NERTrainingResponse.error("Failed to validate data: " + e.getMessage());
        }
    }

    /**
     * Export training data in NER format
     */
    public NERTrainingResponse exportNerFormat(String templateId, String format) {
        logger.info("Exporting NER format for template {} in format {}", templateId, format);
        
        try {
            NERTrainingDataset dataset = datasetRepository.findByTemplateId(templateId)
                .orElseThrow(() -> new RuntimeException("Dataset not found: " + templateId));
            
            // Get all approved samples with annotations
            List<NERSample> samples = sampleRepository.findByDatasetAndApprovedTrue(dataset);
            
            List<Map<String, Object>> exportData = new ArrayList<>();
            for (NERSample sample : samples) {
                Map<String, Object> sampleData = new HashMap<>();
                sampleData.put("text", sample.getText());
                
                List<Map<String, Object>> entities = new ArrayList<>();
                for (NERAnnotation annotation : sample.getAnnotations()) {
                    Map<String, Object> entity = new HashMap<>();
                    entity.put("start", annotation.getStartPos());
                    entity.put("end", annotation.getEndPos());
                    entity.put("text", annotation.getText());
                    entity.put("entity_type", annotation.getEntityType());
                    entities.add(entity);
                }
                sampleData.put("entities", entities);
                exportData.add(sampleData);
            }
            
            Map<String, Object> result = new HashMap<>();
            result.put("template_id", templateId);
            result.put("format", format);
            result.put("total_samples", exportData.size());
            result.put("data", exportData);
            
            return NERTrainingResponse.success("Export completed", result);
            
        } catch (Exception e) {
            logger.error("Error exporting NER format", e);
            return NERTrainingResponse.error("Failed to export NER format: " + e.getMessage());
        }
    }

    /**
     * Bulk delete datasets from database
     */
    @Transactional
    @CacheEvict(value = "trainingDatasets", allEntries = true)
    public NERTrainingResponse bulkDeleteDatasets(List<String> templateIds) {
        logger.info("Bulk deleting datasets: {}", templateIds);
        
        try {
            if (templateIds == null || templateIds.isEmpty()) {
                return NERTrainingResponse.error("No template IDs provided");
            }
            
            int deletedCount = 0;
            int skippedCount = 0;
            List<String> notFound = new ArrayList<>();
            List<String> failed = new ArrayList<>();
            
            for (String templateId : templateIds) {
                try {
                    // Check if exists first
                    if (datasetRepository.findByTemplateId(templateId).isEmpty()) {
                        logger.debug("Dataset not found: {}", templateId);
                        notFound.add(templateId);
                        skippedCount++;
                        continue;
                    }
                    
                    // Delete (cascade will delete samples and annotations)
                    datasetRepository.deleteByTemplateId(templateId);
                    deletedCount++;
                    logger.debug("Deleted dataset: {}", templateId);
                    
                } catch (Exception e) {
                    logger.warn("Failed to delete dataset {}: {}", templateId, e.getMessage());
                    failed.add(templateId + ": " + e.getMessage());
                    skippedCount++;
                }
            }
            
            Map<String, Object> result = new HashMap<>();
            result.put("deleted_count", deletedCount);
            result.put("skipped_count", skippedCount);
            result.put("total_requested", templateIds.size());
            
            if (!notFound.isEmpty()) {
                result.put("not_found", notFound);
            }
            if (!failed.isEmpty()) {
                result.put("failed", failed);
            }
            
            logger.info("Bulk delete completed: {} deleted, {} skipped out of {} requested", 
                       deletedCount, skippedCount, templateIds.size());
            
            return NERTrainingResponse.success("Bulk delete completed", result);
            
        } catch (Exception e) {
            logger.error("Error bulk deleting datasets", e);
            return NERTrainingResponse.error("Failed to bulk delete datasets: " + e.getMessage());
        }
    }

    /**
     * Bulk delete samples from database
     */
    @Transactional
    @CacheEvict(value = {"trainingDatasets", "datasetSamples"}, allEntries = true)
    public NERTrainingResponse bulkDeleteSamples(String templateId, List<String> sampleIds) {
        logger.info("Bulk deleting samples for template {}: {}", templateId, sampleIds);
        
        try {
            NERTrainingDataset dataset = datasetRepository.findByTemplateId(templateId)
                .orElseThrow(() -> new RuntimeException("Dataset not found: " + templateId));
            
            if (sampleIds == null || sampleIds.isEmpty()) {
                return NERTrainingResponse.error("No sample IDs provided");
            }
            
            // Count existing samples before delete
            long beforeCount = sampleRepository.countByDataset(dataset);
            long existingCount = sampleIds.stream()
                .map(sampleRepository::findBySampleId)
                .filter(Optional::isPresent)
                .count();
            
            // Delete samples
            sampleRepository.deleteBySampleIdIn(sampleIds);
            
            // Recalculate counts from database
            long totalSamples = sampleRepository.countByDataset(dataset);
            long approvedCount = sampleRepository.countByDatasetAndApprovedTrue(dataset);
            long actualDeleted = beforeCount - totalSamples;
            
            dataset.setTotalSamples((int) totalSamples);
            dataset.setApprovedCount((int) approvedCount);
            datasetRepository.save(dataset);
            
            Map<String, Object> result = new HashMap<>();
            result.put("deleted_count", actualDeleted);
            result.put("requested_count", sampleIds.size());
            result.put("existing_count", existingCount);
            result.put("remaining_samples", totalSamples);
            result.put("remaining_approved", approvedCount);
            
            logger.info("Bulk deleted {} samples (requested: {}, existed: {}) from template {}", 
                       actualDeleted, sampleIds.size(), existingCount, templateId);
            
            return NERTrainingResponse.success("Bulk delete completed", result);
            
        } catch (Exception e) {
            logger.error("Error bulk deleting samples", e);
            return NERTrainingResponse.error("Failed to bulk delete samples: " + e.getMessage());
        }
    }

    /**
     * Get training status (delegates to Python worker)
     */
    public NERTrainingResponse getTrainingStatus(String trainingId) {
        logger.info("Getting training status for: {}", trainingId);
        
        try {
            Map<String, Object> payload = new HashMap<>();
            payload.put("training_id", trainingId);
            
            String payloadJson = objectMapper.writeValueAsString(payload);
            
            String result = pythonWorkerClient.callWorker(
                "AAA0_0203_W",
                "get_training_status",
                payloadJson
            ).get(); // Block and wait for response
            
            return parseWorkerResponse(result);
        } catch (Exception e) {
            logger.error("Error getting training status", e);
            return NERTrainingResponse.error("Failed to get training status: " + e.getMessage());
        }
    }
    
    // ==================== Entity Type Operations ====================
    
    /**
     * Get all entity types for a dataset (including global ones)
     */
    @Cacheable(value = "entityTypes", 
               key = "#templateId != null && !#templateId.isEmpty() ? #templateId : 'global'", 
               unless = "#result == null || #result.isEmpty()")
    public List<NEREntityType> getEntityTypes(String templateId) {
        logger.info("Getting entity types for template: {}", templateId);
        
        if (templateId == null || templateId.isEmpty()) {
            return entityTypeRepository.findByTemplateIdIsNullAndActiveTrue();
        }
        
        return entityTypeRepository.findByTemplateIdIncludingGlobal(templateId);
    }
    
    /**
     * Get global entity types only
     */
    @Cacheable(value = "entityTypes", key = "'global'", unless = "#result == null || #result.isEmpty()")
    public List<NEREntityType> getGlobalEntityTypes() {
        logger.info("Getting global entity types");
        return entityTypeRepository.findByTemplateIdIsNullAndActiveTrue();
    }
    
    /**
     * Create new entity type
     */
    @CacheEvict(value = "entityTypes", allEntries = true)
    public NERTrainingResponse createEntityType(
            String templateId, 
            String entityCode, 
            String displayLabel,
            String description,
            String color,
            String icon,
            Integer displayOrder,
            String examples) {
        
        try {
            logger.info("Creating entity type: {} for template: {}", entityCode, templateId);
            
            // Check if ACTIVE entity code already exists for this template
            // Note: Inactive (deleted) entities should be allowed to be recreated
            List<NEREntityType> existingTypes = templateId != null && !templateId.isEmpty()
                ? entityTypeRepository.findByTemplateIdAndActiveTrue(templateId)
                : entityTypeRepository.findByTemplateIdIsNullAndActiveTrue();
                
            boolean exists = existingTypes.stream()
                .anyMatch(et -> et.getEntityCode().equals(entityCode));
                
            if (exists) {
                String scope = (templateId != null && !templateId.isEmpty()) 
                    ? "template '" + templateId + "'" 
                    : "global";
                logger.warn("Entity code '{}' already exists for {}", entityCode, scope);
                return NERTrainingResponse.error("Entity code already exists: " + entityCode + " for " + scope);
            }
            
            NEREntityType entityType = new NEREntityType(templateId, entityCode, displayLabel);
            entityType.setDescription(description);
            entityType.setColor(color);
            entityType.setIcon(icon);
            entityType.setDisplayOrder(displayOrder != null ? displayOrder : 0);
            entityType.setExamples(examples);
            
            entityType = entityTypeRepository.save(entityType);
            
            Map<String, Object> responseData = new HashMap<>();
            responseData.put("entity_type", convertEntityTypeToMap(entityType));
            
            logger.info("Created entity type: {}", entityCode);
            return NERTrainingResponse.success("Entity type created successfully", responseData);
            
        } catch (Exception e) {
            logger.error("Failed to create entity type", e);
            return NERTrainingResponse.error("Failed to create entity type: " + e.getMessage());
        }
    }
    
    /**
     * Update entity type
     */
    @CacheEvict(value = "entityTypes", allEntries = true)
    public NERTrainingResponse updateEntityType(
            Long id,
            String entityCode,
            String displayLabel,
            String description,
            String color,
            String icon,
            Integer displayOrder,
            Boolean active,
            String examples) {
        
        try {
            logger.info("Updating entity type: {}", id);
            
            NEREntityType entityType = entityTypeRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Entity type not found: " + id));
            
            if (entityCode != null) entityType.setEntityCode(entityCode);
            if (displayLabel != null) entityType.setDisplayLabel(displayLabel);
            if (description != null) entityType.setDescription(description);
            if (color != null) entityType.setColor(color);
            if (icon != null) entityType.setIcon(icon);
            if (displayOrder != null) entityType.setDisplayOrder(displayOrder);
            if (active != null) entityType.setActive(active);
            if (examples != null) entityType.setExamples(examples);
            
            entityType = entityTypeRepository.save(entityType);
            
            Map<String, Object> responseData = new HashMap<>();
            responseData.put("entity_type", convertEntityTypeToMap(entityType));
            
            logger.info("Updated entity type: {}", id);
            return NERTrainingResponse.success("Entity type updated successfully", responseData);
            
        } catch (Exception e) {
            logger.error("Failed to update entity type", e);
            return NERTrainingResponse.error("Failed to update entity type: " + e.getMessage());
        }
    }
    
    /**
     * Delete entity type
     */
    @CacheEvict(value = "entityTypes", allEntries = true)
    public NERTrainingResponse deleteEntityType(Long id) {
        try {
            logger.info("Deleting entity type: {}", id);
            
            NEREntityType entityType = entityTypeRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Entity type not found: " + id));
            
            // Soft delete - just deactivate
            entityType.setActive(false);
            entityTypeRepository.save(entityType);
            
            logger.info("Deleted (deactivated) entity type: {}", id);
            return NERTrainingResponse.success("Entity type deleted successfully");
            
        } catch (Exception e) {
            logger.error("Failed to delete entity type", e);
            return NERTrainingResponse.error("Failed to delete entity type: " + e.getMessage());
        }
    }
    
    /**
     * Initialize default entity types for medical documents
     */
    @CacheEvict(value = "entityTypes", allEntries = true)
    public NERTrainingResponse initializeDefaultEntityTypes() {
        try {
            logger.info("Initializing default entity types");
            
            // Check if already initialized
            List<NEREntityType> existing = entityTypeRepository.findByTemplateIdIsNullAndActiveTrue();
            if (!existing.isEmpty()) {
                return NERTrainingResponse.success("Default entity types already exist");
            }
            
            // Medical entity types with emoji icons
            createDefaultEntityType("PERSON_NAME", "Tên người bệnh", "Họ tên đầy đủ của người bệnh", "blue", "👤", 1, "Nguyễn Văn A, Trần Thị B");
            createDefaultEntityType("GENDER", "Giới tính", "Nam hoặc nữ", "pink", "👥", 2, "Nam, Nữ");
            createDefaultEntityType("AGE", "Tuổi", "Tuổi hoặc năm sinh", "purple", "📅", 3, "45 tuổi, 1978");
            createDefaultEntityType("DATE", "Ngày tháng", "Ngày khám, nhập viện, xuất viện", "green", "📅", 4, "01/01/2024, 15/12/2023");
            createDefaultEntityType("ADDRESS", "Địa chỉ", "Nơi ở, nơi làm việc", "orange", "📍", 5, "123 Nguyễn Trãi, Hà Nội");
            createDefaultEntityType("PHONE", "Số điện thoại", "Điện thoại liên hệ", "cyan", "📞", 6, "0912345678, 024.38123456");
            createDefaultEntityType("ID_NUMBER", "Số CMND/CCCD", "Chứng minh nhân dân hoặc căn cước công dân", "red", "💳", 7, "001234567890");
            createDefaultEntityType("MEDICAL_RECORD_NUMBER", "Số hồ sơ/BA", "Mã bệnh án", "indigo", "📄", 8, "BA-2024-001234");
            createDefaultEntityType("DIAGNOSIS", "Chẩn đoán", "Chẩn đoán bệnh", "red", "❤️", 9, "Viêm phổi, Đái tháo đường");
            createDefaultEntityType("SYMPTOM", "Triệu chứng", "Dấu hiệu, triệu chứng bệnh", "yellow", "⚠️", 10, "Ho, sốt, đau đầu");
            createDefaultEntityType("MEDICATION", "Thuốc", "Tên thuốc, liều dùng", "teal", "💊", 11, "Paracetamol 500mg");
            createDefaultEntityType("LAB_TEST", "Xét nghiệm", "Tên xét nghiệm và kết quả", "gray", "🧪", 12, "Công thức máu, X-quang");
            createDefaultEntityType("DOCTOR_NAME", "Tên bác sĩ", "Họ tên bác sĩ điều trị", "blue", "🩺", 13, "BS. Nguyễn Văn C");
            createDefaultEntityType("DEPARTMENT", "Khoa/Phòng", "Khoa điều trị", "gray", "🏢", 14, "Khoa Nội, Phòng Khám Tổng Hợp");
            createDefaultEntityType("HOSPITAL_NAME", "Tên bệnh viện", "Tên cơ sở y tế", "green", "🏥", 15, "BV Bạch Mai, BV 108");
            
            logger.info("Initialized 15 default entity types");
            return NERTrainingResponse.success("Default entity types initialized successfully");
            
        } catch (Exception e) {
            logger.error("Failed to initialize default entity types", e);
            return NERTrainingResponse.error("Failed to initialize: " + e.getMessage());
        }
    }
    
    /**
     * Helper method to create default entity type
     */
    private void createDefaultEntityType(String code, String label, String desc, String color, String icon, int order, String examples) {
        NEREntityType entityType = new NEREntityType(null, code, label);
        entityType.setDescription(desc);
        entityType.setColor(color);
        entityType.setIcon(icon);
        entityType.setDisplayOrder(order);
        entityType.setExamples(examples);
        entityTypeRepository.save(entityType);
    }
    
    /**
     * Convert entity type to map for JSON response
     */
    private Map<String, Object> convertEntityTypeToMap(NEREntityType entityType) {
        Map<String, Object> map = new HashMap<>();
        map.put("id", entityType.getId());
        map.put("template_id", entityType.getTemplateId());
        map.put("entity_code", entityType.getEntityCode());
        map.put("display_label", entityType.getDisplayLabel());
        map.put("description", entityType.getDescription());
        map.put("color", entityType.getColor());
        map.put("icon", entityType.getIcon());
        map.put("display_order", entityType.getDisplayOrder());
        map.put("active", entityType.getActive());
        map.put("examples", entityType.getExamples());
        map.put("created_at", entityType.getCreatedAt().toString());
        map.put("updated_at", entityType.getUpdatedAt().toString());
        return map;
    }
}
