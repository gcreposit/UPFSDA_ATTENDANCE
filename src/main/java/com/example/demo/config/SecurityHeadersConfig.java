package com.example.demo.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;

@Configuration
public class SecurityHeadersConfig {

    @Bean
    public SecurityHeadersFilter securityHeadersFilter() {
        return new SecurityHeadersFilter();
    }

    public static class SecurityHeadersFilter extends OncePerRequestFilter {

        @Override
        protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                FilterChain filterChain) throws ServletException, IOException {

            // Content Security Policy - Prevents XSS attacks
            response.setHeader("Content-Security-Policy",
                    "default-src 'self'; " +
                            "script-src 'self' 'unsafe-inline' https://cdn.jsdelivr.net https://cdnjs.cloudflare.com https://maps.googleapis.com; "
                            +
                            "style-src 'self' 'unsafe-inline' https://cdn.jsdelivr.net https://cdnjs.cloudflare.com https://fonts.googleapis.com; "
                            +
                            "font-src 'self' https://fonts.gstatic.com https://cdnjs.cloudflare.com; " +
                            "img-src 'self' data: https: http:; " +
                            "connect-src 'self' https://maps.googleapis.com");

            // X-Frame-Options - Prevents clickjacking
            response.setHeader("X-Frame-Options", "DENY");

            // X-Content-Type-Options - Prevents MIME type sniffing
            response.setHeader("X-Content-Type-Options", "nosniff");

            // X-XSS-Protection - Enables XSS filtering
            response.setHeader("X-XSS-Protection", "1; mode=block");

            // Referrer-Policy - Controls referrer information
            response.setHeader("Referrer-Policy", "strict-origin-when-cross-origin");

            // Permissions-Policy - Controls browser features
            response.setHeader("Permissions-Policy",
                    "camera=(), microphone=(), geolocation=(), payment=()");

            // HSTS - Forces HTTPS (only add in production with HTTPS)
            if (request.isSecure()) {
                response.setHeader("Strict-Transport-Security",
                        "max-age=31536000; includeSubDomains; preload");
            }

            filterChain.doFilter(request, response);
        }
    }
}