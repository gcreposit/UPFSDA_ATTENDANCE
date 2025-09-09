package com.example.demo.controller;

import com.example.demo.dto.*;
import com.example.demo.entity.*;
import com.example.demo.service.AttendanceService;
import com.example.demo.service.EmployeeService;
import com.example.demo.service.FileStorageService;
import com.example.demo.service.LocationService;
import com.example.demo.serviceimpl.EmployeeServiceImpl;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.HandlerAdapter;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
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

    @Value("${file.storage.path}")
    private String uploadPath;

    //    FOR SAVING EMPLOYEE DATA (DTO USE KIYA INSTEAD OF MAIN ENTITY)
    @PostMapping("/employees")
    public ResponseEntity<?> createEmployee(@Valid @ModelAttribute EmployeeRequest request, BindingResult bindingResult) {

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

            return ResponseEntity.status(HttpStatus.CREATED).body(response);

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

    //Api For Mark Attendance
    @PostMapping("/mark-attendance")
    public ResponseEntity<ApiResponse<Object>> markAttendance(
            @RequestParam String userName,
            @RequestParam MultipartFile image,
            @RequestParam String attendanceType,
            @RequestParam(required = false) String reason
    ) {
        try {

            Attendance attendance = attendanceService.saveAttendance(
                    userName, image, attendanceType, reason
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

    //    Api For Submit Field Image When AttendanceType is WFF

    @PostMapping("/attendance/field-images")
    public ResponseEntity<ApiResponse> uploadFieldImages(
            @RequestParam String username,
            @RequestParam("fieldImage") MultipartFile fieldImage,
            @RequestParam(value = "fieldImage1", required = false) MultipartFile fieldImage1) {

        try {
            String message = attendanceService.uploadFieldImages(username, fieldImage, fieldImage1);

            return ResponseEntity.ok(
                    ApiResponse.builder()
                            .username(username)
                            .message(message)
                            .statusCode(HttpStatus.OK.value())
                            .data(null)
                            .build()
            );
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(
                    ApiResponse.builder()
                            .username(username)
                            .message(e.getMessage())
                            .statusCode(HttpStatus.BAD_REQUEST.value())
                            .data(null)
                            .build()
            );
        } catch (Exception e) {
            Map<String, Object> errorData = new HashMap<>();
            errorData.put("error", e.getMessage());

            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                    ApiResponse.builder()
                            .username(username)
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

//    //Api For Fetch Dashboard Data (Today Reports)
//    @PostMapping("/dashboard")
//    public ResponseEntity<ApiResponse<Attendance>> getDashboard(
//            @RequestParam String userName,
//            @RequestParam String date
//    ) {
//        try {
//            Attendance attendance = attendanceService.getDashboardData(userName, date);
//
//            if (attendance == null) {
//                // ✅ No data found case
//                return ResponseEntity.status(HttpStatus.OK).body(
//                        ApiResponse.<Attendance>builder()
//                                .message("No Data Available")
//                                .username(userName)
//                                .statusCode(HttpStatus.OK.value())
//                                .data(null)
//                                .build()
//                );
//            }
//
//            return ResponseEntity.ok(
//                    ApiResponse.<Attendance>builder()
//                            .message("Dashboard data fetched successfully.")
//                            .username(userName)
//                            .statusCode(HttpStatus.OK.value())
//                            .data(attendance)
//                            .build()
//            );
//        } catch (IllegalArgumentException e) {
//            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
//                    ApiResponse.<Attendance>builder()
//                            .message(e.getMessage())
//                            .statusCode(HttpStatus.BAD_REQUEST.value())
//                            .data(null)
//                            .build()
//            );
//        } catch (NoSuchElementException e) {
//            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(
//                    ApiResponse.<Attendance>builder()
//                            .message(e.getMessage())
//                            .statusCode(HttpStatus.NOT_FOUND.value())
//                            .data(null)
//                            .build()
//            );
//        } catch (Exception e) {
//            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
//                    ApiResponse.<Attendance>builder()
//                            .message("Internal server error: " + e.getMessage())
//                            .statusCode(HttpStatus.INTERNAL_SERVER_ERROR.value())
//                            .data(null)
//                            .build()
//            );
//        }
//    }

    @PostMapping("/dashboard")
    public ResponseEntity<ApiResponse<DashboardResponse>> getDashboard(
            @RequestParam String userName,
            @RequestParam String date
    ) {
        try {
            DashboardResponse dashboard = attendanceService.getDashboardData(userName, date);

            if (dashboard == null) {
                return ResponseEntity.ok(
                        ApiResponse.<DashboardResponse>builder()
                                .message("No Data Available")
                                .username(userName)
                                .statusCode(HttpStatus.OK.value())
                                .data(null)
                                .build()
                );
            }

            return ResponseEntity.ok(
                    ApiResponse.<DashboardResponse>builder()
                            .message("Dashboard data fetched successfully.")
                            .username(userName)
                            .statusCode(HttpStatus.OK.value())
                            .data(dashboard)
                            .build()
            );

        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(
                    ApiResponse.<DashboardResponse>builder()
                            .message(e.getMessage())
                            .statusCode(HttpStatus.BAD_REQUEST.value())
                            .data(null)
                            .build()
            );
        } catch (NoSuchElementException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(
                    ApiResponse.<DashboardResponse>builder()
                            .message(e.getMessage())
                            .statusCode(HttpStatus.NOT_FOUND.value())
                            .data(null)
                            .build()
            );
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                    ApiResponse.<DashboardResponse>builder()
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
            @RequestParam(required = false) String lat,
            @RequestParam(required = false) String lon,
            @RequestParam(required = false) String timestamp,
            @RequestParam boolean isActive
    ) {
        try {
            ApiResponse<Object> response = attendanceService.saveLocationForTracking(userName, lat, lon, timestamp, isActive);

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

    //    Api For Dashboard Monthly Counts
    @PostMapping("/dashboard/monthly")
    public ResponseEntity<Map<String, Object>> getMonthlyRecordCount(
            @RequestParam("username") String username,
            @RequestParam("year") int year,
            @RequestParam("month") int month) {

        Map<String, Object> response = attendanceService.getMonthlyAttendanceCount(username, year, month);
        return ResponseEntity.ok(response);

    }

    //    Api For Show Dashboard Data For Admin
    @GetMapping("/dashboard-stats-admin")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getDashboardStatistics() {
        try {
            // Fetch data from the service layer, which returns an Optional.
            Optional<Map<String, Object>> dashboardDataOptional =
                    attendanceService.getDashboardDataForAdmin();

            if (dashboardDataOptional.isEmpty()) {
                // ✅ Case: No data found. Return a 200 OK with a specific message.
                // Note the correct generic type <Map<String, Object>>
                return ResponseEntity.ok(
                        ApiResponse.<Map<String, Object>>builder()
                                .message("No Data Available for the admin dashboard.")
                                .statusCode(HttpStatus.OK.value())
                                .data(null)
                                .build()
                );
            }

            // Case: Data fetched successfully.
            return ResponseEntity.ok(
                    ApiResponse.<Map<String, Object>>builder()
                            .message("Admin dashboard data fetched successfully.")
                            .statusCode(HttpStatus.OK.value())
                            .data(dashboardDataOptional.get())
                            .build()
            );
        } catch (IllegalArgumentException e) {
            // Handle invalid arguments.
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
                    ApiResponse.<Map<String, Object>>builder()
                            .message("Bad Request: " + e.getMessage())
                            .statusCode(HttpStatus.BAD_REQUEST.value())
                            .data(null)
                            .build()
            );
        } catch (NoSuchElementException e) {
            // Handle cases where a required resource is not found.
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(
                    ApiResponse.<Map<String, Object>>builder()
                            .message("Resource Not Found: " + e.getMessage())
                            .statusCode(HttpStatus.NOT_FOUND.value())
                            .data(null)
                            .build()
            );
        } catch (Exception e) {
            // Catch-all for any other unexpected server errors.
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                    ApiResponse.<Map<String, Object>>builder()
                            .message("Internal server error: " + e.getMessage())
                            .statusCode(HttpStatus.INTERNAL_SERVER_ERROR.value())
                            .data(null)
                            .build()
            );
        }
    }

    //    Api To Serve Attendance Image
    @GetMapping("/attendance/image/{id}")
    public ResponseEntity<byte[]> getAttendanceImage(
            @PathVariable Long id,
            @RequestParam(name = "type", defaultValue = "morning") String type) throws IOException {

        Optional<Attendance> optionalAttendance = attendanceService.findById(id);

        if (optionalAttendance.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        Attendance attendance = optionalAttendance.get();

        String imagePath;

        if ("morning".equalsIgnoreCase(type)) {
            imagePath = attendance.getMorningImagePath();
        } else if ("evening".equalsIgnoreCase(type)) {
            imagePath = attendance.getEveningImagePath();
        } else {
            return ResponseEntity.badRequest().build();
        }

        if (imagePath == null || imagePath.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        // Combine base directory from properties and imagePath
        Path path = Paths.get(uploadPath, imagePath);

        if (!Files.exists(path)) {
            return ResponseEntity.notFound().build();
        }

        byte[] imageBytes = Files.readAllBytes(path);
        String contentType = Files.probeContentType(path);

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(contentType != null ? contentType : "application/octet-stream"))
                .body(imageBytes);
    }

    // API to Serve Employee Profile Image
    @GetMapping("/employee/profile/image/{id}")
    public ResponseEntity<byte[]> getProfileImage(@PathVariable Long id) throws IOException {

        Employee employee = employeeService.findById(id); // assuming this returns null if not found

        if (employee == null) {
            return ResponseEntity.notFound().build();
        }

        // Get profile image path from entity
        String imagePath = employee.getUploadFacePhotoImgPath();

        if (imagePath == null || imagePath.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        // Combine base directory from properties and imagePath
        Path path = Paths.get(uploadPath, imagePath);

        if (!Files.exists(path)) {
            return ResponseEntity.notFound().build();
        }

        byte[] imageBytes = Files.readAllBytes(path);
        String contentType = Files.probeContentType(path);

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(contentType != null ? contentType : "application/octet-stream"))
                .body(imageBytes);
    }

    // API to serve field images
    @GetMapping("/attendance/field-images/{id}")
    public ResponseEntity<Map<String, String>> getFieldImages(@PathVariable Long id) throws IOException {

        Optional<Attendance> optionalAttendance = attendanceService.findById(id);
        if (optionalAttendance.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        Attendance attendance = optionalAttendance.get();

        // Get paths from both columns
        List<String> imagePaths = new ArrayList<>();
        if (attendance.getFieldImagePath() != null && !attendance.getFieldImagePath().isBlank()) {
            imagePaths.add(attendance.getFieldImagePath().trim());
        }
        if (attendance.getFieldImagePath1() != null && !attendance.getFieldImagePath1().isBlank()) {
            imagePaths.add(attendance.getFieldImagePath1().trim());
        }

        // If no images found
        if (imagePaths.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        Map<String, String> imagesBase64 = new LinkedHashMap<>();
        for (int i = 0; i < imagePaths.size(); i++) {
            Path path = Paths.get(uploadPath, imagePaths.get(i));
            if (Files.exists(path)) {
                byte[] imageBytes = Files.readAllBytes(path);
                String contentType = Files.probeContentType(path);
                String base64 = "data:" + (contentType != null ? contentType : "image/jpeg") + ";base64,"
                        + Base64.getEncoder().encodeToString(imageBytes);
                imagesBase64.put("image" + (i + 1), base64);
            }
        }

        if (imagesBase64.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.ok(imagesBase64);
    }

    //    Api for fetch Details By Username
    @PostMapping("/detailsByUsername")
    public ResponseEntity<ApiResponse> getDetailsByUsername(@RequestParam String username) {

        try {
            // Call the service to fetch details
            Employee employee = attendanceService.getDetailsByUsername(username);

            if (employee == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(
                        ApiResponse.builder()
                                .username(username)
                                .message("No username found with given username: " + username)
                                .statusCode(HttpStatus.NOT_FOUND.value())
                                .data(null)
                                .build()
                );
            }

            return ResponseEntity.ok(
                    ApiResponse.builder()
                            .username(username)
                            .message("User details fetched successfully")
                            .statusCode(HttpStatus.OK.value())
                            .data(employee)
                            .build()
            );

        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(
                    ApiResponse.builder()
                            .username(username)
                            .message(e.getMessage())
                            .statusCode(HttpStatus.BAD_REQUEST.value())
                            .data(null)
                            .build()
            );

        } catch (Exception e) {
            Map<String, Object> errorData = new HashMap<>();
            errorData.put("error", e.getMessage());

            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                    ApiResponse.builder()
                            .username(username)
                            .message("Error fetching user details")
                            .statusCode(HttpStatus.INTERNAL_SERVER_ERROR.value())
                            .data(errorData)
                            .build()
            );
        }
    }

    //    Api for fetch District For Dropdown
    @PostMapping("/fetchDistricts")
    public ResponseEntity<ApiResponse> fetchDistricts() {

        try {
            // Call the service to fetch details
            List<String> districts = attendanceService.getDistricts();

            return ResponseEntity.ok(
                    ApiResponse.builder()
                            .message("District details fetched successfully")
                            .statusCode(HttpStatus.OK.value())
                            .data(districts)
                            .build()
            );

        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(
                    ApiResponse.builder()
                            .message(e.getMessage())
                            .statusCode(HttpStatus.BAD_REQUEST.value())
                            .data(null)
                            .build()
            );

        } catch (Exception e) {
            Map<String, Object> errorData = new HashMap<>();
            errorData.put("error", e.getMessage());

            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                    ApiResponse.builder()
                            .message("Error fetching user details")
                            .statusCode(HttpStatus.INTERNAL_SERVER_ERROR.value())
                            .data(errorData)
                            .build()
            );
        }
    }

    //    Api for fetch Tehsil For Dropdown By Giving District Name
    @PostMapping("/fetchTehsilByDistrict")
    public ResponseEntity<ApiResponse> fetchTehsilByDistrict(@RequestParam(value = "district") String district) {

        try {
            // Call the service to fetch details
            List<String> tehsils = attendanceService.getTehsilByDistrict(district);

            return ResponseEntity.ok(
                    ApiResponse.builder()
                            .message("Tehsil details fetched successfully")
                            .statusCode(HttpStatus.OK.value())
                            .data(tehsils)
                            .build()
            );

        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(
                    ApiResponse.builder()
                            .message(e.getMessage())
                            .statusCode(HttpStatus.BAD_REQUEST.value())
                            .data(null)
                            .build()
            );

        } catch (Exception e) {
            Map<String, Object> errorData = new HashMap<>();
            errorData.put("error", e.getMessage());

            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                    ApiResponse.builder()
                            .message("Error fetching user details")
                            .statusCode(HttpStatus.INTERNAL_SERVER_ERROR.value())
                            .data(errorData)
                            .build()
            );
        }
    }

    //    Api for Update Employee Profile
    @PostMapping("/employee/updateProfile")
    public ResponseEntity<ApiResponse> updateProfile(
            @RequestParam("id") Long id,
            @RequestParam(value = "dateOfBirth", required = false) String dateOfBirth,
            @RequestParam(value = "labName", required = false) String labName,
            @RequestParam(value = "officeName", required = false) String officeName,
            @RequestParam(value = "mobileNumber", required = false) String mobileNumber,
            @RequestParam(value = "bloodGroup", required = false) String bloodGroup,
            @RequestParam(value = "officeAddress", required = false) String officeAddress,
            @RequestParam(value = "homeLocation", required = false) String homeLocation,
            @RequestParam(value = "emailAddress", required = false) String emailAddress,
            @RequestParam(value = "permanantAddress", required = false) String permanantAddress,
            @RequestParam(value = "emergencyContactNo", required = false) String emergencyContactNo
    ) {
        try {

            Employee updatedEmployee = employeeService.updateEmployeeProfile(
                    id, dateOfBirth, labName, officeName,
                    mobileNumber, bloodGroup, officeAddress, homeLocation, emailAddress,
                    permanantAddress, emergencyContactNo);

            return ResponseEntity.ok(
                    ApiResponse.builder()
                            .message("Employee profile updated successfully")
                            .statusCode(HttpStatus.OK.value())
                            .data(updatedEmployee)
                            .build()
            );

        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(
                    ApiResponse.builder()
                            .message(e.getMessage())
                            .statusCode(HttpStatus.BAD_REQUEST.value())
                            .data(null)
                            .build()
            );
        } catch (Exception e) {
            Map<String, Object> errorData = new HashMap<>();
            errorData.put("error", e.getMessage());

            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                    ApiResponse.builder()
                            .message("Error updating employee profile")
                            .statusCode(HttpStatus.INTERNAL_SERVER_ERROR.value())
                            .data(errorData)
                            .build()
            );
        }
    }

    // API for On-Click Detailed Attendance by Category
    @PostMapping("/dashboard/monthly/details")
    public ResponseEntity<ApiResponse> getMonthlyCategoryDetails(
            @RequestParam("username") String username,
            @RequestParam("year") int year,
            @RequestParam("month") int month,
            @RequestParam("category") String category) {

        try {
            Map<String, Object> details = attendanceService.getMonthlyCategoryDetails(username, year, month, category);

            // Check if service returned an error
            if ("error".equals(details.get("flag"))) {
                return ResponseEntity.badRequest().body(
                        ApiResponse.builder()
                                .message((String) details.get("message"))
                                .statusCode(HttpStatus.BAD_REQUEST.value())
                                .data(null)
                                .build()
                );
            }

            return ResponseEntity.ok(
                    ApiResponse.builder()
                            .message((String) details.get("message"))
                            .statusCode(HttpStatus.OK.value())
                            .data(details.get("data"))
                            .build()
            );

        } catch (Exception e) {
            Map<String, Object> errorData = new HashMap<>();
            errorData.put("error", e.getMessage());

            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                    ApiResponse.builder()
                            .message("Error fetching attendance details")
                            .statusCode(HttpStatus.INTERNAL_SERVER_ERROR.value())
                            .data(errorData)
                            .build()
            );
        }
    }

//    Location Works - Currently These are in work

    //    For get All Employees Details
    @GetMapping("/employeesDetails")
    public List<Employee> employeesDetails() {

        return employeeService.findAllEmployeeDetails();

    }

    //    Api for Location History
    @GetMapping("/location-history")
    public List<WffLocationTracking> getHistory(
            @RequestParam(required = false) String userName,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to
    ) {
        final LocalDateTime toTs = (to != null) ? to : LocalDateTime.now();
        final LocalDateTime fromTs = (from != null) ? from : toTs.minusHours(24);

        if (userName != null && !userName.isBlank()) {

            return attendanceService.getHistoryForUser(userName, fromTs, toTs);
        }
        return attendanceService.getHistory(fromTs, toTs);
    }

    //    Api For Location Latest
    @GetMapping("/location-latest")
    public List<WffLocationTracking> getLatestPerUser() {
        return attendanceService.getLatestPerUser();
    }

    //    Api For Location History By Username
    @GetMapping("/location-latest/{userName}")
    public List<WffLocationTracking> getLatestForUser(@PathVariable String userName) {

        return attendanceService.getLatestForUser(userName);

    }

    //    This api For Mobile App - For Getting Latest Lat Long Data of the User
    @GetMapping("/location-latest-one/{userName}")
    public ResponseEntity<ApiResponse> getLatestForUserOne(@PathVariable String userName) {
        try {
            // Call service to get latest location for the user
            WffLocationTracking latestLocation = attendanceService.getLatestForUserOne(userName);

            // Check if service returned null or some error
            if (latestLocation == null) {
                return ResponseEntity.badRequest().body(
                        ApiResponse.builder()
                                .message("No location found for user: " + userName)
                                .statusCode(HttpStatus.BAD_REQUEST.value())
                                .data(null)
                                .build()
                );
            }

            // Return success response
            return ResponseEntity.ok(
                    ApiResponse.builder()
                            .message("Latest location fetched successfully")
                            .statusCode(HttpStatus.OK.value())
                            .data(latestLocation)
                            .build()
            );

        } catch (Exception e) {
            Map<String, Object> errorData = new HashMap<>();
            errorData.put("error", e.getMessage());

            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                    ApiResponse.builder()
                            .message("Error fetching latest location")
                            .statusCode(HttpStatus.INTERNAL_SERVER_ERROR.value())
                            .data(errorData)
                            .build()
            );
        }
    }

    //    Api For Delete Attendance By Username
    @DeleteMapping("/attendanceDeleteByUsername")
    public ResponseEntity<ApiResponse> attendanceDeleteByUsername(@RequestParam Long id) {

        try {

            Attendance attendance = attendanceService.attendanceDeleteById(id);

            if (attendance == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(
                        ApiResponse.builder()
                                .username(String.valueOf(id))
                                .message("No username found with given username: " + id)
                                .statusCode(HttpStatus.NOT_FOUND.value())
                                .data(null)
                                .build()
                );
            }

            return ResponseEntity.ok(
                    ApiResponse.builder()
                            .username(String.valueOf(id))
                            .message("Attendance Deleted successfully")
                            .statusCode(HttpStatus.OK.value())
                            .data(attendance)
                            .build()
            );

        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(
                    ApiResponse.builder()
                            .username(String.valueOf(id))
                            .message(e.getMessage())
                            .statusCode(HttpStatus.BAD_REQUEST.value())
                            .data(null)
                            .build()
            );

        } catch (Exception e) {
            Map<String, Object> errorData = new HashMap<>();
            errorData.put("error", e.getMessage());

            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                    ApiResponse.builder()
                            .message("Error fetching user details")
                            .statusCode(HttpStatus.INTERNAL_SERVER_ERROR.value())
                            .data(errorData)
                            .build()
            );
        }
    }

    // CREATE Holiday
    @PostMapping("/createHolidays")
    public Holidays saveHoliday(@RequestBody Holidays holiday) {

        return attendanceService.saveHoliday(holiday);

    }

    // UPDATE Holiday
    @PutMapping("/updateHoliday/{id}")
    public ResponseEntity<Holidays> updateHoliday(@PathVariable Long id, @RequestBody Holidays holiday) {

        Holidays updated = attendanceService.updateHoliday(id, holiday);

        return ResponseEntity.ok(updated);
    }

    // DELETE Holiday
    @DeleteMapping("/deleteHoliday/{id}")
    public ResponseEntity<Void> deleteHoliday(@PathVariable Long id) {

        attendanceService.deleteHoliday(id);

        return ResponseEntity.noContent().build();
    }

//    Filter Wise Attendance Report

    // API for Attendance Filter by Office, District, and Date Range
    @PostMapping(path = "/attendance/filter")
    public ResponseEntity<ApiResponse> getAttendanceByFilters(
            @RequestParam String officeName,
            @RequestParam String district,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {

        try {
            Map<String, Object> details = attendanceService.getAttendanceByFilters(
                    officeName, district, startDate, endDate
            );

            // Case 1: Service returned "error" flag
            if ("error".equals(details.get("flag"))) {
                return ResponseEntity.badRequest().body(
                        ApiResponse.builder()
                                .message((String) details.get("message"))
                                .statusCode(HttpStatus.BAD_REQUEST.value())
                                .data(null)
                                .build()
                );
            }

            // Case 2: No data found
            if ("not_found".equals(details.get("flag"))) {
                return ResponseEntity.ok(
                        ApiResponse.builder()
                                .message((String) details.get("message")) // "No attendance records found..."
                                .statusCode(HttpStatus.OK.value()) // still 200
                                .data(Collections.emptyList()) // always return empty list
                                .build()
                );
            }

            // Case 3: Success
            return ResponseEntity.ok(
                    ApiResponse.builder()
                            .message((String) details.get("message"))
                            .statusCode(HttpStatus.OK.value())
                            .data(details.get("data"))
                            .build()
            );

        } catch (Exception e) {
            Map<String, Object> errorData = new HashMap<>();
            errorData.put("error", e.getMessage());

            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                    ApiResponse.builder()
                            .message("Error fetching filtered attendance")
                            .statusCode(HttpStatus.INTERNAL_SERVER_ERROR.value())
                            .data(errorData)
                            .build()
            );
        }
    }

    //    API For Applying Leave
    @PostMapping("/applyLeave")
    public ResponseEntity<ApiResponse> applyLeave(@ModelAttribute LeaveRequestDto leaveRequestDto) {

        try {
            Leave savedLeave = attendanceService.applyLeaveRequest(leaveRequestDto);

            return ResponseEntity.ok(
                    ApiResponse.builder()
                            .username(savedLeave.getUsername())
                            .message("Leave applied successfully")
                            .statusCode(HttpStatus.OK.value())
                            .data(savedLeave)
                            .build()
            );

        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(
                    ApiResponse.builder()
                            .message(e.getMessage())
                            .statusCode(HttpStatus.BAD_REQUEST.value())
                            .data(null)
                            .build()
            );

        } catch (Exception e) {
            Map<String, Object> errorData = new HashMap<>();
            errorData.put("error", e.getMessage());

            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                    ApiResponse.builder()
                            .message("Error while applying for leave")
                            .statusCode(HttpStatus.INTERNAL_SERVER_ERROR.value())
                            .data(errorData)
                            .build()
            );
        }
    }

//    Crud of Office Type Started

    // Create Office Type
    @PostMapping("/saveOfficeType")
    public ResponseEntity<ApiResponse> createOfficeType(@ModelAttribute OfficeTypeDto officeType) {

        try {

            OfficeType saved = employeeService.saveOfficeType(officeType);

            return ResponseEntity.ok(
                    ApiResponse.builder()
                            .message("Office type created successfully")
                            .statusCode(HttpStatus.OK.value())
                            .data(saved)
                            .build()
            );

        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(
                    ApiResponse.builder()
                            .message(e.getMessage())
                            .statusCode(HttpStatus.BAD_REQUEST.value())
                            .data(null)
                            .build()
            );

        } catch (Exception e) {
            Map<String, Object> errorData = new HashMap<>();
            errorData.put("error", e.getMessage());

            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                    ApiResponse.builder()
                            .message("Error while creating office type")
                            .statusCode(HttpStatus.INTERNAL_SERVER_ERROR.value())
                            .data(errorData)
                            .build()
            );
        }
    }

    // Get All Office Details
    @GetMapping("/fetchOfficeDetails")
    public ResponseEntity<ApiResponse> getAllOfficeDetails() {
        try {
            List<OfficeType> officeTypes = employeeService.getAllOfficeTypes();
            List<OfficeName> officeNames = employeeService.getAllOfficeNames();

            Map<String, Object> officeDetails = new HashMap<>();
            officeDetails.put("officeTypes", officeTypes);
            officeDetails.put("officeNames", officeNames);

            return ResponseEntity.ok(
                    ApiResponse.builder()
                            .message("Office Details fetched successfully")
                            .statusCode(HttpStatus.OK.value())
                            .data(officeDetails)
                            .build()
            );

        } catch (Exception e) {
            Map<String, Object> errorData = new HashMap<>();
            errorData.put("error", e.getMessage());

            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                    ApiResponse.builder()
                            .message("Error while fetching office details")
                            .statusCode(HttpStatus.INTERNAL_SERVER_ERROR.value())
                            .data(errorData)
                            .build()
            );
        }
    }

    // Create Office Name
    @PostMapping("/saveOfficeName")
    public ResponseEntity<ApiResponse> createOfficeName(@ModelAttribute OfficeNameDto dto) {

        try {

            OfficeName saved = employeeService.saveOfficeName(dto);

            return ResponseEntity.ok(
                    ApiResponse.builder()
                            .message("Office name created successfully")
                            .statusCode(HttpStatus.OK.value())
                            .data(saved)
                            .build()
            );

        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(
                    ApiResponse.builder()
                            .message(e.getMessage())
                            .statusCode(HttpStatus.BAD_REQUEST.value())
                            .data(null)
                            .build()
            );

        } catch (Exception e) {
            Map<String, Object> errorData = new HashMap<>();
            errorData.put("error", e.getMessage());

            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                    ApiResponse.builder()
                            .message("Error while creating office name")
                            .statusCode(HttpStatus.INTERNAL_SERVER_ERROR.value())
                            .data(errorData)
                            .build()
            );
        }
    }

    // Create Leave Type
    @PostMapping("/saveLeaveType")
    public ResponseEntity<ApiResponse> createLeaveType(@ModelAttribute LeaveTypeDto dto) {

        try {

            LeaveType saved = employeeService.saveLeaveType(dto);

            return ResponseEntity.ok(
                    ApiResponse.builder()
                            .message("Leave Type created successfully")
                            .statusCode(HttpStatus.OK.value())
                            .data(saved)
                            .build()
            );

        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(
                    ApiResponse.builder()
                            .message(e.getMessage())
                            .statusCode(HttpStatus.BAD_REQUEST.value())
                            .data(null)
                            .build()
            );

        } catch (Exception e) {
            Map<String, Object> errorData = new HashMap<>();
            errorData.put("error", e.getMessage());

            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                    ApiResponse.builder()
                            .message("Error while creating leave type")
                            .statusCode(HttpStatus.INTERNAL_SERVER_ERROR.value())
                            .data(errorData)
                            .build()
            );
        }
    }

    //    Fetch All Leave Types
    @GetMapping("/fetchLeaveTypes")
    public ResponseEntity<ApiResponse> getAllLeaveTypes() {

        try {

            List<LeaveType> leaveTypes = employeeService.getAllLeaveTypes();

            return ResponseEntity.ok(
                    ApiResponse.builder()
                            .message("Leave Types fetched successfully")
                            .statusCode(HttpStatus.OK.value())
                            .data(leaveTypes)
                            .build()
            );

        } catch (Exception e) {
            Map<String, Object> errorData = new HashMap<>();
            errorData.put("error", e.getMessage());

            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                    ApiResponse.builder()
                            .message("Error while fetching leave types")
                            .statusCode(HttpStatus.INTERNAL_SERVER_ERROR.value())
                            .data(errorData)
                            .build()
            );
        }
    }

    @PostMapping("/{id}/toggle-approval")
    public ResponseEntity<String> toggleApproval(@PathVariable Long id) {

        employeeService.toggleApproval(id);

        return ResponseEntity.ok("Approval status updated successfully.");
    }

