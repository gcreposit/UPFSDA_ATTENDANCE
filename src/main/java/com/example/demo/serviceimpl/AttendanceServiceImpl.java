package com.example.demo.serviceimpl;

import com.example.demo.dto.ApiResponse;
import com.example.demo.entity.Attendance;
import com.example.demo.entity.Employee;
import com.example.demo.entity.WffLocationTracking;
import com.example.demo.repository.AttendanceRepository;
import com.example.demo.repository.EmployeeRepository;
import com.example.demo.repository.WffLocationTrackingRepository;
import com.example.demo.repository.WorkTypesRepository;
import com.example.demo.service.AttendanceService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.File;
import java.io.IOException;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class AttendanceServiceImpl implements AttendanceService {

    // username -> list of active SSE connections
    private final Map<String, List<SseEmitter>> emitters = new ConcurrentHashMap<>();

    @Value("${file.storage.path}")
    private String uploadPath;

    @Autowired
    private AttendanceRepository attendanceRepository;

    @Autowired
    private WffLocationTrackingRepository locationTrackingRepository;

    @Autowired
    private EmployeeRepository employeeRepository;

    @Autowired
    private WorkTypesRepository workTypesRepository;
    @Autowired
    private AttendanceService attendanceService;

    @Override
    @Transactional
    public Attendance saveAttendance(String userName,
                                     MultipartFile image,
                                     String attendanceTypeFromRequest,
                                     String reason) throws IOException {

        // Step 1: Check if username exists in Employee table
        boolean userExists = employeeRepository.existsByUsername(userName);
        if (!userExists) {
            throw new IllegalArgumentException("Username not registered");
        }

        LocalDate today = LocalDate.now();
        Optional<Attendance> existingAttendanceOpt = attendanceRepository.findTopByUserNameAndDate(userName, today);
        Attendance attendance = existingAttendanceOpt.orElse(new Attendance());

        // Determine if this is morning attendance (morning image or time not set yet)
        boolean isMorningAttendance = (attendance.getMorningImagePath() == null || attendance.getMorningTime() == null);

        if (isMorningAttendance
                && attendanceTypeFromRequest != null
                && (attendanceTypeFromRequest.equalsIgnoreCase("WFH") || attendanceTypeFromRequest.equalsIgnoreCase("WFF"))
                && (attendance.getMorningImagePath() == null || attendance.getMorningImagePath().trim().isEmpty())
                && (reason == null || reason.trim().isEmpty())) {

            throw new IllegalStateException("Reason is required when attendanceType is WFH or WFF and morning image is not present");
        }

        // Set attendance type only once (when creating new record)
        if (attendance.getId() == null) {
            attendance.setAttendanceType(attendanceTypeFromRequest);
        }

        // Prevent double attendance marking
        if (attendance.getMorningImagePath() != null && attendance.getEveningImagePath() != null &&
                attendance.getMorningTime() != null && attendance.getEveningTime() != null) {
            throw new IllegalStateException("User has already marked attendance for today.");
        }

        attendance.setUserName(userName);
        attendance.setReason(reason);

        // Office timing constants
        LocalTime officeStart = LocalTime.of(10, 0); // 10:00 AM
        LocalTime officeEnd = LocalTime.of(18, 0);   // 6:00 PM

        // Always get backend current time
        LocalDateTime currentTime = LocalDateTime.now();

        // 🟢 Mark Morning Attendance
        if (attendance.getMorningImagePath() == null || attendance.getMorningTime() == null) {
            if (image != null && !image.isEmpty()) {
                String morningPath = saveImageToDisk(image);
                attendance.setMorningImagePath(morningPath);
                attendance.setMorningTime(currentTime);
                attendance.setStatus(
                        currentTime.toLocalTime().isAfter(officeStart) ? "Late Entry" : "On Time"
                );
            }
        }
        // 🟢 Mark Evening Attendance
        else if (image != null && !image.isEmpty()) {
            String eveningPath = saveImageToDisk(image);
            attendance.setEveningImagePath(eveningPath);
            attendance.setEveningTime(currentTime);

            if (currentTime.toLocalTime().isBefore(officeEnd)) {
                if ("Late Entry".equals(attendance.getStatus())) {
                    attendance.setStatus("Late & Half");
                } else if ("On Time".equals(attendance.getStatus())) {
                    attendance.setStatus("Half Day");
                }
            }
        }

        // Set date only on creation
        if (attendance.getDate() == null) {
            attendance.setDate(today);
        }

        return attendanceRepository.save(attendance);
    }


    private String saveImageToDisk(MultipartFile file) throws IOException {
        String filename = UUID.randomUUID() + "_" + file.getOriginalFilename();
        File dest = new File(uploadPath + filename);
        dest.getParentFile().mkdirs();
        file.transferTo(dest);
        return filename;
    }

    @Override
    public Attendance getDashboardData(String userName, String date) {

        // Validate if employee exists
        Employee employee = employeeRepository.findByUsername(userName);
        if (employee == null) {
            throw new IllegalArgumentException("Employee not found for username: " + userName);
        }

        // Parse date string to LocalDate
        LocalDate parsedDate = LocalDate.parse(date, DateTimeFormatter.ofPattern("yyyy-MM-dd"));

        return attendanceRepository.findByUserNameAndDate(userName, parsedDate);

    }

    @Override
    @Transactional
    public ApiResponse<Object> saveLocationForTracking(String userName, String lat, String lon, String timestamp) {

        try {
            LocalDateTime parsedTimestamp = LocalDateTime.parse(timestamp, DateTimeFormatter.ISO_DATE_TIME);

            WffLocationTracking tracking = WffLocationTracking.builder()
                    .userName(userName)
                    .lat(Double.valueOf(lat))
                    .lon(Double.valueOf(lon))
                    .timestamp(parsedTimestamp)
                    .build();

            WffLocationTracking saved = locationTrackingRepository.save(tracking);
            notifyClients(userName, saved);

            Map<String, Object> responseData = new HashMap<>();
            responseData.put("flag", "success");

            return ApiResponse.builder()
                    .message("Location saved successfully")
                    .statusCode(HttpStatus.OK.value())
                    .data(responseData)
                    .build();

        } catch (DateTimeParseException e) {
            Map<String, Object> error = new HashMap<>();
            error.put("error", "Invalid timestamp format. Use ISO format: yyyy-MM-dd'T'HH:mm:ss");

            return ApiResponse.builder()
                    .message("Invalid timestamp format")
                    .statusCode(HttpStatus.BAD_REQUEST.value())
                    .data(error)
                    .build();
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("error", e.getMessage());

            return ApiResponse.builder()
                    .message("Failed to save location")
                    .statusCode(HttpStatus.INTERNAL_SERVER_ERROR.value())
                    .data(error)
                    .build();
        }
    }

    @Override
    @Transactional
    public String uploadFieldImages(String username, MultipartFile fieldImage, MultipartFile fieldImage1) throws IOException {

        LocalDate localDate = LocalDate.now();

        // Validate if employee exists
        Employee employee = employeeRepository.findByUsername(username);
        if (employee == null) {
            throw new IllegalArgumentException("Employee not found for username: " + username);
        }

        Attendance attendance = attendanceRepository.findByUserNameAndDate(username, localDate);
        if (attendance == null) {
            throw new IllegalArgumentException("Attendance record not found for today");
        }

        // Validation: If already uploaded, stop
        if ("Images Added".equalsIgnoreCase(attendance.getFieldImageUploaded())) {
            throw new IllegalArgumentException("Field images already uploaded for today");
        }

        // Validate: only WFF type allowed
        if (!"WFF".equalsIgnoreCase(attendance.getAttendanceType())) {
            throw new IllegalArgumentException("Field images are allowed only for WFF attendance type");
        }

        // Validate: both images must be provided
        if ((fieldImage == null || fieldImage.isEmpty()) && (fieldImage1 == null || fieldImage1.isEmpty())) {
            throw new IllegalArgumentException("At least one field image is required");
        }

        // Save first image
        if (fieldImage != null && !fieldImage.isEmpty()) {
            String filePath = saveImageToDisk(fieldImage); // your helper method
            attendance.setFieldImagePath(filePath);
        }

        // Save second image
        if (fieldImage1 != null && !fieldImage1.isEmpty()) {
            String filePath1 = saveImageToDisk(fieldImage1); // your helper method
            attendance.setFieldImagePath1(filePath1);
        }

        attendance.setFieldImageTime(LocalDateTime.now());
        attendance.setFieldImageUploaded("Images Added");

        attendanceRepository.save(attendance);

        return "Field images uploaded successfully";
    }


//    @Override
//    public Map<String, Object> getMonthlyAttendanceCount(String employeeId, int year, int month) {
//
//        Attendance attendance = attendanceRepository.findByUsername(employeeId);
//        if (attendance == null) {
//            Map<String, Object> response = new HashMap<>();
//            response.put("flag", "error");
//            response.put("message", "Username not found");
//            return response;
//        }
//
//        // Fetch all attendance records for the given employee, year, and month.
//        List<Attendance> records = attendanceRepository.findByUserNameAndMonthAndYear(employeeId, year, month);
//
//        // Calculate the total number of days in the specified month and year.
//        int totalDaysInMonth = YearMonth.of(year, month).lengthOfMonth();
//
//        // Count attendance by status using streams.
//        long onTime = records.stream()
//                .filter(r -> "On Time".equalsIgnoreCase(r.getStatus()))
//                .count();
//        long lateEntry = records.stream()
//                .filter(r -> "Late Entry".equalsIgnoreCase(r.getStatus()))
//                .count();
//        long halfDay = records.stream()
//                .filter(r -> "Half Day".equalsIgnoreCase(r.getStatus()))
//                .count();
//        long lateAndHalf = records.stream()
//                .filter(r -> "Late & Half".equalsIgnoreCase(r.getStatus()))
//                .count();
//
//        // Calculate the number of absent days.
//        // This is the total days in the month minus the number of records (which includes present and on leave).
//        long absent = totalDaysInMonth - records.size();
//
//        // Count attendance by work type.
//        long wfh = records.stream()
//                .filter(r -> "WFH".equalsIgnoreCase(r.getAttendanceType()))
//                .count();
//        long wfo = records.stream()
//                .filter(r -> "WFO".equalsIgnoreCase(r.getAttendanceType()))
//                .count();
//        long wff = records.stream()
//                .filter(r -> "WFF".equalsIgnoreCase(r.getAttendanceType()))
//                .count();
//
//        // Prepare the detailed data map.
//        Map<String, Object> data = new HashMap<>();
//        data.put("on_time", onTime);
//        data.put("late_entry", lateEntry + lateAndHalf); // Combine late entries for a clearer summary
//        data.put("half_day", halfDay);
//        data.put("absent", absent); // Correctly calculated absent days
//        data.put("total_work_from_home", wfh);
//        data.put("total_work_from_office", wfo);
//        data.put("total_work_from_field", wff);
//        data.put("total_days_in_month", totalDaysInMonth);
//
//        // Prepare the final response map.
//        Map<String, Object> response = new HashMap<>();
//        response.put("flag", "success");
//        response.put("message", "Monthly attendance summary retrieved successfully");
//        response.put("data", data);
//
//        return response;
//    }

    @Override
    public Map<String, Object> getMonthlyAttendanceCount(String employeeId, int year, int month) {

        Employee employee = employeeRepository.findByUsername(employeeId);

        if (employee == null) {
            Map<String, Object> response = new HashMap<>();
            response.put("flag", "error");
            response.put("message", "Username not found");
            return response;
        }

        // Fetch records for the given month/year
        List<Attendance> records = attendanceRepository.findByUserNameAndMonthAndYear(employeeId, year, month);

        // Step 1: Generate working days list (exclude Sundays)
        YearMonth ym = YearMonth.of(year, month);
        LocalDate today = LocalDate.now();
        LocalDate endDate = (year == today.getYear() && month == today.getMonthValue())
                ? today  // if current month, only consider days up to today
                : ym.atEndOfMonth(); // for past months, consider full month

        List<LocalDate> workingDays = new ArrayList<>();
        for (int day = 1; day <= endDate.getDayOfMonth(); day++) {
            LocalDate date = LocalDate.of(year, month, day);
            if (date.getDayOfWeek() != DayOfWeek.SUNDAY) {
                workingDays.add(date);
            }
        }

        // Step 2: Get attended days from records
        Set<LocalDate> attendedDays = records.stream()
                .map(Attendance::getDate)
                .collect(Collectors.toSet());

        // Step 3: Calculate absent days (working days without attendance)
        long absent = workingDays.stream()
                .filter(day -> !attendedDays.contains(day))
                .count();

        // Step 4: Count attendance by status
        long onTime = records.stream()
                .filter(r -> "On Time".equalsIgnoreCase(r.getStatus()))
                .count();
        long lateEntry = records.stream()
                .filter(r -> "Late Entry".equalsIgnoreCase(r.getStatus()))
                .count();
        long halfDay = records.stream()
                .filter(r -> "Half Day".equalsIgnoreCase(r.getStatus()))
                .count();
        long lateAndHalf = records.stream()
                .filter(r -> "Late & Half".equalsIgnoreCase(r.getStatus()))
                .count();

        // Step 5: Count by attendance type
        long wfh = records.stream()
                .filter(r -> "WFH".equalsIgnoreCase(r.getAttendanceType()))
                .count();
        long wfo = records.stream()
                .filter(r -> "WFO".equalsIgnoreCase(r.getAttendanceType()))
                .count();
        long wff = records.stream()
                .filter(r -> "WFF".equalsIgnoreCase(r.getAttendanceType()))
                .count();

        // Step 6: Prepare the detailed data
        Map<String, Object> data = new HashMap<>();
        data.put("on_time", onTime);
        data.put("late_entry", lateEntry);
        data.put("late_and_half", lateAndHalf);
        data.put("half_day", halfDay);
        data.put("absent", absent);
        data.put("total_work_from_home", wfh);
        data.put("total_work_from_office", wfo);
        data.put("total_work_from_field", wff);
        data.put("total_days_in_month", ym.lengthOfMonth());

        // Step 7: Prepare final response
        Map<String, Object> response = new HashMap<>();
        response.put("flag", "success");
        response.put("message", "Monthly attendance summary retrieved successfully");
        response.put("data", data);

        return response;
    }


    @Override
    public Optional<Map<String, Object>> getDashboardDataForAdmin() {

        LocalDate today = LocalDate.now();

        // Fetch today's attendance records.
        List<Attendance> todaysAttendances = attendanceRepository.findAttendanceByDate(today);

        // Get total employee count from the repository.
        long totalEmployees = employeeRepository.count();

        // Calculate counts based on attendance status.
        long onTime = todaysAttendances.stream()
                .filter(r -> "On Time".equalsIgnoreCase(r.getStatus()))
                .count();
        long lateEntry = todaysAttendances.stream()
                .filter(r -> "Late Entry".equalsIgnoreCase(r.getStatus()))
                .count();
        long halfDay = todaysAttendances.stream()
                .filter(r -> "Half Day".equalsIgnoreCase(r.getStatus()))
                .count();
        long lateAndHalf = todaysAttendances.stream()
                .filter(r -> "Late & Half".equalsIgnoreCase(r.getStatus()))
                .count();

        // Calculate count for employees who are marked as "On Leave".
        long onLeaveToday = todaysAttendances.stream()
                .filter(r -> "On Leave".equalsIgnoreCase(r.getStatus()))
                .count();

        // Calculate present today (excluding those on leave).
        long presentToday = onTime + lateEntry + halfDay + lateAndHalf;

        // Calculate the total number of employees who are accounted for (present or on leave).
        long accountedForToday = presentToday + onLeaveToday;

        // The number of absent employees is the total number of employees minus those accounted for.
        long absentToday = totalEmployees - accountedForToday;

        // Calculate counts based on work type.
        long wfh = todaysAttendances.stream()
                .filter(r -> "WFH".equalsIgnoreCase(r.getAttendanceType()))
                .count();
        long wfo = todaysAttendances.stream()
                .filter(r -> "WFO".equalsIgnoreCase(r.getAttendanceType()))
                .count();
        long wff = todaysAttendances.stream()
                .filter(r -> "WFF".equalsIgnoreCase(r.getAttendanceType()))
                .count();

        // Prepare the dashboard data map.
        Map<String, Object> data = new HashMap<>();
        data.put("total_employees", totalEmployees);
        data.put("present_today", presentToday); // explicitly present today count
        data.put("on_time", onTime);
        data.put("late_entry", lateEntry);
        data.put("half_day", halfDay);
        data.put("late_and_half", lateAndHalf);
        data.put("absent_today", absentToday);
        data.put("on_leave_today", onLeaveToday);
        data.put("total_work_from_home", wfh);
        data.put("total_work_from_office", wfo);
        data.put("total_work_from_field", wff);
        data.put("todays_date", today);

        if (totalEmployees == 0) {
            return Optional.empty();
        }

        return Optional.of(data);
    }

    @Override
    public List<Attendance> getAttendanceByType(String type) {

        LocalDate today = LocalDate.now();

        switch (type) {
            case "on_time":
                return attendanceRepository.findOnTimeEmployees(today);

            case "late_entry":
                return attendanceRepository.findLateEmployees(today);

            case "absent":

                // 1. Get all employee usernames in the *same format* as stored in Attendance
                List<String> allEmployees = employeeRepository.findAll()
                        .stream()
                        .map(e -> e.getUsername().trim().toLowerCase()) // normalize for comparison
                        .collect(Collectors.toList());

                // 2. Get today's present employees (null-safe and normalized)
                List<String> presentEmployees = Optional.ofNullable(
                                attendanceRepository.findUserNamesByDate(today)
                        ).orElse(Collections.emptyList())
                        .stream()
                        .map(name -> name.trim().toLowerCase()) // normalize for comparison
                        .collect(Collectors.toList());

                // 3. Use a Set for faster lookup
                Set<String> presentSet = new HashSet<>(presentEmployees);

                // 4. Filter out absentees
                List<String> absentEmployees = allEmployees.stream()
                        .filter(emp -> !presentSet.contains(emp))
                        .collect(Collectors.toList());

                // 5. Convert to Attendance objects
                return absentEmployees.stream()
                        .map(emp -> {
                            Attendance att = new Attendance();
                            att.setUserName(emp);
                            att.setDate(today);
                            att.setStatus("Absent"); // computed in code, not DB
                            return att;
                        })
                        .collect(Collectors.toList());
            case "half_day":
                return attendanceRepository.findHalfDayEmployees(today);

            case "late_and_half":
                return attendanceRepository.findLateAndHalfDayEmployees(today);

            case "wfh":
                return attendanceRepository.findWfhEmployees(today);

            case "wff":
                return attendanceRepository.findWffEmployees(today);

            case "wfo":
                return attendanceRepository.findWfoEmployees(today);

            case "today_present":

                return attendanceRepository.findTodayPresentEmployees(today);

            default:
                return new ArrayList<>();
        }
    }

    @Override
    public List<Employee> fetchAllEmployeeDetails() {

        return employeeRepository.findAll();

    }

    @Override
    public Optional<Attendance> findById(Long id) {

        return attendanceRepository.findById(id);

    }

    @Override
    public Employee getDetailsByUsername(String username) {

        if (username == null || username.isBlank()) {
            throw new IllegalArgumentException("Username cannot be blank");
        }

        Employee employee = employeeRepository.findByUsername(username);

        return employee;

    }

    @Override
    public List<String> getDistricts() {

        return employeeRepository.fetchAllDistricts();

    }

    @Override
    public List<String> getTehsilByDistrict(String district) {

        return employeeRepository.fetchAllTehsilByDistrict(district);

    }

    @Override
    public List<String> fetchWffEmployees() {

        LocalDate today = LocalDate.now();

        List<Attendance> employeesList = attendanceRepository.findWffEmployees(today);

        return employeesList.stream()
                .map(Attendance::getUserName)
                .distinct()
                .collect(Collectors.toList());

    }

    @Override
    public List<WffLocationTracking> fetchWffEmployeesLocationHistory(String userName) {

        return locationTrackingRepository.findByUserNameOrderByTimestampAsc(userName);

    }

    @Override
    public SseEmitter createEmitter(String userName) {

        SseEmitter emitter = new SseEmitter(0L); // 0L = no timeout
        emitters.computeIfAbsent(userName, k -> new CopyOnWriteArrayList<>()).add(emitter);

        emitter.onCompletion(() -> removeEmitter(userName, emitter));
        emitter.onTimeout(() -> removeEmitter(userName, emitter));
        emitter.onError((e) -> removeEmitter(userName, emitter));

        return emitter;
    }

    @Override
    public WffLocationTracking getLatest(String userName) {

        return locationTrackingRepository.findTopByUserNameOrderByTimestampDesc(userName);

    }

    @Override
    public List<WffLocationTracking> findAllLocationHistory() {

        LocalDate today = LocalDate.now();

        return locationTrackingRepository.findTrackingDataByDate(today);

    }

    @Override
    public Map<String, Object> getMonthlyCategoryDetails(String employeeId, int year, int month, String category) {
        Map<String, Object> response = new HashMap<>();

        // Step 1: Check if employee exists
        Employee employee = employeeRepository.findByUsername(employeeId);
        if (employee == null) {
            response.put("flag", "error");
            response.put("message", "Username not found");
            return response;
        }

        // Step 2: Fetch all records for the month
        List<Attendance> records = attendanceRepository.findByUserNameAndMonthAndYear(employeeId, year, month);

        if (records.isEmpty()) {
            response.put("flag", "success");
            response.put("message", "No records found for this month");
            response.put("data", Collections.emptyList());
            return response;
        }

        // Step 3: Filter based on category
        List<Attendance> filtered;
        switch (category.toLowerCase()) {
            case "on_time":
                filtered = records.stream()
                        .filter(r -> "On Time".equalsIgnoreCase(r.getStatus()))
                        .collect(Collectors.toList());
                break;

            case "late_entry":
                filtered = records.stream()
                        .filter(r -> "Late Entry".equalsIgnoreCase(r.getStatus()))
                        .collect(Collectors.toList());
                break;

            case "half_day":
                filtered = records.stream()
                        .filter(r -> "Half Day".equalsIgnoreCase(r.getStatus()))
                        .collect(Collectors.toList());
                break;

            case "late_and_half":
                filtered = records.stream()
                        .filter(r -> "Late & Half".equalsIgnoreCase(r.getStatus()))
                        .collect(Collectors.toList());
                break;

            case "absent":
                // Generate working days (excluding Sundays) to detect absent days
                YearMonth ym = YearMonth.of(year, month);
                LocalDate today = LocalDate.now();
                LocalDate endDate = (year == today.getYear() && month == today.getMonthValue())
                        ? today
                        : ym.atEndOfMonth();

                List<LocalDate> workingDays = new ArrayList<>();
                for (int day = 1; day <= endDate.getDayOfMonth(); day++) {
                    LocalDate date = LocalDate.of(year, month, day);
                    if (date.getDayOfWeek() != DayOfWeek.SUNDAY) {
                        workingDays.add(date);
                    }
                }

                Set<LocalDate> attendedDays = records.stream()
                        .map(Attendance::getDate)
                        .collect(Collectors.toSet());

                List<Attendance> absentList = new ArrayList<>();
                for (LocalDate day : workingDays) {
                    if (!attendedDays.contains(day)) {
                        Attendance absentRecord = new Attendance();
                        absentRecord.setDate(day);
                        absentRecord.setStatus("Absent");
                        absentRecord.setAttendanceType("-");
                        absentList.add(absentRecord);
                    }
                }
                filtered = absentList;
                break;

            case "wfh":
                filtered = records.stream()
                        .filter(r -> "WFH".equalsIgnoreCase(r.getAttendanceType()))
                        .collect(Collectors.toList());
                break;

            case "wfo":
                filtered = records.stream()
                        .filter(r -> "WFO".equalsIgnoreCase(r.getAttendanceType()))
                        .collect(Collectors.toList());
                break;

            case "wff":
                filtered = records.stream()
                        .filter(r -> "WFF".equalsIgnoreCase(r.getAttendanceType()))
                        .collect(Collectors.toList());
                break;

            default:
                response.put("flag", "error");
                response.put("message", "Invalid category");
                return response;
        }

        List<Map<String, Object>> details = filtered.stream().map(r -> {
            Map<String, Object> map = new HashMap<>();
            map.put("date", r.getDate());
            map.put("status", r.getStatus());
            map.put("attendance_type", r.getAttendanceType());
            map.put("in_time", r.getMorningTime() != null ? r.getMorningTime().toString() : "");
            map.put("out_time", r.getEveningTime() != null ? r.getEveningTime().toString() : "");
            map.put("remarks", r.getReason() != null ? r.getReason() : "");
            return map;
        }).collect(Collectors.toList());

        response.put("flag", "success");
        response.put("message", "Details retrieved successfully");
        response.put("data", details);

        return response;
    }

    private void removeEmitter(String userName, SseEmitter emitter) {
        List<SseEmitter> userEmitters = emitters.get(userName);
        if (userEmitters != null) {
            userEmitters.remove(emitter);
            if (userEmitters.isEmpty()) {
                emitters.remove(userName);
            }
        }
    }

    public void notifyClients(String userName, WffLocationTracking location) {
        List<SseEmitter> userEmitters = emitters.get(userName);
        if (userEmitters != null) {
            for (SseEmitter emitter : userEmitters) {
                try {
                    emitter.send(SseEmitter.event().name("location").data(location));
                } catch (IOException e) {
                    removeEmitter(userName, emitter);
                }
            }
        }
    }

}
