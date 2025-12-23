package com.deepapp.vn.io.infrastructure.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class OpenApiConfig {

    @Value("${server.port:8080}")
    private int serverPort;

    @Bean
    public OpenAPI customOpenAPI() {
        Server localServer = new Server();
        localServer.setUrl("http://localhost:" + serverPort);
        localServer.setDescription("Local Development");

        Server dockerServer = new Server();
        dockerServer.setUrl("http://localhost:" + serverPort);
        dockerServer.setDescription("Docker Container");

        Contact contact = new Contact();
        contact.setEmail("support@deepapp.io");
        contact.setName("DeepApp Team");
        contact.setUrl("https://github.com/doanngocthanh9x/deepapp_bigdata_ml_traininghub");

        License license = new License()
                .name("Apache 2.0")
                .url("https://www.apache.org/licenses/LICENSE-2.0");

        Info info = new Info()
                .title("DeepApp - Java + C++ gRPC System API")
                .version("1.0.0")
                .description("""
                    # DeepApp Main API Documentation
                    
                    Scalable architecture combining Java Spring Boot with C++ workers via gRPC for high-performance processing.
                    
                    ## Architecture
                    - **Java Spring Boot**: REST API layer with business logic
                    - **C++ Workers**: High-performance processing via gRPC
                    - **gRPC Hub**: Bidirectional streaming at 72.60.111.138:50051
                    
                    ## Features
                    - 3-Layer Architecture (Infrastructure → Workers → Modules)
                    - Auto-registering C++ workers
                    - OAuth2 authentication (Google, GitHub)
                    - Real-time gRPC communication
                    - Docker support with layer caching
                    
                    ## Authentication
                    OAuth2 is optional. All endpoints work without authentication for testing.
                    Configure GOOGLE_CLIENT_ID and GITHUB_CLIENT_ID to enable OAuth2.
                    """)
                .contact(contact)
                .license(license);

        return new OpenAPI()
                .info(info)
                .servers(List.of(localServer, dockerServer));
    }
}
