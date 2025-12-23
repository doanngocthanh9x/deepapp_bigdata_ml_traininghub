package com.deepapp.vn.io.modules.documentprocessing;

import com.deepapp.vn.io.workers.OcrWorkerClient;
import com.deepapp.vn.io.workers.NerWorkerClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;

/**
 * Document Processing Service
 * Demonstrates how a module service uses multiple worker clients
 */
@Service
public class DocumentProcessingService {

    private static final Logger logger = LoggerFactory.getLogger(DocumentProcessingService.class);

    @Autowired
    private OcrWorkerClient ocrWorkerClient;

    @Autowired
    private NerWorkerClient nerWorkerClient;

    /**
     * Process a document: OCR + NER
     */
    public CompletableFuture<DocumentResult> processDocument(String imagePath) {
        logger.info("Starting document processing for: {}", imagePath);

        // First, perform OCR
        return ocrWorkerClient.performOcr(imagePath)
                .thenCompose(ocrText -> {
                    logger.info("OCR completed, text length: {}", ocrText.length());
                    
                    // Then, perform NER on the extracted text
                    return nerWorkerClient.extractEntities(ocrText)
                            .thenApply(entities -> {
                                logger.info("NER completed");
                                return new DocumentResult(ocrText, entities);
                            });
                })
                .exceptionally(ex -> {
                    logger.error("Error processing document: {}", ex.getMessage(), ex);
                    throw new RuntimeException("Document processing failed", ex);
                });
    }

    /**
     * Process document with language
     */
    public CompletableFuture<DocumentResult> processDocument(String imagePath, String language) {
        logger.info("Starting document processing for: {} with language: {}", imagePath, language);

        return ocrWorkerClient.performOcr(imagePath, language)
                .thenCompose(ocrText -> 
                    nerWorkerClient.extractEntities(ocrText)
                            .thenApply(entities -> new DocumentResult(ocrText, entities))
                )
                .exceptionally(ex -> {
                    logger.error("Error processing document: {}", ex.getMessage(), ex);
                    throw new RuntimeException("Document processing failed", ex);
                });
    }

    /**
     * Only perform OCR
     */
    public CompletableFuture<String> extractText(String imagePath) {
        logger.info("Extracting text from: {}", imagePath);
        return ocrWorkerClient.performOcr(imagePath);
    }

    /**
     * Only perform NER
     */
    public CompletableFuture<String> extractEntities(String text) {
        logger.info("Extracting entities from text");
        return nerWorkerClient.extractEntities(text);
    }

    public static class DocumentResult {
        private final String text;
        private final String entities;

        public DocumentResult(String text, String entities) {
            this.text = text;
            this.entities = entities;
        }

        public String getText() {
            return text;
        }

        public String getEntities() {
            return entities;
        }
    }
}
