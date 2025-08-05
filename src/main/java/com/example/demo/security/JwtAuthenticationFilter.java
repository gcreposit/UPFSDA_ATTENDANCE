package com.example.demo.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(JwtAuthenticationFilter.class);

    private final JwtUtil jwtUtil;
    private final UserDetailsService userDetailsService; // Use interface instead of concrete class

    public JwtAuthenticationFilter(JwtUtil jwtUtil, UserDetailsService userDetailsService) {
        this.jwtUtil = jwtUtil;
        this.userDetailsService = userDetailsService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        final String requestTokenHeader = request.getHeader("Authorization");

        String username = null;
        String jwtToken = null;

        log.debug("Processing request to: {}", request.getRequestURI());

        if (requestTokenHeader != null && requestTokenHeader.startsWith("Bearer ")) {
            jwtToken = requestTokenHeader.substring(7);
            log.debug("JWT token found in request header");

            try {
                username = jwtUtil.extractUsername(jwtToken);
                log.debug("Username extracted from token: {}", username);
            } catch (Exception e) {
                log.warn("Unable to extract username from JWT token", e);
            }
        } else {
            log.debug("JWT token not found or doesn't start with Bearer");
        }

        if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {
            log.debug("Validating token for user: {}", username);

            try {
                UserDetails userDetails = userDetailsService.loadUserByUsername(username);

                if (jwtUtil.validateToken(jwtToken, userDetails)) {
                    log.info("JWT token validated successfully for user: {}", username);

                    UsernamePasswordAuthenticationToken authToken =
                            new UsernamePasswordAuthenticationToken(
                                    userDetails, null, userDetails.getAuthorities());
                    authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                    SecurityContextHolder.getContext().setAuthentication(authToken);

                    log.debug("Authentication set in security context for user: {}", username);
                } else {
                    log.warn("JWT token validation failed for user: {}", username);
                }
            } catch (Exception e) {
                log.error("Error validating JWT token for user: {}", username, e);
            }
        }

        filterChain.doFilter(request, response);
    }
}