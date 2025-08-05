package com.example.demo.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.security.core.userdetails.UserDetails;

import javax.crypto.SecretKey;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class JwtUtil {
    
    private static final Logger log = LoggerFactory.getLogger(JwtUtil.class);
    
    @Value("${jwt.secret:mySecretKey}")
    private String secret;
    
    @Value("${jwt.expiration:86400}")
    private Long expiration;
    
    private SecretKey getSigningKey() {
        return Keys.hmacShaKeyFor(secret.getBytes());
    }
    
    public String extractUsername(String token) {
        log.debug("Extracting username from JWT token");
        return extractClaim(token, Claims::getSubject);
    }
    
    public Date extractExpiration(String token) {
        log.debug("Extracting expiration from JWT token");
        return extractClaim(token, Claims::getExpiration);
    }
    
    public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }
    
    private Claims extractAllClaims(String token) {
        try {
            return Jwts.parserBuilder()
                    .setSigningKey(getSigningKey())
                    .build()
                    .parseClaimsJws(token)
                    .getBody();
        } catch (Exception e) {
            log.error("Error extracting claims from token: {}", e.getMessage());
            throw e;
        }
    }
    
    private Boolean isTokenExpired(String token) {
        boolean expired = extractExpiration(token).before(new Date());
        log.debug("Token expired: {}", expired);
        return expired;
    }
    
    public String generateToken(UserDetails userDetails) {
        log.info("Generating JWT token for user: {}", userDetails.getUsername());
        Map<String, Object> claims = new HashMap<>();
        return createToken(claims, userDetails.getUsername());
    }
    
    private String createToken(Map<String, Object> claims, String subject) {
        Instant now = Instant.now();
        Instant expiryDate = now.plus(expiration, ChronoUnit.SECONDS);
        
        String token = Jwts.builder()
                .setClaims(claims)
                .setSubject(subject)
                .setIssuedAt(Date.from(now))
                .setExpiration(Date.from(expiryDate))
                .signWith(getSigningKey(), SignatureAlgorithm.HS512)
                .compact();
        
        log.info("JWT token created successfully for subject: {}", subject);
        return token;
    }
    
    @Cacheable(value = "tokenValidation", key = "#token")
    public Boolean validateToken(String token, UserDetails userDetails) {
        log.debug("Validating JWT token for user: {}", userDetails.getUsername());
        final String username = extractUsername(token);
        boolean valid = username.equals(userDetails.getUsername()) && !isTokenExpired(token);
        log.info("Token validation result for user {}: {}", userDetails.getUsername(), valid);
        return valid;
    }
    
    // Simple token validation without UserDetails (for web client validation)
    public Boolean isTokenValid(String token) {
        try {
            log.debug("Validating JWT token structure and expiration");
            return !isTokenExpired(token);
        } catch (Exception e) {
            log.debug("Token validation failed: {}", e.getMessage());
            return false;
        }
    }
}