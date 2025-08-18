package com.example.demo.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AuthRequest {
    private static final Logger log = LoggerFactory.getLogger(AuthRequest.class);

    private String username;
    private String password;
    private String email; // for registration

    public void logRequest(String action) {
        log.info("Auth request received for action: {} with username: {}", action, username);
    }

}