package com.example.demo.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FaceRecognitionServiceTest {
    
    @Mock
    private RestTemplate restTemplate;
    
    @Mock
    private FileStorageService fileStorageService;
    
    @InjectMocks
    private FaceRecognitionService faceRecognitionService;
    
    @TempDir
    Path tempDir;
    
    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(faceRecognitionService, "apiUrl", "http://test-api.com/face-recognition");
        ReflectionTestUtils.setField(faceRecognitionService, "apiKey", "test-api-key");
        ReflectionTestUtils.setField(faceRecognitionService, "enabled", true);
        ReflectionTestUtils.setField(faceRecognitionService, "timeoutSeconds", 30);
    }
    
    @Test
    void generateUserKeyForTesting_ShouldGenerateCorrectKey() {
        String result = faceRecognitionService.generateUserKeyForTesting("ABC123", "john_doe");
        
        assertThat(result).isEqualTo("ABC123_john_doe");
    }
    
    @Test
    void generateUserKeyForTesting_ShouldSanitizeSpecialCharacters() {
        String result = faceRecognitionService.generateUserKeyForTesting("ABC-123/456", "john doe");
        
        assertThat(result).isEqualTo("ABC123456_john_doe");
    }
    
    @Test
    void sendToFaceRecognitionAPI_ShouldSkipWhenDisabled() throws Exception {
        ReflectionTestUtils.setField(faceRecognitionService, "enabled", false);
        
        CompletableFuture<Void> result = faceRecognitionService.sendToFaceRecognitionAPI(
            "ABC123", "john_doe", "path/to/photo.jpg");
        
        result.get(); // Wait for completion
        
        verify(restTemplate, never()).exchange(any(), any(), any(), eq(String.class));
    }
    
    @Test
    void sendToFaceRecognitionAPI_ShouldSkipWhenApiUrlEmpty() throws Exception {
        ReflectionTestUtils.setField(faceRecognitionService, "apiUrl", "");
        
        CompletableFuture<Void> result = faceRecognitionService.sendToFaceRecognitionAPI(
            "ABC123", "john_doe", "path/to/photo.jpg");
        
        result.get(); // Wait for completion
        
        verify(restTemplate, never()).exchange(any(), any(), any(), eq(String.class));
    }
    
    @Test
    void sendToFaceRecognitionAPI_ShouldSkipWhenFileNotExists() throws Exception, IOException {
        Path nonExistentFile = tempDir.resolve("nonexistent.jpg");
        when(fileStorageService.getAbsolutePath("path/to/photo.jpg")).thenReturn(nonExistentFile);
        
        CompletableFuture<Void> result = faceRecognitionService.sendToFaceRecognitionAPI(
            "ABC123", "john_doe", "path/to/photo.jpg");
        
        result.get(); // Wait for completion
        
        verify(restTemplate, never()).exchange(any(), any(), any(), eq(String.class));
    }
    
    @Test
    void sendToFaceRecognitionAPI_ShouldMakeApiCallWhenFileExists() throws Exception, IOException {
        // Create a test file
        Path testFile = tempDir.resolve("test.jpg");
        Files.createFile(testFile);
        
        when(fileStorageService.getAbsolutePath("path/to/photo.jpg")).thenReturn(testFile);
        when(restTemplate.exchange(any(), any(), any(), eq(String.class)))
            .thenReturn(new ResponseEntity<>("Success", HttpStatus.OK));
        
        CompletableFuture<Void> result = faceRecognitionService.sendToFaceRecognitionAPI(
            "ABC123", "john_doe", "path/to/photo.jpg");
        
        result.get(); // Wait for completion
        
        verify(restTemplate, times(1)).exchange(any(), any(), any(), eq(String.class));
    }
    
    @Test
    void sendToFaceRecognitionAPI_ShouldHandleApiException() throws Exception, IOException {
        // Create a test file
        Path testFile = tempDir.resolve("test.jpg");
        Files.createFile(testFile);
        
        when(fileStorageService.getAbsolutePath("path/to/photo.jpg")).thenReturn(testFile);
        when(restTemplate.exchange(any(), any(), any(), eq(String.class)))
            .thenThrow(new RuntimeException("API Error"));
        
        CompletableFuture<Void> result = faceRecognitionService.sendToFaceRecognitionAPI(
            "ABC123", "john_doe", "path/to/photo.jpg");
        
        // Should complete without throwing exception
        result.get();
        
        verify(restTemplate, times(1)).exchange(any(), any(), any(), eq(String.class));
    }
    
    @Test
    void isEnabled_ShouldReturnConfiguredValue() {
        ReflectionTestUtils.setField(faceRecognitionService, "enabled", true);
        assertThat(faceRecognitionService.isEnabled()).isTrue();
        
        ReflectionTestUtils.setField(faceRecognitionService, "enabled", false);
        assertThat(faceRecognitionService.isEnabled()).isFalse();
    }
    
    @Test
    void getApiUrl_ShouldReturnConfiguredUrl() {
        String testUrl = "http://test-url.com";
        ReflectionTestUtils.setField(faceRecognitionService, "apiUrl", testUrl);
        
        assertThat(faceRecognitionService.getApiUrl()).isEqualTo(testUrl);
    }
}