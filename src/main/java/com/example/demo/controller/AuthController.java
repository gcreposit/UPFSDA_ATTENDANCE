package com.example.demo.controller;

import com.example.demo.dto.AuthRequest;
import com.example.demo.dto.AuthResponse;
import com.example.demo.service.UserService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@RestController
@RequestMapping("/api/auth")
@CrossOrigin(origins = "*")
public class AuthController {
    
    private static final Logger log = LoggerFactory.getLogger(AuthController.class);
    
    private final UserService userService;
    
    public AuthController(UserService userService) {
        this.userService = userService;
    }
    
    @PostMapping("/signup")
    public ResponseEntity<AuthResponse> signup(@RequestBody AuthRequest authRequest) {
        log.info("Signup request received for username: {}", authRequest.getUsername());
        
        try {
            // Enhanced server-side validation
            String validationError = validateSignupRequest(authRequest);
            if (validationError != null) {
                log.warn("Signup validation failed: {}", validationError);
                return ResponseEntity.badRequest()
                        .body(AuthResponse.failure(validationError));
            }
            
            AuthResponse response = userService.registerUser(authRequest);
            
            if (response.getToken() != null) {
                log.info("Signup successful for username: {}", authRequest.getUsername());
                return ResponseEntity.ok(response);
            } else {
                log.warn("Signup failed for username: {} - {}", authRequest.getUsername(), response.getMessage());
                return ResponseEntity.badRequest().body(response);
            }
            
        } catch (Exception e) {
            log.error("Error during signup for username: {}", authRequest.getUsername(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(AuthResponse.failure("Internal server error"));
        }
    }
    
    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@RequestBody AuthRequest authRequest, HttpServletRequest request) {
        log.info("Login request received for username: {}", authRequest.getUsername());
        
        try {
            // Rate limiting check
            String clientIP = getClientIP(request);
            if (isRateLimited(clientIP, authRequest.getUsername())) {
                log.warn("Rate limit exceeded for IP: {} and username: {}", clientIP, authRequest.getUsername());
                return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                        .body(AuthResponse.failure("Too many login attempts. Please try again later."));
            }
            
            // Enhanced server-side validation
            String validationError = validateLoginRequest(authRequest);
            if (validationError != null) {
                log.warn("Login validation failed: {}", validationError);
                return ResponseEntity.badRequest()
                        .body(AuthResponse.failure(validationError));
            }
            
            AuthResponse response = userService.authenticateUser(authRequest);
            
            if (response.getToken() != null) {
                log.info("Login successful for username: {}", authRequest.getUsername());
                // Reset rate limit on successful login
                resetRateLimit(clientIP, authRequest.getUsername());
                return ResponseEntity.ok(response);
            } else {
                log.warn("Login failed for username: {} - {}", authRequest.getUsername(), response.getMessage());
                // Increment failed attempts
                incrementFailedAttempts(clientIP, authRequest.getUsername());
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
            }
            
        } catch (Exception e) {
            log.error("Error during login for username: {}", authRequest.getUsername(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(AuthResponse.failure("Internal server error"));
        }
    }
    
    private String getClientIP(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
    
    // Simple in-memory rate limiting (in production, use Redis)
    private final Map<String, Integer> failedAttempts = new ConcurrentHashMap<>();
    private final Map<String, Long> lastAttemptTime = new ConcurrentHashMap<>();
    private static final int MAX_ATTEMPTS = 5;
    private static final long LOCKOUT_TIME = 15 * 60 * 1000; // 15 minutes
    
    private boolean isRateLimited(String clientIP, String username) {
        String key = clientIP + ":" + username;
        Integer attempts = failedAttempts.get(key);
        Long lastAttempt = lastAttemptTime.get(key);
        
        if (attempts == null || attempts < MAX_ATTEMPTS) {
            return false;
        }
        
        if (lastAttempt != null && (System.currentTimeMillis() - lastAttempt) > LOCKOUT_TIME) {
            // Reset after lockout period
            failedAttempts.remove(key);
            lastAttemptTime.remove(key);
            return false;
        }
        
        return true;
    }
    
    private void incrementFailedAttempts(String clientIP, String username) {
        String key = clientIP + ":" + username;
        failedAttempts.merge(key, 1, Integer::sum);
        lastAttemptTime.put(key, System.currentTimeMillis());
    }
    
    private void resetRateLimit(String clientIP, String username) {
        String key = clientIP + ":" + username;
        failedAttempts.remove(key);
        lastAttemptTime.remove(key);
    }
    
    // Enhanced input validation
    private String validateLoginRequest(AuthRequest request) {
        if (request.getUsername() == null || request.getUsername().trim().isEmpty()) {
            return "Username is required";
        }
        
        if (request.getPassword() == null || request.getPassword().trim().isEmpty()) {
            return "Password is required";
        }
        
        // Username validation
        String username = request.getUsername().trim();
        if (username.length() < 3 || username.length() > 50) {
            return "Username must be between 3 and 50 characters";
        }
        
        if (!username.matches("^[a-zA-Z0-9._-]+$")) {
            return "Username can only contain letters, numbers, dots, underscores, and hyphens";
        }
        
        // Password validation
        if (request.getPassword().length() < 6) {
            return "Password must be at least 6 characters long";
        }
        
        // Check for potential SQL injection patterns
        if (containsSqlInjectionPatterns(username) || containsSqlInjectionPatterns(request.getPassword())) {
            return "Invalid characters detected";
        }
        
        return null; // Valid
    }
    
    private String validateSignupRequest(AuthRequest request) {
        // First check login validation
        String loginValidation = validateLoginRequest(request);
        if (loginValidation != null) {
            return loginValidation;
        }
        
        // Email validation
        if (request.getEmail() == null || request.getEmail().trim().isEmpty()) {
            return "Email is required";
        }
        
        String email = request.getEmail().trim();
        if (!email.matches("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")) {
            return "Please enter a valid email address";
        }
        
        if (email.length() > 100) {
            return "Email address is too long";
        }
        
        // Enhanced password validation for signup
        String password = request.getPassword();
        if (password.length() > 128) {
            return "Password is too long";
        }
        
        // Check for at least one letter and one number
        if (!password.matches(".*[a-zA-Z].*") || !password.matches(".*\\d.*")) {
            return "Password must contain at least one letter and one number";
        }
        
        return null; // Valid
    }
    
    private boolean containsSqlInjectionPatterns(String input) {
        if (input == null) return false;
        
        String lowerInput = input.toLowerCase();
        String[] sqlPatterns = {
            "select", "insert", "update", "delete", "drop", "create", "alter",
            "union", "or 1=1", "and 1=1", "--", "/*", "*/", "xp_", "sp_",
            "exec", "execute", "script", "<script", "javascript:", "vbscript:"
        };
        
        for (String pattern : sqlPatterns) {
            if (lowerInput.contains(pattern)) {
                return true;
            }
        }
        
        return false;
    }
    
    @GetMapping("/validate")
    public ResponseEntity<String> validateToken(@RequestHeader("Authorization") String token) {
        log.info("Token validation request received");
        
        if (token != null && token.startsWith("Bearer ")) {
            log.debug("Valid token format received");
            return ResponseEntity.ok("Token is valid");
        } else {
            log.warn("Invalid token format received");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid token");
        }
    }
}