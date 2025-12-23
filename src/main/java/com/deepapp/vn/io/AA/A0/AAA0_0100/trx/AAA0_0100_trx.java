package com.deepapp.vn.io.AA.A0.AAA0_0100.trx;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.web.bind.annotation.*;
import org.springframework.beans.factory.annotation.Autowired;
import com.deepapp.vn.io.workers.CppWorkerClient;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/AA/A0/AAA0_0100")
public class AAA0_0100_trx {
    
    @Autowired
    private CppWorkerClient cppWorkerClient;
    
    /**
     * Get user profile after OAuth2 login (or anonymous if not logged in)
     */
    @GetMapping
    public Map<String, Object> getUserProfile(@AuthenticationPrincipal OAuth2User principal) {
        Map<String, Object> response = new HashMap<>();
        
        if (principal != null) {
            response.put("success", true);
            response.put("authenticated", true);
            response.put("user", Map.of(
                "name", principal.getAttribute("name"),
                "email", principal.getAttribute("email"),
                "provider", "OAuth2",
                "attributes", principal.getAttributes()
            ));
        } else {
            response.put("success", true);
            response.put("authenticated", false);
            response.put("message", "Anonymous access - OAuth2 not configured yet");
            response.put("user", Map.of(
                "name", "Anonymous",
                "email", "demo@deepapp.io",
                "provider", "None"
            ));
        }
        
        return response;
    }
    
    /**
     * Process data with C++ worker (works with or without auth)
     */
    @PostMapping
    public Map<String, Object> processData(
            @AuthenticationPrincipal OAuth2User principal,
            @RequestBody Map<String, Object> request) {
        
        Map<String, Object> response = new HashMap<>();
        
        try {
            // Get user info (or use anonymous)
            String userName = principal != null ? principal.getAttribute("name") : "Anonymous";
            String userEmail = principal != null ? principal.getAttribute("email") : "demo@deepapp.io";
            boolean authenticated = principal != null;
            
            // Call C++ worker
            String taskId = "AAA0_0100_W";
            String eventType = request.getOrDefault("eventType", "process").toString();
            String inputData = request.getOrDefault("data", "").toString();
            
            // Add user context to data
            String dataWithContext = String.format("{\"user\":\"%s\",\"email\":\"%s\",\"data\":\"%s\"}", 
                userName, userEmail, inputData);
            
            String cppResponse = cppWorkerClient.callWorker(taskId, eventType, dataWithContext).get();
            
            response.put("success", true);
            response.put("authenticated", authenticated);
            response.put("user", userName);
            response.put("email", userEmail);
            response.put("flowName", "GeneralOAuthFlow");
            response.put("cppResponse", cppResponse);
            response.put("timestamp", System.currentTimeMillis());
            
        } catch (Exception e) {
            response.put("success", false);
            response.put("error", e.getMessage());
        }
        
        return response;
    }
    
    /**
     * Logout endpoint
     */
    @PostMapping("/logout")
    public Map<String, Object> logout() {
        return Map.of(
            "success", true,
            "message", "Logged out successfully"
        );
    }
}

