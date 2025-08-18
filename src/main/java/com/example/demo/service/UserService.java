package com.example.demo.service;

import com.example.demo.dto.AuthRequest;
import com.example.demo.dto.AuthResponse;
import com.example.demo.entity.User;
import org.springframework.security.core.userdetails.UserDetailsService;

public interface UserService extends UserDetailsService {
    
    AuthResponse registerUser(AuthRequest authRequest);
    
    AuthResponse authenticateUser(AuthRequest authRequest);
    
    User findByUsername(String username);
    
    boolean existsByUsername(String username);
    
    boolean existsByEmail(String email);

}