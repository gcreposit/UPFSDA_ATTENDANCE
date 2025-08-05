package com.example.demo.serviceimpl;

import com.example.demo.dto.EmployeeRequest;
import com.example.demo.dto.EmployeeResponse;
import com.example.demo.entity.Employee;
import com.example.demo.repository.EmployeeRepository;
import com.example.demo.service.EmployeeService;
import com.example.demo.service.FileStorageService;
import com.example.demo.service.FaceRecognitionService;
import com.example.demo.service.LocationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmployeeServiceImpl implements EmployeeService {
    
    private final EmployeeRepository employeeRepository;
    private final FileStorageService fileStorageService;
    private final FaceRecognitionService faceRecognitionService;
    private final LocationService locationService;
    
    @Override
    @Transactional
    public EmployeeResponse createEmployee(EmployeeRequest request) {
        try {
            log.info("Creating employee with identity card: {}", request.getIdentityCardNo());
            
            // Validate identity card uniqueness
            if (!isIdentityCardUnique(request.getIdentityCardNo())) {
                log.warn("Identity card number already exists: {}", request.getIdentityCardNo());
                throw new IdentityCardDuplicateException("Identity card number already exists: " + request.getIdentityCardNo());
            }
            
            // Validate location data
            validateLocationData(request.getDistrict(), request.getTehsil());
            
            // Store files
            FileStorageService.FileStorageResult fileResult = fileStorageService.storeEmployeeFiles(
                request.getName(), 
                request.getUploadFacePhoto(), 
                request.getUploadSignature()
            );
            
            if (!fileResult.isSuccess()) {
                throw new FileStorageService.FileStorageException("Failed to store employee files: " + fileResult.getErrorMessage());
            }
            
            // Create and save employee
            Employee employee = mapRequestToEmployee(request, fileResult);
            Employee savedEmployee = employeeRepository.save(employee);
            
            log.info("Successfully created employee with ID: {} and identity card: {}", 
                savedEmployee.getId(), savedEmployee.getIdentityCardNo());
            
            // Trigger face recognition API call asynchronously
            faceRecognitionService.sendToFaceRecognitionAPI(
                savedEmployee.getIdentityCardNo(),
                savedEmployee.getName(),
                savedEmployee.getUploadFacePhotoImgPath()
            );
            
            return EmployeeResponse.success(
                savedEmployee.getId(),
                savedEmployee.getName(),
                savedEmployee.getIdentityCardNo(),
                "Employee created successfully"
            );
            
        } catch (IdentityCardDuplicateException | FileStorageService.FileStorageException e) {
            log.error("Failed to create employee: {}", e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("Unexpected error creating employee: {}", e.getMessage(), e);
            throw new EmployeeCreationException("Failed to create employee: " + e.getMessage(), e);
        }
    }
    
    @Override
    public boolean isIdentityCardUnique(String identityCardNo) {
        if (identityCardNo == null || identityCardNo.trim().isEmpty()) {
            return false;
        }
        return !employeeRepository.existsByIdentityCardNo(identityCardNo.trim());
    }
    
    @Override
    public Employee findByIdentityCardNo(String identityCardNo) {
        return employeeRepository.findByIdentityCardNo(identityCardNo)
            .orElseThrow(() -> new EmployeeNotFoundException("Employee not found with identity card: " + identityCardNo));
    }
    
    @Override
    public Employee findById(Long id) {
        return employeeRepository.findById(id)
            .orElseThrow(() -> new EmployeeNotFoundException("Employee not found with ID: " + id));
    }
    
    private void validateLocationData(String district, String tehsil) {
        if (!locationService.isValidDistrict(district)) {
            throw new InvalidLocationException("Invalid district: " + district);
        }
        
        if (!locationService.isValidTehsilForDistrict(district, tehsil)) {
            throw new InvalidLocationException("Invalid tehsil '" + tehsil + "' for district '" + district + "'");
        }
    }
    
    private Employee mapRequestToEmployee(EmployeeRequest request, FileStorageService.FileStorageResult fileResult) {
        return Employee.builder()
                // Basic information
                .name(request.getName())
                .identityCardNo(request.getIdentityCardNo())
                .dateOfBirth(convertDateFormat(request.getDateOfBirth()))
                .assignedLocation(request.getAssignedLocation())
                .district(request.getDistrict())
                .tehsil(request.getTehsil())
                
                // Optional fields
                .mobileNumber(request.getMobileNumber())
                .bloodGroup(request.getBloodGroup())
                .emailAddress(request.getEmailAddress())
                .emergencyContactNo(request.getEmergencyContactNo())
                .labName(request.getLabName())
                .officeName(request.getOfficeName())
                
                // Address fields
                .officeAddress(request.getAddress())
                .permanantAddress(request.getAddress()) // Using same address for now
                
                // File paths
                .uploadFacePhotoImgPath(fileResult.getFacePhotoPath())
                .uploadSignatureImgPath(fileResult.getSignaturePath())
                
                .build();
    }
    
    private String convertDateFormat(String dateOfBirth) {
        if (dateOfBirth == null || dateOfBirth.trim().isEmpty()) {
            return dateOfBirth;
        }
        
        try {
            // Check if it's already in dd/MM/yyyy format
            if (dateOfBirth.matches("\\d{2}/\\d{2}/\\d{4}")) {
                return dateOfBirth;
            }
            
            // Check if it's in YYYY-MM-DD format (from HTML5 date input)
            if (dateOfBirth.matches("\\d{4}-\\d{2}-\\d{2}")) {
                String[] parts = dateOfBirth.split("-");
                return parts[2] + "/" + parts[1] + "/" + parts[0]; // Convert to dd/MM/yyyy
            }
            
            // If neither format matches, return as is
            return dateOfBirth;
            
        } catch (Exception e) {
            log.warn("Error converting date format for: {}", dateOfBirth, e);
            return dateOfBirth;
        }
    }
    
    // Custom exceptions
    public static class IdentityCardDuplicateException extends RuntimeException {
        public IdentityCardDuplicateException(String message) {
            super(message);
        }
    }
    
    public static class InvalidLocationException extends RuntimeException {
        public InvalidLocationException(String message) {
            super(message);
        }
    }
    
    public static class EmployeeCreationException extends RuntimeException {
        public EmployeeCreationException(String message, Throwable cause) {
            super(message, cause);
        }
    }
    
    public static class EmployeeNotFoundException extends RuntimeException {
        public EmployeeNotFoundException(String message) {
            super(message);
        }
    }
}