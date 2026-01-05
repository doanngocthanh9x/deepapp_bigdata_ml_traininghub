package com.deepapp.vn.io.AA.A0.AAA0_0203.config;

import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Cache configuration for NER Training module
 */
@Configuration
@EnableCaching
public class NERCacheConfig {

    @Bean
    public CacheManager nerCacheManager() {
        return new ConcurrentMapCacheManager(
            "trainingDatasets",
            "datasetSamples",
            "trainedModels",
            "entityTypes"
        );
    }
}