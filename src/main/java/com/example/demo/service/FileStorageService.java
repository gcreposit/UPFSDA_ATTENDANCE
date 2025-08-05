package com.example.demo.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class FileStorageService {
    
    @Value("${file.storage.path}")
    private String basePath;
    
    private static final List<String> ALLOWED_IMAGE_TYPES = Arrays.asList(
        "image/jpeg", "image/jpg", "image/png"
    );
    
    private static final long MAX_FILE_SIZE = 50 * 1024 * 1024; // 5MB
    
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd-MM-yyyy");
    
    public FileStorageResult storeEmployeeFiles(String userName, MultipartFile facePhoto, MultipartFile signature) {
        try {
            // Validate files
            validateFile(facePhoto, "Face Photo");
            validateFile(signature, "Signature");
            
            // Create employee directory
            String employeeDir = createEmployeeDirectory(userName);
            Path employeePath = Paths.get(basePath, employeeDir);
            
            // Create Face and Signature subdirectories
            Path faceDir = employeePath.resolve("Face");
            Path signatureDir = employeePath.resolve("Signature");
            
            Files.createDirectories(faceDir);
            Files.createDirectories(signatureDir);


            // Store face photo
            String facePhotoFileName = userName+"_photo"+getFileExtension(facePhoto.getOriginalFilename());
            Path facePhotoPath = faceDir.resolve(facePhotoFileName);
            Files.copy(facePhoto.getInputStream(), facePhotoPath, StandardCopyOption.REPLACE_EXISTING);


            // Store signature
            String signatureFileName =userName+"_sign"+getFileExtension(signature.getOriginalFilename());
            Path signaturePath = signatureDir.resolve(signatureFileName);
            Files.copy(signature.getInputStream(), signaturePath, StandardCopyOption.REPLACE_EXISTING);
            
            // Create relative paths for database storage
            String facePhotoRelativePath = employeeDir + "/Face/" + facePhotoFileName;
            String signatureRelativePath = employeeDir + "/Signature/" + signatureFileName;
            
            log.info("Successfully stored files for employee: {}", userName);
            log.debug("Face photo stored at: {}", facePhotoRelativePath);
            log.debug("Signature stored at: {}", signatureRelativePath);
            
            return FileStorageResult.success(facePhotoRelativePath, signatureRelativePath);
            
        } catch (IOException e) {
            log.error("Failed to store files for employee: {}", userName, e);
            throw new FileStorageException("Failed to store employee files: " + e.getMessage(), e);
        }
    }
    
    private void validateFile(MultipartFile file, String fileType) {
        if (file == null || file.isEmpty()) {
            throw new FileStorageException(fileType + " is required");
        }
        
        if (file.getSize() > MAX_FILE_SIZE) {
            throw new FileStorageException(fileType + " size cannot exceed 5MB");
        }
        
        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_IMAGE_TYPES.contains(contentType.toLowerCase())) {
            throw new FileStorageException(fileType + " must be a JPEG or PNG image");
        }
    }
    
    private String createEmployeeDirectory(String userName) {
        String currentDate = LocalDate.now().format(DATE_FORMATTER);
        return userName.replaceAll("[^a-zA-Z0-9]", "_") + "_" + currentDate;
    }
    
    private String generateFileName(String originalFileName) {
        if (originalFileName == null) {
            return UUID.randomUUID().toString() + ".jpg";
        }
        
        String extension = getFileExtension(originalFileName);
        return UUID.randomUUID().toString() + extension;
    }
    
    private String getFileExtension(String fileName) {
        int lastDotIndex = fileName.lastIndexOf('.');
        if (lastDotIndex > 0 && lastDotIndex < fileName.length() - 1) {
            return fileName.substring(lastDotIndex);
        }
        return ".jpg"; // Default extension
    }
    
    public Path getAbsolutePath(String relativePath) {
        return Paths.get(basePath, relativePath);
    }
    
    public boolean fileExists(String relativePath) {
        return Files.exists(getAbsolutePath(relativePath));
    }
    
    public static class FileStorageResult {
        private final String facePhotoPath;
        private final String signaturePath;
        private final boolean success;
        private final String errorMessage;
        
        private FileStorageResult(String facePhotoPath, String signaturePath, boolean success, String errorMessage) {
            this.facePhotoPath = facePhotoPath;
            this.signaturePath = signaturePath;
            this.success = success;
            this.errorMessage = errorMessage;
        }
        
        public static FileStorageResult success(String facePhotoPath, String signaturePath) {
            return new FileStorageResult(facePhotoPath, signaturePath, true, null);
        }
        
        public static FileStorageResult error(String errorMessage) {
            return new FileStorageResult(null, null, false, errorMessage);
        }
        
        // Getters
        public String getFacePhotoPath() { return facePhotoPath; }
        public String getSignaturePath() { return signaturePath; }
        public boolean isSuccess() { return success; }
        public String getErrorMessage() { return errorMessage; }
    }
    
    public static class FileStorageException extends RuntimeException {
        public FileStorageException(String message) {
            super(message);
        }
        
        public FileStorageException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}