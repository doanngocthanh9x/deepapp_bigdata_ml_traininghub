package com.deepapp.vn.io.AA.A0.AAA0_0202.service;

import com.deepapp.vn.io.AA.A0.AAA0_0202.model.RAGOcrRequest;
import com.deepapp.vn.io.AA.A0.AAA0_0202.model.RAGOcrResponse;
import com.deepapp.vn.io.workers.PythonWorkerClient;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.HashMap;

/**
 * Service for RAG OCR processing
 */
@Service
public class RAGOcrService {

    private static final Logger logger = LoggerFactory.getLogger(RAGOcrService.class);
    private static final String RAG_OCR_WORKER_ID = "AAA0_0202_W";  // Python RAG OCR worker task ID

    @Autowired
    private PythonWorkerClient pythonWorkerClient;

    private final ObjectMapper objectMapper = new ObjectMapper();

    // In-memory template storage (in production, use database)
    private Map<String, Map<String, Object>> templates = new HashMap<>();

    public RAGOcrService() {
        // Initialize with default templates
        initializeDefaultTemplates();
    }

    /**
     * Process RAG OCR request
     */
    public RAGOcrResponse processRAGOcr(RAGOcrRequest request) {
        long startTime = System.currentTimeMillis();

        try {
            // Build JSON payload for Python worker
            String payload = buildRAGOcrPayload(request);
            logger.info("Sending RAG OCR request to worker: template={}, query={}",
                       request.getTemplateId(), request.getQuery());

            // Call RAG OCR worker via gRPC using PythonWorkerClient
            String result = pythonWorkerClient.callWorker(RAG_OCR_WORKER_ID, "process_document", payload).get();
            long processingTime = System.currentTimeMillis() - startTime;

            logger.info("RAG OCR completed in {}ms", processingTime);
            logger.debug("Python worker response: {}", result);

            // Parse response from Python worker
            return parseRAGOcrResult(result, processingTime);

        } catch (Exception e) {
            logger.error("RAG OCR processing failed", e);
            return RAGOcrResponse.error("RAG OCR processing failed: " + e.getMessage());
        }
    }

    /**
     * Query document information
     */
    public RAGOcrResponse queryDocument(RAGOcrRequest request) {
        try {
            // Build JSON payload for Python worker
            String payload = buildQueryPayload(request);
            logger.info("Sending query request to worker: {}", request.getQuery());

            // Call RAG OCR worker via gRPC
            String result = pythonWorkerClient.callWorker(RAG_OCR_WORKER_ID, "query_document", payload).get();

            // Parse response
            return parseQueryResult(result);

        } catch (Exception e) {
            logger.error("Query processing failed", e);
            return RAGOcrResponse.error("Query processing failed: " + e.getMessage());
        }
    }

    /**
     * Extract fields using document template
     */
    private Map<String, Object> extractFieldsWithTemplate(List<String> ocrText, String templateId) {
        Map<String, Object> fields = new HashMap<>();

        // Mock extraction based on template
        if ("discharge_summary".equals(templateId)) {
            fields.put("patient_name", "Nguyễn Văn A");
            fields.put("diagnosis", "Viêm phổi");
            fields.put("age", "45");
        }

        return fields;
    }

    /**
     * Query document fields
     */
    private Map<String, Object> queryDocumentFields(String query, Map<String, Object> fields) {
        String answer = "Không tìm thấy thông tin";
        String confidence = "LOW";
        List<Map<String, Object>> ragResults = List.of();

        query = query.toLowerCase();

        if (query.contains("tên") || query.contains("bệnh nhân")) {
            answer = (String) fields.getOrDefault("patient_name", "Không có thông tin");
            confidence = "HIGH";
        } else if (query.contains("tuổi")) {
            answer = (String) fields.getOrDefault("age", "Không có thông tin");
            confidence = "HIGH";
        } else if (query.contains("bệnh") || query.contains("chẩn đoán")) {
            answer = (String) fields.getOrDefault("diagnosis", "Không có thông tin");
            confidence = "HIGH";
        }

        return Map.of(
            "answer", answer,
            "confidence", confidence,
            "ragResults", ragResults
        );
    }

    /**
     * Get available templates
     */
    public List<Map<String, Object>> getAvailableTemplates() {
        return templates.values().stream().toList();
    }

    /**
     * Save template configuration
     */
    public boolean saveTemplate(Object templateConfig) {
        try {
            // In production, validate and save to database
            logger.info("Template saved: {}", templateConfig);
            return true;
        } catch (Exception e) {
            logger.error("Error saving template", e);
            return false;
        }
    }

    /**
     * Delete template
     */
    public boolean deleteTemplate(String templateId) {
        try {
            templates.remove(templateId);
            logger.info("Template deleted: {}", templateId);
            return true;
        } catch (Exception e) {
            logger.error("Error deleting template", e);
            return false;
        }
    }

    /**
     * Build JSON payload for Python RAG OCR worker
     */
    private String buildRAGOcrPayload(RAGOcrRequest request) {
        try {
            StringBuilder json = new StringBuilder("{");

            if (request.getImageBase64() != null && !request.getImageBase64().isEmpty()) {
                json.append("\"image_base64\":\"").append(request.getImageBase64()).append("\",");
            }

            if (request.getImagePath() != null && !request.getImagePath().isEmpty()) {
                json.append("\"image_path\":\"").append(request.getImagePath()).append("\",");
            }

            if (request.getTemplateId() != null && !request.getTemplateId().isEmpty()) {
                json.append("\"template_id\":\"").append(request.getTemplateId()).append("\",");
            }

            if (request.getQuery() != null && !request.getQuery().isEmpty()) {
                json.append("\"query\":\"").append(request.getQuery().replace("\"", "\\\"")).append("\",");
            }

            json.append("\"language\":\"").append(request.getLanguage()).append("\",");
            json.append("\"save_index\":").append(request.getSaveIndex()).append(",");

            if (request.getIndexPath() != null && !request.getIndexPath().isEmpty()) {
                json.append("\"index_path\":\"").append(request.getIndexPath()).append("\"");
            } else {
                json.append("\"index_path\":\"rag_index\"");
            }

            json.append("}");

            return json.toString();

        } catch (Exception e) {
            throw new RuntimeException("Failed to build RAG OCR payload", e);
        }
    }

