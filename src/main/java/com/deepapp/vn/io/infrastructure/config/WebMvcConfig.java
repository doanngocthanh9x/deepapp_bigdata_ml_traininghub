package com.deepapp.vn.io.infrastructure.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Web MVC Configuration
 *
 * Configures static resource handling for serving rendered page images
 * from the /tmp/deepapp/pages/ directory.
 *
 * URLs:
 * - /pages/** -> /tmp/deepapp/pages/
 * - /static/** -> classpath:/static/
 */
@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // Serve rendered page images from /tmp/deepapp/pages/ (new path)
        // Example: http://localhost:8080/pages/document1/page_1.png
        //      -> /tmp/deepapp/pages/document1/page_1.png
        registry.addResourceHandler("/pages/**")
                .addResourceLocations("file:/tmp/deepapp/pages/")
                .setCachePeriod(3600); // Cache for 1 hour

        // Serve old uploaded files from /tmp/deepapp/uploads/ (backward compatibility)
        // Example: http://localhost:8080/uploads/uuid/page_1.png
        //      -> /tmp/deepapp/uploads/uuid/page_1.png
        registry.addResourceHandler("/uploads/**")
                .addResourceLocations("file:/tmp/deepapp/uploads/")
                .setCachePeriod(3600); // Cache for 1 hour

        // Serve static assets from classpath (CSS, JS, etc.)
        registry.addResourceHandler("/static/**")
                .addResourceLocations("classpath:/static/")
                .setCachePeriod(0); // Disable cache for development
    }
}
