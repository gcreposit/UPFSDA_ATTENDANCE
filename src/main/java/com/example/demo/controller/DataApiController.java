package com.example.demo.controller;

import com.example.demo.dto.EmployeeRequest;
import com.example.demo.dto.EmployeeResponse;
import com.example.demo.dto.ErrorResponse;
import com.example.demo.service.EmployeeService;
import com.example.demo.service.FileStorageService;
import com.example.demo.service.LocationService;
import com.example.demo.serviceimpl.EmployeeServiceImpl;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/data")
@RequiredArgsConstructor
@Slf4j
public class DataApiController {
    
    private final EmployeeService employeeService;
    private final LocationService locationService;

//    FOR SAVING EMPLOYEE DATA (DTO USE KIYA INSTEAD OF MAIN ENTITY)
    @PostMapping("/employees")
    public ResponseEntity<?> createEmployee(@Valid @ModelAttribute EmployeeRequest request, BindingResult bindingResult) {
        log.info("Received request to create employee with identity card: {}", request.getIdentityCardNo());
        
        try {
            // Check for validation errors
            if (bindingResult.hasErrors()) {
                List<ErrorResponse.FieldError> fieldErrors = bindingResult.getFieldErrors().stream()
                    .map(error -> new ErrorResponse.FieldError(error.getField(), error.getDefaultMessage()))
                    .collect(Collectors.toList());
                
                ErrorResponse errorResponse = ErrorResponse.of("Validation failed", fieldErrors);
                return ResponseEntity.badRequest().body(errorResponse);
            }
            
            //--------------After A To Create employee--------
            EmployeeResponse response = employeeService.createEmployee(request);
            log.info("Successfully created employee with ID: {}", response.getId());
            
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
            
        } catch (EmployeeServiceImpl.IdentityCardDuplicateException e) {
            log.warn("Identity card duplicate error: {}", e.getMessage());
            ErrorResponse errorResponse = ErrorResponse.of(e.getMessage());
            return ResponseEntity.status(HttpStatus.CONFLICT).body(errorResponse);
            
        } catch (EmployeeServiceImpl.InvalidLocationException e) {
            log.warn("Invalid location error: {}", e.getMessage());
            ErrorResponse errorResponse = ErrorResponse.of(e.getMessage());
            return ResponseEntity.badRequest().body(errorResponse);
            
        } catch (FileStorageService.FileStorageException e) {
            log.error("File storage error: {}", e.getMessage());
            ErrorResponse errorResponse = ErrorResponse.of("File upload failed: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
            
        } catch (Exception e) {
            log.error("Unexpected error creating employee: {}", e.getMessage(), e);
            ErrorResponse errorResponse = ErrorResponse.of("An unexpected error occurred while creating the employee");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }
    
    @GetMapping("/districts")
    public ResponseEntity<?> getDistricts() {
        log.debug("Received request to get all districts");
        
        try {
            List<String> districts = locationService.getAllDistricts();
            log.debug("Returning {} districts", districts.size());
            
            return ResponseEntity.ok(districts);
            
        } catch (Exception e) {
            log.error("Error fetching districts: {}", e.getMessage(), e);
            ErrorResponse errorResponse = ErrorResponse.of("Failed to fetch districts");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }
    
    @GetMapping("/tehsils")
    public ResponseEntity<?> getTehsils(@RequestParam String district) {
        log.debug("Received request to get tehsils for district: {}", district);
        
        try {
            if (district == null || district.trim().isEmpty()) {
                ErrorResponse errorResponse = ErrorResponse.of("District parameter is required");
                return ResponseEntity.badRequest().body(errorResponse);
            }
            
            List<String> tehsils = locationService.getTehsilsByDistrict(district.trim());
            log.debug("Returning {} tehsils for district: {}", tehsils.size(), district);
            
            return ResponseEntity.ok(tehsils);
            
        } catch (Exception e) {
            log.error("Error fetching tehsils for district {}: {}", district, e.getMessage(), e);
            ErrorResponse errorResponse = ErrorResponse.of("Failed to fetch tehsils for district: " + district);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }
    
    @GetMapping("/employees/{id}")
    public ResponseEntity<?> getEmployee(@PathVariable Long id) {
        log.debug("Received request to get employee with ID: {}", id);
        
        try {
            var employee = employeeService.findById(id);
            return ResponseEntity.ok(employee);
            
        } catch (EmployeeServiceImpl.EmployeeNotFoundException e) {
            log.warn("Employee not found: {}", e.getMessage());
            ErrorResponse errorResponse = ErrorResponse.of(e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorResponse);
            
        } catch (Exception e) {
            log.error("Error fetching employee with ID {}: {}", id, e.getMessage(), e);
            ErrorResponse errorResponse = ErrorResponse.of("Failed to fetch employee");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }
    
    @GetMapping("/employees/identity/{identityCardNo}")
    public ResponseEntity<?> getEmployeeByIdentityCard(@PathVariable String identityCardNo) {
        log.debug("Received request to get employee with identity card: {}", identityCardNo);
        
        try {
            var employee = employeeService.findByIdentityCardNo(identityCardNo);
            return ResponseEntity.ok(employee);
            
        } catch (EmployeeServiceImpl.EmployeeNotFoundException e) {
            log.warn("Employee not found: {}", e.getMessage());
            ErrorResponse errorResponse = ErrorResponse.of(e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorResponse);
            
        } catch (Exception e) {
            log.error("Error fetching employee with identity card {}: {}", identityCardNo, e.getMessage(), e);
            ErrorResponse errorResponse = ErrorResponse.of("Failed to fetch employee");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }
    
    @GetMapping("/employees/check-identity/{identityCardNo}")
    public ResponseEntity<?> checkIdentityCardUniqueness(@PathVariable String identityCardNo) {
        log.debug("Checking uniqueness for identity card: {}", identityCardNo);
        
        try {
            boolean isUnique = employeeService.isIdentityCardUnique(identityCardNo);
            return ResponseEntity.ok(new IdentityCheckResponse(isUnique, 
                isUnique ? "Identity card is available" : "Identity card already exists"));
            
        } catch (Exception e) {
            log.error("Error checking identity card uniqueness: {}", e.getMessage(), e);
            ErrorResponse errorResponse = ErrorResponse.of("Failed to check identity card uniqueness");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }
    
    // Helper class for identity check response
    public static class IdentityCheckResponse {
        private final boolean unique;
        private final String message;
        
        public IdentityCheckResponse(boolean unique, String message) {
            this.unique = unique;
            this.message = message;
        }
        
        public boolean isUnique() { return unique; }
        public String getMessage() { return message; }
    }
}