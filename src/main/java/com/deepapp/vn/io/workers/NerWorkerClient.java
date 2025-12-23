package com.deepapp.vn.io.workers;

import com.deepapp.hub.EventChunk;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * Example Worker Client 2 - NER (Named Entity Recognition) Worker
 * This demonstrates how to create another specific worker client
 */
@Service
public class NerWorkerClient extends BaseWorkerClient {

    private static final Logger logger = LoggerFactory.getLogger(NerWorkerClient.class);

    @Value("${workers.ner.targetId:cpp-ner-worker}")
    private String nerTargetId;

    public NerWorkerClient(
            @Value("${workers.ner.host:localhost}") String nerHost,
            @Value("${workers.ner.port:50053}") int nerPort) {
        super("java-ner-client", nerHost, nerPort);
        logger.info("NerWorkerClient created with host={}, port={}", nerHost, nerPort);
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        super.afterPropertiesSet();
        logger.info("NER Worker Client initialized - target: {}", nerTargetId);
    }

    @Override
    protected String getWorkerTargetId() {
        return nerTargetId;
    }

    /**
     * Extract named entities from text
     */
    public java.util.concurrent.CompletableFuture<String> extractEntities(String text) {
        logger.info("Requesting NER for text: {}", text.substring(0, Math.min(50, text.length())));
        return executeTask("ner", text);
    }

    /**
     * Extract entities with specific model
     */
    public java.util.concurrent.CompletableFuture<String> extractEntities(String text, String model) {
        String taskData = String.format("{\"text\":\"%s\",\"model\":\"%s\"}", text, model);
        return executeTask("ner", taskData);
    }

    @Override
    protected void handleWorkerEvent(EventChunk event) {
        logger.info("NER Worker event: {} - {}", event.getEventType(), event.getPayload().toStringUtf8());
    }
}
