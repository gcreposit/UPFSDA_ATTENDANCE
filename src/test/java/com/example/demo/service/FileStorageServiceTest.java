package com.example.demo.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class FileStorageServiceTest {
    
    private FileStorageService fileStorageService;
    
    @TempDir
    Path tempDir;
    
    @BeforeEach
    void setUp() {
        fileStorageService = new FileStorageService();
        ReflectionTestUtils.setField(fileStorageService, "basePath", tempDir.toString());
    }
    
    @Test
    void storeEmployeeFiles_ShouldStoreFilesSuccessfully() throws IOException {
        // Arrange
        String userName = "john_doe";
        MultipartFile facePhoto = createValidImageFile("face.jpg", "image/jpeg");
        MultipartFile signature = createValidImageFile("signature.png", "image/png");
        
        // Act
        FileStorageService.FileStorageResult result = fileStorageService.storeEmployeeFiles(userName, facePhoto);
        
        // Assert
        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getFacePhotoPath()).isNotNull();
        
        // Verify directory structure
        String expectedDirName = userName + "_" + LocalDate.now().format(DateTimeFormatter.ofPattern("dd-MM-yyyy"));
        Path employeeDir = tempDir.resolve(expectedDirName);
        assertThat(Files.exists(employeeDir)).isTrue();
        assertThat(Files.exists(employeeDir.resolve("Face"))).isTrue();
        assertThat(Files.exists(employeeDir.resolve("Signature"))).isTrue();
        
        // Verify files exist
        assertThat(fileStorageService.fileExists(result.getFacePhotoPath())).isTrue();
    }
    
    @Test
    void storeEmployeeFiles_ShouldThrowExceptionForNullFacePhoto() {
        MultipartFile signature = createValidImageFile("signature.png", "image/png");
        
        assertThatThrownBy(() -> fileStorageService.storeEmployeeFiles("john_doe", null))
            .isInstanceOf(FileStorageService.FileStorageException.class)
            .hasMessageContaining("Face Photo is required");
    }
    
    @Test
    void storeEmployeeFiles_ShouldThrowExceptionForEmptyFacePhoto() {
        MultipartFile facePhoto = new MockMultipartFile("face", "face.jpg", "image/jpeg", new byte[0]);
        MultipartFile signature = createValidImageFile("signature.png", "image/png");
        
        assertThatThrownBy(() -> fileStorageService.storeEmployeeFiles("john_doe", facePhoto))
            .isInstanceOf(FileStorageService.FileStorageException.class)
            .hasMessageContaining("Face Photo is required");
    }
    
    @Test
    void storeEmployeeFiles_ShouldThrowExceptionForInvalidFileType() {
        MultipartFile facePhoto = new MockMultipartFile("face", "face.txt", "text/plain", "content".getBytes());
        MultipartFile signature = createValidImageFile("signature.png", "image/png");
        
        assertThatThrownBy(() -> fileStorageService.storeEmployeeFiles("john_doe", facePhoto))
            .isInstanceOf(FileStorageService.FileStorageException.class)
            .hasMessageContaining("Face Photo must be a JPEG or PNG image");
    }
    
    @Test
    void storeEmployeeFiles_ShouldThrowExceptionForLargeFile() {
        byte[] largeContent = new byte[6 * 1024 * 1024]; // 6MB
        MultipartFile facePhoto = new MockMultipartFile("face", "face.jpg", "image/jpeg", largeContent);
        MultipartFile signature = createValidImageFile("signature.png", "image/png");
        
        assertThatThrownBy(() -> fileStorageService.storeEmployeeFiles("john_doe", facePhoto))
            .isInstanceOf(FileStorageService.FileStorageException.class)
            .hasMessageContaining("Face Photo size cannot exceed 5MB");
    }
    
    @Test
    void fileExists_ShouldReturnTrueForExistingFile() throws IOException {
        // Create a test file
        Path testFile = tempDir.resolve("test.txt");
        Files.createFile(testFile);
        
        boolean exists = fileStorageService.fileExists("test.txt");
        
        assertThat(exists).isTrue();
    }
    
    @Test
    void fileExists_ShouldReturnFalseForNonExistingFile() {
        boolean exists = fileStorageService.fileExists("nonexistent.txt");
        
        assertThat(exists).isFalse();
    }
    
    @Test
    void getAbsolutePath_ShouldReturnCorrectPath() {
        Path absolutePath = fileStorageService.getAbsolutePath("test/file.txt");
        
        assertThat(absolutePath).isEqualTo(tempDir.resolve("test/file.txt"));
    }
    
    private MultipartFile createValidImageFile(String fileName, String contentType) {
        byte[] content = "fake image content".getBytes();
        return new MockMultipartFile("file", fileName, contentType, content);
    }
}