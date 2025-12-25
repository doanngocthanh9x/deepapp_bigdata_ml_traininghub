package com.deepapp.vn.io.ZZ.A0.ZZA0_0100.service;

import com.deepapp.vn.io.storage.dto.PageDTO;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Service to store and retrieve processed page data
 * Stores page data in memory for document management
 */
@Service
public class PageDataStorageService {

    // requestId -> List<PageDTO>
    private final Map<String, List<PageDTO>> pageStorage = new ConcurrentHashMap<>();

    /**
     * Store pages for a request
     */
    public void storePages(String requestId, List<PageDTO> pages) {
        pageStorage.put(requestId, pages);
    }

    /**
     * Get pages for a request
     */
    public List<PageDTO> getPages(String requestId) {
        return pageStorage.get(requestId);
    }

    /**
     * Check if pages exist for a request
     */
    public boolean hasPages(String requestId) {
        return pageStorage.containsKey(requestId);
    }

    /**
     * Remove pages for a request
     */
    public void removePages(String requestId) {
        pageStorage.remove(requestId);
    }

    /**
     * Get all stored request IDs
     */
    public List<String> getAllRequestIds() {
        return pageStorage.keySet().stream().collect(Collectors.toList());
    }

    /**
     * Get storage statistics
     */
    public Map<String, Object> getStatistics() {
        int totalRequests = pageStorage.size();
        int totalPages = pageStorage.values().stream()
                .mapToInt(List::size)
                .sum();

        return Map.of(
            "totalRequests", totalRequests,
            "totalPages", totalPages
        );
    }

    /**
     * Clear all stored data
     */
    public void clearAll() {
        pageStorage.clear();
    }
}