package com.deepapp.vn.io.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * Web Controller for serving static pages
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
     * Serve home page
     */
    @GetMapping("/")
    public String home() {
        return "redirect:/yolo-detect";
    }
}