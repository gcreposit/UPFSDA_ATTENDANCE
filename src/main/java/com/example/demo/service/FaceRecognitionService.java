package com.example.demo.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.*;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import java.io.File;
import java.util.concurrent.CompletableFuture;

@Service
@RequiredArgsConstructor
@Slf4j
public class FaceRecognitionService {

    private final RestTemplate restTemplate;
    private final FileStorageService fileStorageService;

    @Value("${face-recognition.api.url:}")
    private String apiUrl;

    @Value("${face-recognition.enabled:false}")
    private boolean enabled;

    @Value("${face-recognition.timeout-seconds:30}")
    private int timeoutSeconds;

    @Async
    public CompletableFuture<Void> sendToFaceRecognitionAPI(String identityCardNo, String userName, String facePhotoPath) {
        if (!enabled) {
            log.debug("Face recognition API is disabled, skipping call for user: {}", userName);
            return CompletableFuture.completedFuture(null);
        }

        if (apiUrl == null || apiUrl.trim().isEmpty()) {
            log.warn("Face recognition API URL is not configured, skipping call for user: {}", userName);
            return CompletableFuture.completedFuture(null);
        }

        try {
            String userKey = generateUserKey(identityCardNo, userName);
            log.info("Sending face recognition data for user: {} with key: {}", userName, userKey);

            // Prepare the request
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.MULTIPART_FORM_DATA);

            // Prepare the multipart body
            MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
            body.add("idusername", userKey);

            // Add the image file
            File imageFile = fileStorageService.getAbsolutePath(facePhotoPath).toFile();
            if (imageFile.exists()) {
                body.add("file", new FileSystemResource(imageFile));
            } else {
                log.error("Face photo file not found at path: {}", facePhotoPath);
                return CompletableFuture.completedFuture(null);
            }

            HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(body, headers);

            // Make the API call
            ResponseEntity<String> response = restTemplate.exchange(
                    apiUrl,
                    HttpMethod.POST,
                    requestEntity,
                    String.class
            );

            if (response.getStatusCode().is2xxSuccessful()) {
                log.info("Successfully sent face recognition data for user: {}", userName);
            } else {
                log.warn("Face recognition API returned non-success status: {} for user: {}",
                        response.getStatusCode(), userName);
            }

        } catch (Exception e) {
            log.error("Failed to send face recognition data for user: {} - Error: {}", userName, e.getMessage(), e);
            // Don't rethrow the exception - we don't want to fail employee creation
        }

        return CompletableFuture.completedFuture(null);
    }

    private String generateUserKey(String identityCardNo, String userName) {
        if (identityCardNo == null || userName == null) {
            throw new IllegalArgumentException("Identity card number and user name cannot be null");
        }

        String sanitizedIdentityCard = identityCardNo.replaceAll("[^a-zA-Z0-9]", "");
        String sanitizedUserName = userName.replaceAll("[^a-zA-Z0-9]", "_");

        return sanitizedIdentityCard + "_" + sanitizedUserName;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public String getApiUrl() {
        return apiUrl;
    }

    // For testing purposes
    public String generateUserKeyForTesting(String identityCardNo, String userName) {
        return generateUserKey(identityCardNo, userName);
    }

}