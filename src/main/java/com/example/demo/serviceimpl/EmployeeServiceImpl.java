package com.example.demo.serviceimpl;

import com.example.demo.dto.EmployeeRequest;
import com.example.demo.dto.EmployeeResponse;
import com.example.demo.entity.Employee;
import com.example.demo.repository.EmployeeRepository;
import com.example.demo.repository.WorkTypesRepository;
import com.example.demo.service.EmployeeService;
import com.example.demo.service.FileStorageService;
import com.example.demo.service.FaceRecognitionService;
import com.example.demo.service.LocationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmployeeServiceImpl implements EmployeeService {

    private final EmployeeRepository employeeRepository;
    private final FileStorageService fileStorageService;
    private final FaceRecognitionService faceRecognitionService;
    private final LocationService locationService;
    private final WorkTypesRepository workTypesRepository;

    @Override
    @Transactional
    public EmployeeResponse createEmployee(EmployeeRequest request) {

        try {
            log.info("Creating employee with identity card: {}", request.getIdentityCardNo());

            // Validate location data
            validateLocationData(request.getDistrict(), request.getTehsil());

            // Store files
            FileStorageService.FileStorageResult fileResult = fileStorageService.storeEmployeeFiles(
                    request.getName(),
                    request.getUploadFacePhoto()
            );

            if (!fileResult.isSuccess()) {
                throw new FileStorageService.FileStorageException("Failed to store employee files: " + fileResult.getErrorMessage());
            }

            // Create and save employee
            Employee employee = mapRequestToEmployee(request, fileResult);
            String idCardNo = employee.getIdentityCardNo();
            String name = request.getName();

            String username = idCardNo + '_' + name;
            employee.setUsername(username);

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

    @Override
    public List<String> getDistinctWorkTypes() {

        return workTypesRepository.fetchAllWorkTypes();

    }

    @Override
    public List<Employee> findAllEmployeeDetails() {

        LocalDate today = LocalDate.now();

        return employeeRepository.findAllEmployeeDetailsForToday(today);

    }

    private void validateLocationData(String district, String tehsil) {
        if (!locationService.isValidDistrict(district)) {
            throw new InvalidLocationException("Invalid district: " + district);
        }

        if (!locationService.isValidTehsilForDistrict(district, tehsil)) {
            throw new InvalidLocationException("Invalid tehsil '" + tehsil + "' for district '" + district + "'");
        }
    }

//    private String generateUniqueIdentityCardNo() {
//
//        return UUID.randomUUID().toString().substring(0, 8).toUpperCase();
//    }

    private Employee mapRequestToEmployee(EmployeeRequest request, FileStorageService.FileStorageResult fileResult) {
        return Employee.builder()
                // Basic information
                .name(request.getName())
                .identityCardNo(request.getIdentityCardNo()) // auto-generated UUID
                .dateOfBirth(convertDateFormat(request.getDateOfBirth()))
                .post(request.getPost())
                .district(request.getDistrict())
                .tehsil(request.getTehsil())
                .homeLocation(request.getHomeLocation())
                .designation(generateUniqueDesignation(request.getDistrict(), request.getPost()))

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

                .build();
    }

    private String generateUniqueDesignation(String district, String post) {

        int counter = 1;
        String designation;

        do {
            designation = district + "-" + post + "-" + counter;
            counter++;
        } while (employeeRepository.existsByDesignation(designation));

        return designation;
    }

    private String convertDateFormat(String dateOfBirth) {
        if (dateOfBirth == null || dateOfBirth.trim().isEmpty()) {
            return null; // No date provided
        }

        try {
            // Convert only if in YYYY-MM-DD format
            if (dateOfBirth.contains("-")) {
                String[] parts = dateOfBirth.split("-");
                if (parts.length == 3) {
                    return parts[2] + "/" + parts[1] + "/" + parts[0];
                }
            }
            // Otherwise return original
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