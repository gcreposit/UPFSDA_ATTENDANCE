package com.example.demo.controller;

import com.example.demo.security.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/web")
public class AuthCheckController {

    @Autowired
    private JwtUtil jwtUtil;

    @PostMapping("/validate-token")
    public ResponseEntity<Map<String, Object>> validateToken(@RequestBody Map<String, String> request) {
        String token = request.get("token");
        Map<String, Object> response = new HashMap<>();
        
        try {
            if (token != null && jwtUtil.isTokenValid(token)) {
                String username = jwtUtil.extractUsername(token);
                response.put("valid", true);
                response.put("username", username);
                return ResponseEntity.ok(response);
            }
        } catch (Exception e) {
            // Token is invalid (expired, malformed, etc.)
        }
        
        response.put("valid", false);
        return ResponseEntity.ok(response);
    }
}