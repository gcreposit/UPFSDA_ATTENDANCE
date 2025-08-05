package com.example.demo.config;

import com.example.demo.security.JwtAuthenticationFilter;
import com.example.demo.security.JwtUtil;
import com.example.demo.service.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class JwtConfig {
    
    private static final Logger log = LoggerFactory.getLogger(JwtConfig.class);
    
    @Bean
    public JwtUtil jwtUtil() {
        log.info("Creating JWT utility bean");
        return new JwtUtil();
    }
    
//    @Bean
//    public JwtAuthenticationFilter jwtAuthenticationFilter(JwtUtil jwtUtil, UserService userService) {
//        log.info("Creating JWT authentication filter bean");
//        return new JwtAuthenticationFilter(jwtUtil, userService);
//    }
}