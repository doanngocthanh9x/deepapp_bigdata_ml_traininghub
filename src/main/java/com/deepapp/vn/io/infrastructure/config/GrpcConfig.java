package com.deepapp.vn.io.infrastructure.config;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class GrpcConfig {

    @Value("${grpc.hub.host:72.60.111.138}")
    private String hubHost;

    @Value("${grpc.hub.port:50051}")
    private int hubPort;

    @Bean
    public ManagedChannel hubManagedChannel() {
        return ManagedChannelBuilder.forAddress(hubHost, hubPort)
                .usePlaintext()
                .build();
    }
}