//    Apply For Extra Work

    @PostMapping("/applyExtraWork")
    public ResponseEntity<ApiResponse> applyExtraWork(@Valid @ModelAttribute ExtraWork extraWork,
                                                      BindingResult bindingResult) {
        if (bindingResult.hasErrors()) {
            return ResponseEntity.badRequest().body(
                    ApiResponse.builder()
                            .message("Validation failed")
                            .statusCode(HttpStatus.BAD_REQUEST.value())
                            .data(bindingResult.getAllErrors())
                            .build()
            );
        }

        try {
            ExtraWork saved = employeeService.applyExtraWork(extraWork);

            return ResponseEntity.ok(
                    ApiResponse.builder()
                            .message("Extra Work applied successfully")
                            .statusCode(HttpStatus.OK.value())
                            .data(saved)
                            .build()
            );

        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(
                    ApiResponse.builder()
                            .message(e.getMessage())
                            .statusCode(HttpStatus.BAD_REQUEST.value())
                            .data(null)
                            .build()
            );
        } catch (Exception e) {
            Map<String, Object> errorData = new HashMap<>();
            errorData.put("error", e.getMessage());

            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                    ApiResponse.builder()
                            .message("Error while applying for extra work")
                            .statusCode(HttpStatus.INTERNAL_SERVER_ERROR.value())
                            .data(errorData)
                            .build()
            );
        }
    }


    //    For Check Username is Approved or not for python service
    @GetMapping("/admin/employees/{username}")
    public ResponseEntity<Employee> getEmployeeByUsername(@PathVariable String username) {
        return employeeService.getEmployeeByUsername(username)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }


//    For Filter Wise Reporting Advance Filter

    @PostMapping("/filterWiseAdvanceReporting")
    public List<Attendance> filterAttendance(@RequestBody AttendanceFilterRequest filter) {

        return attendanceService.filterWiseAdvanceReporting(filter);

    }

}