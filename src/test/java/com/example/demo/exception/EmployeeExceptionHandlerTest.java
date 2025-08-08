//package com.example.demo.exception;
//
//import com.example.demo.dto.ErrorResponse;
//import com.example.demo.service.EmployeeService;
//import com.example.demo.service.FileStorageService;
//import org.junit.jupiter.api.BeforeEach;
//import org.junit.jupiter.api.Test;
//import org.junit.jupiter.api.extension.ExtendWith;
//import org.mockito.InjectMocks;
//import org.mockito.junit.jupiter.MockitoExtension;
//import org.springframework.http.HttpStatus;
//import org.springframework.http.ResponseEntity;
//import org.springframework.validation.BindingResult;
//import org.springframework.validation.FieldError;
//import org.springframework.web.bind.MethodArgumentNotValidException;
//import org.springframework.web.multipart.MaxUploadSizeExceededException;
//
//import java.util.Arrays;
//import java.util.List;
//
//import static org.assertj.core.api.Assertions.assertThat;
//import static org.mockito.Mockito.mock;
//import static org.mockito.Mockito.when;
//
//@ExtendWith(MockitoExtension.class)
//class EmployeeExceptionHandlerTest {
//
//    @InjectMocks
//    private EmployeeExceptionHandler exceptionHandler;
//
//    @Test
//    void handleIdentityCardDuplicate_ShouldReturnConflictStatus() {
//        EmployeeService.IdentityCardDuplicateException ex =
//            new EmployeeService.IdentityCardDuplicateException("Identity card already exists");
//
//        ResponseEntity<ErrorResponse> response = exceptionHandler.handleIdentityCardDuplicate(ex);
//
//        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
//        assertThat(response.getBody().getMessage()).isEqualTo("Identity card already exists");
//        assertThat(response.getBody().isSuccess()).isFalse();
//    }
//
//    @Test
//    void handleInvalidLocation_ShouldReturnBadRequestStatus() {
//        EmployeeService.InvalidLocationException ex =
//            new EmployeeService.InvalidLocationException("Invalid district");
//
//        ResponseEntity<ErrorResponse> response = exceptionHandler.handleInvalidLocation(ex);
//
//        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
//        assertThat(response.getBody().getMessage()).isEqualTo("Invalid district");
//        assertThat(response.getBody().isSuccess()).isFalse();
//    }
//
//    @Test
//    void handleEmployeeNotFound_ShouldReturnNotFoundStatus() {
//        EmployeeService.EmployeeNotFoundException ex =
//            new EmployeeService.EmployeeNotFoundException("Employee not found");
//
//        ResponseEntity<ErrorResponse> response = exceptionHandler.handleEmployeeNotFound(ex);
//
//        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
//        assertThat(response.getBody().getMessage()).isEqualTo("Employee not found");
//        assertThat(response.getBody().isSuccess()).isFalse();
//    }
//
//    @Test
//    void handleEmployeeCreation_ShouldReturnInternalServerErrorStatus() {
//        EmployeeService.EmployeeCreationException ex =
//            new EmployeeService.EmployeeCreationException("Database error", new RuntimeException());
//
//        ResponseEntity<ErrorResponse> response = exceptionHandler.handleEmployeeCreation(ex);
//
//        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
//        assertThat(response.getBody().getMessage()).isEqualTo("Failed to create employee: Database error");
//        assertThat(response.getBody().isSuccess()).isFalse();
//    }
//
//    @Test
//    void handleFileStorage_ShouldReturnInternalServerErrorStatus() {
//        FileStorageService.FileStorageException ex =
//            new FileStorageService.FileStorageException("File upload failed");
//
//        ResponseEntity<ErrorResponse> response = exceptionHandler.handleFileStorage(ex);
//
//        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
//        assertThat(response.getBody().getMessage()).isEqualTo("File upload failed: File upload failed");
//        assertThat(response.getBody().isSuccess()).isFalse();
//    }
//
//    @Test
//    void handleValidation_ShouldReturnBadRequestWithFieldErrors() {
//        MethodArgumentNotValidException ex = mock(MethodArgumentNotValidException.class);
//        BindingResult bindingResult = mock(BindingResult.class);
//
//        List<FieldError> fieldErrors = Arrays.asList(
//            new FieldError("employee", "name", "Name is required"),
//            new FieldError("employee", "email", "Email is invalid")
//        );
//
//        when(ex.getBindingResult()).thenReturn(bindingResult);
//        when(bindingResult.getFieldErrors()).thenReturn(fieldErrors);
//
//        ResponseEntity<ErrorResponse> response = exceptionHandler.handleValidation(ex);
//
//        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
//        assertThat(response.getBody().getMessage()).isEqualTo("Validation failed");
//        assertThat(response.getBody().getErrors()).hasSize(2);
//        assertThat(response.getBody().getErrors().get(0).getField()).isEqualTo("name");
//        assertThat(response.getBody().getErrors().get(0).getMessage()).isEqualTo("Name is required");
//    }
//
//    @Test
//    void handleMaxUploadSize_ShouldReturnPayloadTooLargeStatus() {
//        MaxUploadSizeExceededException ex = new MaxUploadSizeExceededException(5000000);
//
//        ResponseEntity<ErrorResponse> response = exceptionHandler.handleMaxUploadSize(ex);
//
//        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.PAYLOAD_TOO_LARGE);
//        assertThat(response.getBody().getMessage()).isEqualTo("File size exceeds the maximum allowed limit");
//        assertThat(response.getBody().isSuccess()).isFalse();
//    }
//
//    @Test
//    void handleIllegalArgument_ShouldReturnBadRequestStatus() {
//        IllegalArgumentException ex = new IllegalArgumentException("Invalid parameter");
//
//        ResponseEntity<ErrorResponse> response = exceptionHandler.handleIllegalArgument(ex);
//
//        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
//        assertThat(response.getBody().getMessage()).isEqualTo("Invalid request: Invalid parameter");
//        assertThat(response.getBody().isSuccess()).isFalse();
//    }
//
//    @Test
//    void handleGeneral_ShouldReturnInternalServerErrorStatus() {
//        RuntimeException ex = new RuntimeException("Unexpected error");
//
//        ResponseEntity<ErrorResponse> response = exceptionHandler.handleGeneral(ex);
//
//        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
//        assertThat(response.getBody().getMessage()).isEqualTo("An unexpected error occurred. Please try again later.");
//        assertThat(response.getBody().isSuccess()).isFalse();
//    }
//}