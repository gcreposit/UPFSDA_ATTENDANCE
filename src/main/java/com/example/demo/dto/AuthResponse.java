package com.example.demo.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AuthResponse {
    private static final Logger log = LoggerFactory.getLogger(AuthResponse.class);
    
    private boolean success;
    private String token;
    private String message;
    private String username;
    
    public static AuthResponse success(String token, String username) {
        log.info("Creating successful auth response for user: {}", username);
        return new AuthResponse(true, token, "Authentication successful", username);
    }
    
    public static AuthResponse failure(String message) {
        log.warn("Creating failure auth response with message: {}", message);
        return new AuthResponse(false, null, message, null);
    }
}