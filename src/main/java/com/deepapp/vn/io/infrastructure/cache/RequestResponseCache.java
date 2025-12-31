package com.deepapp.vn.io.infrastructure.cache;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.util.Map;

/**
 * Shared SQLite cache for request/response data to avoid gRPC buffer overflow
 * Both Java and Python can read/write to this cache
 */
public class RequestResponseCache {

    private static final Logger logger = LoggerFactory.getLogger(RequestResponseCache.class);

    private static final String DB_PATH = "/tmp/grpc_response_cache.db";
    private static final String DB_URL = "jdbc:sqlite:" + DB_PATH;
    private static final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * Store data in SQLite cache and return cache ID
     */
    public static String storeData(Object data, int ttlSeconds) {
        try {
            // Generate unique ID
            String cacheId = "resp_" + System.currentTimeMillis() + "_" + Thread.currentThread().getId();

            // Serialize data to JSON
            String jsonData = objectMapper.writeValueAsString(data);
            int sizeBytes = jsonData.getBytes("UTF-8").length;

            // Calculate expiration time
            double createdAt = System.currentTimeMillis() / 1000.0;
            double expiresAt = createdAt + ttlSeconds;

            // Store in database
            try (Connection conn = DriverManager.getConnection(DB_URL);
                 PreparedStatement stmt = conn.prepareStatement(
                     "INSERT INTO response_cache (response_id, data, created_at, expires_at, size_bytes) " +
                     "VALUES (?, ?, ?, ?, ?)")) {

                stmt.setString(1, cacheId);
                stmt.setString(2, jsonData);
                stmt.setDouble(3, createdAt);
                stmt.setDouble(4, expiresAt);
                stmt.setInt(5, sizeBytes);

                stmt.executeUpdate();

                logger.info("[RequestResponseCache] Stored data {} ({} bytes, TTL: {}s)",
                    cacheId, sizeBytes, ttlSeconds);

                return cacheId;
            }

        } catch (Exception e) {
            logger.error("[RequestResponseCache] Failed to store data", e);
            return null;
        }
    }

    /**
     * Retrieve data from SQLite cache by ID
     */
    public static Map<String, Object> getData(String cacheId) {
        try {
            try (Connection conn = DriverManager.getConnection(DB_URL);
                 PreparedStatement stmt = conn.prepareStatement(
                     "SELECT data, expires_at FROM response_cache WHERE response_id = ?")) {

                stmt.setString(1, cacheId);

                try (ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) {
                        String jsonData = rs.getString("data");
                        double expiresAt = rs.getDouble("expires_at");

                        // Check if expired
                        if (System.currentTimeMillis() / 1000.0 > expiresAt) {
                            logger.warn("[RequestResponseCache] Data expired: {}", cacheId);
                            return null;
                        }

                        // Parse JSON data
                        @SuppressWarnings("unchecked")
                        Map<String, Object> data = objectMapper.readValue(jsonData, Map.class);
                        return data;
                    }
                }
            }

            logger.warn("[RequestResponseCache] Data not found: {}", cacheId);
            return null;

        } catch (Exception e) {
            logger.error("[RequestResponseCache] Failed to retrieve data: {}", cacheId, e);
            return null;
        }
    }

    /**
     * Delete data from cache
     */
    public static void deleteData(String cacheId) {
        try {
            try (Connection conn = DriverManager.getConnection(DB_URL);
                 PreparedStatement stmt = conn.prepareStatement(
                     "DELETE FROM response_cache WHERE response_id = ?")) {

                stmt.setString(1, cacheId);
                stmt.executeUpdate();

                logger.debug("[RequestResponseCache] Deleted data: {}", cacheId);
            }

        } catch (Exception e) {
            logger.error("[RequestResponseCache] Failed to delete data: {}", cacheId, e);
        }
    }
}
