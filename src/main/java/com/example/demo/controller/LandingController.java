package com.example.demo.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@RestController
@CrossOrigin(origins = "*")
public class LandingController {
    
    private static final Logger log = LoggerFactory.getLogger(LandingController.class);
    
    @GetMapping("/home")
    public ResponseEntity<Map<String, Object>> home() {
        log.info("Home page accessed");
        
        Map<String, Object> response = new HashMap<>();
        response.put("message", "Welcome to the Demo Application!");
        response.put("status", "success");
        response.put("endpoints", Map.of(
            "signup", "/api/auth/signup",
            "login", "/api/auth/login",
            "landing", "/landing",
            "validate", "/api/auth/validate"
        ));
        
        log.debug("Home page response prepared");
        return ResponseEntity.ok(response);
    }
    
    @GetMapping("/landing")
    public ResponseEntity<Map<String, Object>> landing() {
        log.info("Landing page accessed");
        
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        boolean isAuthenticated = authentication != null && authentication.isAuthenticated() 
                && !authentication.getName().equals("anonymousUser");
        
        Map<String, Object> response = new HashMap<>();
        response.put("message", "Welcome to the Landing Page!");
        response.put("authenticated", isAuthenticated);
        
        if (isAuthenticated) {
            log.info("Authenticated user {} accessed landing page", authentication.getName());
            response.put("username", authentication.getName());
            response.put("authorities", authentication.getAuthorities());
        } else {
            log.info("Anonymous user accessed landing page");
            response.put("message", "Please login to access personalized content");
        }
        
        return ResponseEntity.ok(response);
    }
    
    @GetMapping("/protected")
    public ResponseEntity<Map<String, Object>> protectedEndpoint() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        log.info("Protected endpoint accessed by user: {}", authentication.getName());
        
        Map<String, Object> response = new HashMap<>();
        response.put("message", "This is a protected endpoint!");
        response.put("username", authentication.getName());
        response.put("authorities", authentication.getAuthorities());
        response.put("timestamp", System.currentTimeMillis());
        
        return ResponseEntity.ok(response);
    }
}