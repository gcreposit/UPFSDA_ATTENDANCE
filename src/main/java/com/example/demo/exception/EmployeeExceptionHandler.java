package com.example.demo.exception;

import com.example.demo.dto.ErrorResponse;
import com.example.demo.serviceimpl.EmployeeServiceImpl;
import com.example.demo.service.FileStorageService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

import java.util.List;
import java.util.stream.Collectors;

@ControllerAdvice
@Slf4j
public class EmployeeExceptionHandler {

    @ExceptionHandler(EmployeeServiceImpl.IdentityCardDuplicateException.class)
    public ResponseEntity<ErrorResponse> handleIdentityCardDuplicate(EmployeeServiceImpl.IdentityCardDuplicateException ex) {
        log.warn("Identity card duplicate error: {}", ex.getMessage());
        ErrorResponse errorResponse = ErrorResponse.of(ex.getMessage());
        return ResponseEntity.status(HttpStatus.CONFLICT).body(errorResponse);
    }

    @ExceptionHandler(EmployeeServiceImpl.InvalidLocationException.class)
    public ResponseEntity<ErrorResponse> handleInvalidLocation(EmployeeServiceImpl.InvalidLocationException ex) {
        log.warn("Invalid location error: {}", ex.getMessage());
        ErrorResponse errorResponse = ErrorResponse.of(ex.getMessage());
        return ResponseEntity.badRequest().body(errorResponse);
    }

    @ExceptionHandler(EmployeeServiceImpl.EmployeeNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleEmployeeNotFound(EmployeeServiceImpl.EmployeeNotFoundException ex) {
        log.warn("Employee not found: {}", ex.getMessage());
        ErrorResponse errorResponse = ErrorResponse.of(ex.getMessage());
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorResponse);
    }

    @ExceptionHandler(EmployeeServiceImpl.EmployeeCreationException.class)
    public ResponseEntity<ErrorResponse> handleEmployeeCreation(EmployeeServiceImpl.EmployeeCreationException ex) {
        log.error("Employee creation error: {}", ex.getMessage(), ex);
        ErrorResponse errorResponse = ErrorResponse.of("Failed to create employee: " + ex.getMessage());
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
    }

    @ExceptionHandler(FileStorageService.FileStorageException.class)
    public ResponseEntity<ErrorResponse> handleFileStorage(FileStorageService.FileStorageException ex) {
        log.error("File storage error: {}", ex.getMessage(), ex);
        ErrorResponse errorResponse = ErrorResponse.of("File upload failed: " + ex.getMessage());
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException ex) {
        log.warn("Validation error: {}", ex.getMessage());

        List<ErrorResponse.FieldError> fieldErrors = ex.getBindingResult().getFieldErrors().stream()
                .map(error -> new ErrorResponse.FieldError(error.getField(), error.getDefaultMessage()))
                .collect(Collectors.toList());

        ErrorResponse errorResponse = ErrorResponse.of("Validation failed", fieldErrors);
        return ResponseEntity.badRequest().body(errorResponse);
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<ErrorResponse> handleMaxUploadSize(MaxUploadSizeExceededException ex) {
        log.warn("File upload size exceeded: {}", ex.getMessage());
        ErrorResponse errorResponse = ErrorResponse.of("File size exceeds the maximum allowed limit");
        return ResponseEntity.status(HttpStatus.PAYLOAD_TOO_LARGE).body(errorResponse);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArgument(IllegalArgumentException ex) {
        log.warn("Invalid argument: {}", ex.getMessage());
        ErrorResponse errorResponse = ErrorResponse.of("Invalid request: " + ex.getMessage());
        return ResponseEntity.badRequest().body(errorResponse);
    }

//    @ExceptionHandler(Exception.class)
//    public ResponseEntity<ErrorResponse> handleGeneral(Exception ex) {
//        log.error("Unexpected error: {}", ex.getMessage(), ex);
//        ErrorResponse errorResponse = ErrorResponse.of("An unexpected error occurred. Please try again later.");
//        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
//    }
}