    /**
     * Build JSON payload for query request
     */
    private String buildQueryPayload(RAGOcrRequest request) {
        try {
            StringBuilder json = new StringBuilder("{");

            if (request.getQuery() != null && !request.getQuery().isEmpty()) {
                json.append("\"query\":\"").append(request.getQuery().replace("\"", "\\\"")).append("\",");
            }

            if (request.getTemplateId() != null && !request.getTemplateId().isEmpty()) {
                json.append("\"template_id\":\"").append(request.getTemplateId()).append("\",");
            }

            if (request.getIndexPath() != null && !request.getIndexPath().isEmpty()) {
                json.append("\"index_path\":\"").append(request.getIndexPath()).append("\"");
            } else {
                json.append("\"index_path\":\"rag_index\"");
            }

            json.append("}");

            return json.toString();

        } catch (Exception e) {
            throw new RuntimeException("Failed to build query payload", e);
        }
    }

    /**
     * Parse RAG OCR result from Python worker
     */
    private RAGOcrResponse parseRAGOcrResult(String result, long processingTime) {
        try {
            logger.debug("Parsing RAG OCR result: {}", result);
            JsonNode jsonNode = objectMapper.readTree(result);

            // Check for error
            if (jsonNode.has("error")) {
                return RAGOcrResponse.error(jsonNode.get("error").asText());
            }

            // Extract data from response
            JsonNode dataNode = jsonNode.has("data") ? jsonNode.get("data") : jsonNode;
            logger.debug("Data node: {}", dataNode.toString());

            // Extract OCR text
            List<String> ocrText = List.of();
            if (dataNode.has("ocr_text")) {
                ocrText = objectMapper.convertValue(dataNode.get("ocr_text"), List.class);
            }

            // Extract fields
            Map<String, Object> extractedFields = new HashMap<>();
            if (dataNode.has("extracted_fields")) {
                extractedFields = objectMapper.convertValue(dataNode.get("extracted_fields"), Map.class);
            }
            logger.debug("Extracted fields: {} (size: {})", extractedFields, extractedFields.size());

            // Extract answer and confidence
            String answer = dataNode.has("answer") ? dataNode.get("answer").asText() : null;
            String confidence = dataNode.has("confidence") ? dataNode.get("confidence").asText() : "LOW";
            logger.debug("Answer: {}, Confidence: {}", answer, confidence);

            // Extract RAG results
            List<Map<String, Object>> ragResults = List.of();
            if (dataNode.has("rag_results")) {
                ragResults = objectMapper.convertValue(dataNode.get("rag_results"), List.class);
            }

            // Extract template used
            String templateUsed = dataNode.has("template_used") ? dataNode.get("template_used").asText() : null;

            return RAGOcrResponse.success(
                ocrText,
                extractedFields,
                answer,
                confidence,
                ragResults,
                processingTime,
                templateUsed
            );

        } catch (Exception e) {
            logger.error("Failed to parse RAG OCR result", e);
            return RAGOcrResponse.error("Failed to parse RAG OCR result: " + e.getMessage());
        }
    }

    /**
     * Parse query result from Python worker
     */
    private RAGOcrResponse parseQueryResult(String result) {
        try {
            JsonNode jsonNode = objectMapper.readTree(result);

            // Check for error
            if (jsonNode.has("error")) {
                return RAGOcrResponse.error(jsonNode.get("error").asText());
            }

            // Extract data from response
            JsonNode dataNode = jsonNode.has("data") ? jsonNode.get("data") : jsonNode;

            // Extract fields
            Map<String, Object> extractedFields = new HashMap<>();
            if (dataNode.has("extracted_fields")) {
                extractedFields = objectMapper.convertValue(dataNode.get("extracted_fields"), Map.class);
            }

            // Extract answer and confidence
            String answer = dataNode.has("answer") ? dataNode.get("answer").asText() : null;
            String confidence = dataNode.has("confidence") ? dataNode.get("confidence").asText() : "LOW";

            // Extract RAG results
            List<Map<String, Object>> ragResults = List.of();
            if (dataNode.has("rag_results")) {
                ragResults = objectMapper.convertValue(dataNode.get("rag_results"), List.class);
            }

            return RAGOcrResponse.success(
                List.of(),
                extractedFields,
                answer,
                confidence,
                ragResults,
                100,
                "query_template"
            );

        } catch (Exception e) {
            logger.error("Failed to parse query result", e);
            return RAGOcrResponse.error("Failed to parse query result: " + e.getMessage());
        }
    }

    /**
     * Initialize default templates
     */
    private void initializeDefaultTemplates() {
        templates.put("discharge_summary", Map.of(
            "id", "discharge_summary",
            "name", "Giấy ra viện",
            "description", "Template cho giấy tóm tắt ra viện",
            "fields", List.of("patient_name", "diagnosis", "age", "address")
        ));

        templates.put("prescription", Map.of(
            "id", "prescription",
            "name", "Đơn thuốc",
            "description", "Template cho đơn thuốc",
            "fields", List.of("doctor_name", "medication", "dosage")
        ));
    }
}