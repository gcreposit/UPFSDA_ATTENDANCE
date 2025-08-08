package com.example.demo.controller;

import com.example.demo.dto.ApiResponse;
import com.example.demo.dto.EmployeeRequest;
import com.example.demo.dto.EmployeeResponse;
import com.example.demo.dto.ErrorResponse;
import com.example.demo.entity.Attendance;
import com.example.demo.entity.Employee;
import com.example.demo.service.AttendanceService;
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
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/data")
@RequiredArgsConstructor
@Slf4j
public class DataApiController {

    private final EmployeeService employeeService;
    private final LocationService locationService;
    private final AttendanceService attendanceService;

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

        public boolean isUnique() {
            return unique;
        }

        public String getMessage() {
            return message;
        }
    }


    //Api For Mark Attendance (In This Also Field Image accept for WFF)
    @PostMapping("/mark-attendance")
    public ResponseEntity<ApiResponse<Object>> markAttendance(
            @RequestParam String userName,
            @RequestParam MultipartFile image,
            @RequestParam String time,
            @RequestParam String attendanceType,
            @RequestParam(required = false) String reason,
            @RequestParam(required = false) MultipartFile[] fieldImage
    ) {
        try {
            // Validate reason if attendanceType is WFH or WFF
            if ((attendanceType != null && (attendanceType.equalsIgnoreCase("WFH") || attendanceType.equalsIgnoreCase("WFF")))
                    && (reason == null || reason.trim().isEmpty())) {
                Map<String, Object> errorData = new HashMap<>();
                errorData.put("error", "Reason is required when attendanceType is WFH or WFF");

                return ResponseEntity.badRequest().body(
                        ApiResponse.builder()
                                .username(userName)
                                .message("Validation error")
                                .statusCode(HttpStatus.BAD_REQUEST.value())
                                .data(errorData)
                                .build()
                );
            }

            Attendance attendance = attendanceService.saveAttendance(
                    userName, image, time, attendanceType, reason, fieldImage
            );

            Map<String, Object> responseData = new HashMap<>();
            responseData.put("attendanceId", attendance.getId());
            responseData.put("username", attendance.getUserName());
            responseData.put("morningTime", attendance.getMorningTime());
            responseData.put("eveningTime", attendance.getEveningTime());

            return ResponseEntity.ok(
                    ApiResponse.builder()
                            .username(userName)
                            .message("Attendance marked successfully")
                            .statusCode(HttpStatus.OK.value())
                            .data(responseData)
                            .build()
            );

        } catch (Exception e) {
            Map<String, Object> errorData = new HashMap<>();
            errorData.put("error", e.getMessage());

            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                    ApiResponse.builder()
                            .username(userName)
                            .message("Error processing attendance request")
                            .statusCode(HttpStatus.INTERNAL_SERVER_ERROR.value())
                            .data(errorData)
                            .build()
            );
        }
    }

    //Api For Fetch Work Types
    @GetMapping("/work-types")
    public ResponseEntity<ApiResponse<Object>> getWorkTypes() {

        try {
            List<String> workTypes = employeeService.getDistinctWorkTypes();

            if (workTypes == null || workTypes.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(
                        ApiResponse.builder()
                                .message("No work types found")
                                .statusCode(HttpStatus.NOT_FOUND.value())
                                .data(Collections.emptyMap())
                                .build()
                );
            }

            Map<String, Object> data = new HashMap<>();
            data.put("workTypes", workTypes);

            return ResponseEntity.ok(
                    ApiResponse.builder()
                            .message("Fetched distinct work types successfully")
                            .statusCode(HttpStatus.OK.value())
                            .data(data)
                            .build()
            );

        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("error", e.getMessage());

            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                    ApiResponse.builder()
                            .message("Failed to fetch work types")
                            .statusCode(HttpStatus.INTERNAL_SERVER_ERROR.value())
                            .data(error)
                            .build()
            );
        }
    }

    //Api For Fetch Dashboard Data (Today Reports)
    @PostMapping("/dashboard")
    public ResponseEntity<ApiResponse<Attendance>> getDashboard(
            @RequestParam String userName,
            @RequestParam String date
    ) {
        try {
            Attendance attendance = attendanceService.getDashboardData(userName, date);

            if (attendance == null) {
                // ✅ No data found case
                return ResponseEntity.status(HttpStatus.OK).body(
                        ApiResponse.<Attendance>builder()
                                .message("No Data Available")
                                .username(userName)
                                .statusCode(HttpStatus.OK.value())
                                .data(null)
                                .build()
                );
            }

            return ResponseEntity.ok(
                    ApiResponse.<Attendance>builder()
                            .message("Dashboard data fetched successfully.")
                            .username(userName)
                            .statusCode(HttpStatus.OK.value())
                            .data(attendance)
                            .build()
            );
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
                    ApiResponse.<Attendance>builder()
                            .message(e.getMessage())
                            .statusCode(HttpStatus.BAD_REQUEST.value())
                            .data(null)
                            .build()
            );
        } catch (NoSuchElementException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(
                    ApiResponse.<Attendance>builder()
                            .message(e.getMessage())
                            .statusCode(HttpStatus.NOT_FOUND.value())
                            .data(null)
                            .build()
            );
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                    ApiResponse.<Attendance>builder()
                            .message("Internal server error: " + e.getMessage())
                            .statusCode(HttpStatus.INTERNAL_SERVER_ERROR.value())
                            .data(null)
                            .build()
            );
        }
    }

    //    Api For Save Location Tracking For WFF
    @PostMapping("/location-tracking")
    public ResponseEntity<ApiResponse<Object>> trackLocation(
            @RequestParam String userName,
            @RequestParam String lat,
            @RequestParam String lon,
            @RequestParam String timestamp
    ) {
        try {
            ApiResponse<Object> response = attendanceService.saveLocationForTracking(userName, lat, lon, timestamp);

            if (response.getStatusCode() != HttpStatus.OK.value()) {
                return ResponseEntity.status(response.getStatusCode()).body(response);
            }

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("error", e.getMessage());

            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                    ApiResponse.builder()
                            .message("Internal server error while saving location")
                            .statusCode(HttpStatus.INTERNAL_SERVER_ERROR.value())
                            .data(error)
                            .build()
            );
        }
    }

}