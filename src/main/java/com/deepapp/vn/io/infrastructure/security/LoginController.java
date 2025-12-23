package com.deepapp.vn.io.infrastructure.security;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class LoginController {
    
    @GetMapping("/")
    public String home() {
        return "redirect:/oauth-setup";
    }
    
    @GetMapping("/login")
    public String login() {
        // Check if OAuth is configured
        String googleClientId = System.getenv("GOOGLE_CLIENT_ID");
        if (googleClientId == null || "your-google-client-id".equals(googleClientId)) {
            return "redirect:/oauth-setup";
        }
        return "login";
    }
    
    @GetMapping("/oauth-setup")
    public String oauthSetup() {
        return "oauth-setup";
    }
}
