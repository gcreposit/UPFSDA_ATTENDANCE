package com.example.demo.controller;

import com.example.demo.dto.ApiResponse;
import com.example.demo.dto.EmployeeRequest;
import com.example.demo.dto.EmployeeResponse;
import com.example.demo.dto.ErrorResponse;
import com.example.demo.entity.Attendance;
import com.example.demo.entity.Employee;
import com.example.demo.entity.WffLocationTracking;
import com.example.demo.service.AttendanceService;
import com.example.demo.service.EmployeeService;
import com.example.demo.service.FileStorageService;
import com.example.demo.service.LocationService;
import com.example.demo.serviceimpl.EmployeeServiceImpl;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
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
            @RequestParam("fieldImage") MultipartFile fieldImage,@RequestParam("fieldImage1") MultipartFile fieldImage1) {

        try {

            String message = attendanceService.uploadFieldImages(username, fieldImage,fieldImage1);

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

    //    For Location Tracking Work - Not Currently In Use
    @GetMapping("/history/{userName}")
    public List<WffLocationTracking> getHistory(@PathVariable String userName) {

        return attendanceService.fetchWffEmployeesLocationHistory(userName);

    }

    // SSE stream for live updates - Not Currently In Use
    @GetMapping("/stream/{userName}")
    public SseEmitter streamLocation(@PathVariable String userName) {

        SseEmitter emitter = attendanceService.createEmitter(userName);

        // Send last known location immediately
        WffLocationTracking latest = attendanceService.getLatest(userName);
        if (latest != null) {
            try {
                emitter.send(SseEmitter.event().name("location").data(latest));
            } catch (Exception ignored) {
            }
        }

        return emitter;
    }

    //    For get All Employees Details
    @GetMapping("/employeesDetails")
    public List<Employee> employeesDetails() {

        return employeeService.findAllEmployeeDetails();
    }

    //    For Get Location History Data
    @GetMapping("/location-history")
    public List<WffLocationTracking> getLocationHistory() {

        return attendanceService.findAllLocationHistory();
    }

}