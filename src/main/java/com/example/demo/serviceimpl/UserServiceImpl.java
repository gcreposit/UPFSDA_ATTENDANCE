package com.example.demo.serviceimpl;

import com.example.demo.dto.AuthRequest;
import com.example.demo.dto.AuthResponse;
import com.example.demo.entity.User;
import com.example.demo.repository.UserRepository;
import com.example.demo.security.JwtUtil;
import com.example.demo.service.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class UserServiceImpl implements UserService {
    
    private static final Logger log = LoggerFactory.getLogger(UserServiceImpl.class);
    
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    
    public UserServiceImpl(UserRepository userRepository, PasswordEncoder passwordEncoder, JwtUtil jwtUtil) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtil = jwtUtil;
    }
    
    @Override
    public AuthResponse registerUser(AuthRequest authRequest) {
        log.info("Starting user registration process for username: {}", authRequest.getUsername());
        authRequest.logRequest("REGISTER");
        
        try {
            // Check if user already exists
            if (existsByUsername(authRequest.getUsername())) {
                log.warn("Registration failed: Username {} already exists", authRequest.getUsername());
                return AuthResponse.failure("Username already exists");
            }
            
            if (existsByEmail(authRequest.getEmail())) {
                log.warn("Registration failed: Email {} already exists", authRequest.getEmail());
                return AuthResponse.failure("Email already exists");
            }
            
            // Create new user
            User user = new User();
            user.setUsername(authRequest.getUsername());
            user.setEmail(authRequest.getEmail());
            user.setPassword(passwordEncoder.encode(authRequest.getPassword()));
            
            log.debug("Saving new user to database");
            User savedUser = userRepository.save(user);
            
            // Generate JWT token
            String token = jwtUtil.generateToken(savedUser);
            
            log.info("User registration completed successfully for username: {}", authRequest.getUsername());
            return AuthResponse.success(token, savedUser.getUsername());
            
        } catch (Exception e) {
            log.error("Error during user registration for username: {}", authRequest.getUsername(), e);
            return AuthResponse.failure("Registration failed: " + e.getMessage());
        }
    }
    
    @Override
    public AuthResponse authenticateUser(AuthRequest authRequest) {
        log.info("Starting authentication process for username: {}", authRequest.getUsername());
        authRequest.logRequest("LOGIN");
        
        try {
            // Manual authentication using password encoder
            log.debug("Attempting to find user: {}", authRequest.getUsername());
            User user = findByUsername(authRequest.getUsername());
            log.debug("User found: {}, ID: {}", user.getUsername(), user.getId());
            
            log.debug("Verifying password for user: {}", authRequest.getUsername());
            if (passwordEncoder.matches(authRequest.getPassword(), user.getPassword())) {
                log.debug("Password verification successful, generating JWT token");
                String token = jwtUtil.generateToken(user);
                log.debug("JWT token generated successfully");
                
                log.info("Authentication completed successfully for username: {}", authRequest.getUsername());
                return AuthResponse.success(token, user.getUsername());
            } else {
                log.warn("Password verification failed for username: {}", authRequest.getUsername());
                return AuthResponse.failure("Invalid username or password");
            }
            
        } catch (UsernameNotFoundException e) {
            log.warn("User not found during authentication: {}", authRequest.getUsername());
            return AuthResponse.failure("Invalid username or password");
        } catch (ClassCastException e) {
            log.error("ClassCastException during authentication for username: {}", authRequest.getUsername(), e);
            return AuthResponse.failure("Authentication system error. Please try again.");
        } catch (Exception e) {
            log.error("Error during authentication for username: {}", authRequest.getUsername(), e);
            return AuthResponse.failure("Authentication failed: " + e.getMessage());
        }
    }
    
    @Override
    public User findByUsername(String username) {
        log.debug("Finding user by username: {}", username);
        
        // For MasterAdmin, try database first
        if ("MasterAdmin".equals(username)) {
            try {
                return findRealUserByUsername(username);
            } catch (UsernameNotFoundException e) {
                // If not found in database, create it
                createMasterAdminUser();
                return findRealUserByUsername(username);
            }
        }
        
        // Support mock employee user
        if ("user".equals(username)) {
            log.debug("Returning mock employee user");
            User mockEmployee = new User();
            mockEmployee.setId(998L); // Set a mock ID
            mockEmployee.setUsername("user");
            mockEmployee.setEmail("lokesh@UPFSDA.com");
            mockEmployee.setPassword(passwordEncoder.encode("pass"));
            return mockEmployee;
        }
        
        // Use database for real users
        return findRealUserByUsername(username);
    }
    
    private User findRealUserByUsername(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> {
                    log.error("User not found with username: {}", username);
                    return new UsernameNotFoundException("User not found: " + username);
                });
    }
    
    @Override
    public boolean existsByUsername(String username) {
        log.debug("Checking if username exists: {}", username);
        
        // For MasterAdmin, check database first, create if doesn't exist
        if ("MasterAdmin".equals(username)) {
            boolean exists = userRepository.existsByUsername(username);
            if (!exists) {
                // Auto-create MasterAdmin user
                createMasterAdminUser();
                return true;
            }
            return exists;
        }
        
        // Check mock users
        if ("user".equals(username)) {
            return true;
        }
        
        boolean exists = userRepository.existsByUsername(username);
        log.debug("Username {} exists: {}", username, exists);
        return exists;
    }
    
    private void createMasterAdminUser() {
        try {
            log.info("Auto-creating MasterAdmin user");
            User masterAdmin = new User();
            masterAdmin.setUsername("MasterAdmin");
            masterAdmin.setEmail("admin@UPFSDA.com");
            masterAdmin.setPassword(passwordEncoder.encode("admin123"));
            userRepository.save(masterAdmin);
            log.info("MasterAdmin user created successfully");
        } catch (Exception e) {
            log.error("Failed to create MasterAdmin user", e);
        }
    }
    
    @Override
    public boolean existsByEmail(String email) {
        log.debug("Checking if email exists: {}", email);
        
        // Check mock emails first
        if ("admin@UPFSDA.com".equals(email) || "lokesh@UPFSDA.com".equals(email)) {
            return true;
        }
        
        boolean exists = userRepository.existsByEmail(email);
        log.debug("Email {} exists: {}", email, exists);
        return exists;
    }
    
    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        log.info("Loading user details for username: {}", username);
        
        // For MasterAdmin, try database first
        if ("MasterAdmin".equals(username)) {
            try {
                return findRealUserByUsername(username);
            } catch (UsernameNotFoundException e) {
                // If not found in database, create it
                createMasterAdminUser();
                return findRealUserByUsername(username);
            }
        }
        
        // Support mock employee user
        if ("user".equals(username)) {
            log.debug("Loading mock employee user");
            User mockEmployee = new User();
            mockEmployee.setId(998L); // Set a mock ID
            mockEmployee.setUsername("user");
            mockEmployee.setEmail("lokesh@UPFSDA.com");
            mockEmployee.setPassword(passwordEncoder.encode("pass"));
            return mockEmployee;
        }
        
        // Try to find in database
        try {
            User user = findRealUserByUsername(username);
            log.debug("User details loaded successfully for username: {}", username);
            return user;
        } catch (UsernameNotFoundException e) {
            log.error("User not found with username: {}", username);
            throw e;
        }
    }
}