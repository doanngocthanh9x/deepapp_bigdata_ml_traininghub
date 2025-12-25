package com.deepapp.vn.io.workers;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class WorkerConfig {

    @Bean
    public CppWorkerClient cppWorkerClient(
            @Value("${workers.cpp.host:localhost}") String host,
            @Value("${workers.cpp.port:50051}") int port) {
        return new CppWorkerClient(host, port);
    }
}
