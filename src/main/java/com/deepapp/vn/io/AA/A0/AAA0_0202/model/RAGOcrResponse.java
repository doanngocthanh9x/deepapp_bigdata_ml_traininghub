package com.deepapp.vn.io.AA.A0.AAA0_0202.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import java.util.Map;

/**
 * Response model for RAG OCR processing
 */
public class RAGOcrResponse {

    @JsonProperty("success")
    private boolean success;

    @JsonProperty("error")
    private String error;

    @JsonProperty("ocr_text")
    private List<String> ocrText;

    @JsonProperty("extracted_fields")
    private Map<String, Object> extractedFields;

    @JsonProperty("answer")
    private String answer;

    @JsonProperty("answer_confidence")
    private String answerConfidence;

    @JsonProperty("rag_results")
    private List<Map<String, Object>> ragResults;

    @JsonProperty("processing_time_ms")
    private long processingTimeMs;

    @JsonProperty("template_used")
    private String templateUsed;

    // Constructors
    private RAGOcrResponse() {}

    public static RAGOcrResponse success(List<String> ocrText, Map<String, Object> extractedFields,
                                       String answer, String confidence, List<Map<String, Object>> ragResults,
                                       long processingTime, String templateUsed) {
        RAGOcrResponse response = new RAGOcrResponse();
        response.success = true;
        response.ocrText = ocrText;
        response.extractedFields = extractedFields;
        response.answer = answer;
        response.answerConfidence = confidence;
        response.ragResults = ragResults;
        response.processingTimeMs = processingTime;
        response.templateUsed = templateUsed;
        return response;
    }

    public static RAGOcrResponse error(String errorMessage) {
        RAGOcrResponse response = new RAGOcrResponse();
        response.success = false;
        response.error = errorMessage;
        return response;
    }

    // Getters
    public boolean isSuccess() {
        return success;
    }

    public String getError() {
        return error;
    }

    public List<String> getOcrText() {
        return ocrText;
    }

    public Map<String, Object> getExtractedFields() {
        return extractedFields;
    }

    public String getAnswer() {
        return answer;
    }

    public String getAnswerConfidence() {
        return answerConfidence;
    }

    public List<Map<String, Object>> getRagResults() {
        return ragResults;
    }

    public long getProcessingTimeMs() {
        return processingTimeMs;
    }

    public String getTemplateUsed() {
        return templateUsed;
    }

    @Override
    public String toString() {
        return "RAGOcrResponse{" +
                "success=" + success +
                ", error='" + error + '\'' +
                ", ocrTextCount=" + (ocrText != null ? ocrText.size() : 0) +
                ", extractedFieldsCount=" + (extractedFields != null ? extractedFields.size() : 0) +
                ", answer='" + answer + '\'' +
                ", answerConfidence='" + answerConfidence + '\'' +
                ", ragResultsCount=" + (ragResults != null ? ragResults.size() : 0) +
                ", processingTimeMs=" + processingTimeMs +
                ", templateUsed='" + templateUsed + '\'' +
                '}';
    }
}