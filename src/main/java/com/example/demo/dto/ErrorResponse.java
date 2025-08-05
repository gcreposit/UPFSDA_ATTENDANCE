package com.example.demo.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ErrorResponse {
    
    private boolean success;
    private String message;
    private List<FieldError> errors;
    
    @Data
    @AllArgsConstructor
    public static class FieldError {
        private String field;
        private String message;
    }
    
    public static ErrorResponse of(String message) {
        return new ErrorResponse(false, message, null);
    }
    
    public static ErrorResponse of(String message, List<FieldError> errors) {
        return new ErrorResponse(false, message, errors);
    }
}