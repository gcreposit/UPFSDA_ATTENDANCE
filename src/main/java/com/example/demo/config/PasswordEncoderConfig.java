package com.example.demo.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Configuration
public class PasswordEncoderConfig {

    private static final Logger log = LoggerFactory.getLogger(PasswordEncoderConfig.class);

    @Bean
    public PasswordEncoder passwordEncoder() {
        log.info("Creating BCrypt password encoder bean");
        return new BCryptPasswordEncoder();
    }
}