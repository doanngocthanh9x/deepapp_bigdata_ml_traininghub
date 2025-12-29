package com.deepapp.vn.io.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * Web Controller for serving static pages and SPA routing
 */
@Controller
public class WebController {

    /**
     * Serve YOLO detection page
     */
    @GetMapping("/yolo-detect")
    public String yoloDetect() {
        return "yolo-detect";
    }

    /**
     * Serve React SPA for all other routes
     * This enables client-side routing for the React application
     */
    @GetMapping("/{path:[^\\.]*}")
    public String serveSpa() {
        return "forward:/static/index.html";
    }

    /**
     * Serve home page - redirect to React SPA
     */
    @GetMapping("/")
    public String home() {
        return "forward:/static/index.html";
    }
}