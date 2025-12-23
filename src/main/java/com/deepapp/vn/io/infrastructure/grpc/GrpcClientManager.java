package com.deepapp.vn.io.infrastructure.grpc;

import org.springframework.stereotype.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Central manager for all gRPC client connections.
 * Provides a registry for different worker clients and modules.
 */
@Component
public class GrpcClientManager {

    private static final Logger logger = LoggerFactory.getLogger(GrpcClientManager.class);

    private final Map<String, BaseGrpcClientService> clients = new ConcurrentHashMap<>();

    /**
     * Register a gRPC client
     */
    public void registerClient(String name, BaseGrpcClientService client) {
        clients.put(name, client);
        logger.info("Registered gRPC client: {}", name);
    }

    /**
     * Get a registered client by name
     */
    public BaseGrpcClientService getClient(String name) {
        BaseGrpcClientService client = clients.get(name);
        if (client == null) {
            throw new IllegalArgumentException("No client registered with name: " + name);
        }
        return client;
    }

    /**
     * Check if a client is registered
     */
    public boolean hasClient(String name) {
        return clients.containsKey(name);
    }

    /**
     * Get all registered client names
     */
    public java.util.Set<String> getClientNames() {
        return clients.keySet();
    }

    /**
     * Unregister a client
     */
    public void unregisterClient(String name) {
        BaseGrpcClientService client = clients.remove(name);
        if (client != null) {
            logger.info("Unregistered gRPC client: {}", name);
        }
    }
}
