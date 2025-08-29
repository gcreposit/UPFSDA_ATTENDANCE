package com.example.demo.serviceimpl;

import com.example.demo.dto.ApiResponse;
import com.example.demo.dto.LeaveRequestDto;
import com.example.demo.entity.*;
import com.example.demo.repository.*;
import com.example.demo.service.AttendanceService;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
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
    private HolidayRepository holidayRepository;

    @Autowired
    private OfficeTimeRepository officeTimeRepository;

    @Autowired
    private LeaveRepository leaveRepository;

    private final LocationEventPublisher publisher;

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

        Employee employee = employeeRepository.findByUsername(userName);

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
        attendance.setOfficeName(employee.getOfficeName());

        OfficeTime officeTime = officeTimeRepository.getOfficeTime();

        LocalTime officeStart = officeTime.getStartTime();
        LocalTime officeEnd = officeTime.getEndTime();

        // Office timing constants
//        LocalTime officeStart = LocalTime.of(10, 0); // 10:00 AM
//        LocalTime officeEnd = LocalTime.of(18, 0);   // 6:00 PM

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

//    @Override
//    public Attendance getDashboardData(String userName, String date) {
//
//        // Validate if employee exists
//        Employee employee = employeeRepository.findByUsername(userName);
//        if (employee == null) {
//            throw new IllegalArgumentException("Employee not found for username: " + userName);
//        }
//
//        // Parse date string to LocalDate
//        LocalDate parsedDate = LocalDate.parse(date, DateTimeFormatter.ofPattern("yyyy-MM-dd"));
//
//        return attendanceRepository.findByUserNameAndDate(userName, parsedDate);
//
//    }

    @Override
    public Attendance getDashboardData(String userName, String date) {

        // 1. Validate employee exists
        Employee employee = employeeRepository.findByUsername(userName);
        if (employee == null) {
            throw new IllegalArgumentException("Employee not found for username: " + userName);
        }

        // 2. Parse date
        LocalDate parsedDate = LocalDate.parse(date, DateTimeFormatter.ofPattern("yyyy-MM-dd"));

        // 3. Check if Sunday
        if (parsedDate.getDayOfWeek().getValue() == 7) { // 7 = Sunday
            Attendance holidayAttendance = new Attendance();
            holidayAttendance.setUserName(userName);
            holidayAttendance.setDate(parsedDate);
            holidayAttendance.setStatus("Holiday");
            holidayAttendance.setReason("Weekend (Sunday)");
            holidayAttendance.setOfficeName(employee.getOfficeName());
            return holidayAttendance;
        }

        // 4. Check if holiday exists in DB
        Holidays holiday = holidayRepository.findByHolidayDate(parsedDate);
        if (holiday != null) {
            Attendance holidayAttendance = new Attendance();
            holidayAttendance.setUserName(userName);
            holidayAttendance.setDate(parsedDate);
            holidayAttendance.setStatus("Holiday");
            holidayAttendance.setReason(holiday.getName() != null ? holiday.getName() : holiday.getDescription());
            holidayAttendance.setOfficeName(employee.getOfficeName());
            return holidayAttendance;
        }

        // 5. Try to fetch leave for this date
        Leave leave = leaveRepository.findByUsernameAndDateRange(userName, parsedDate);
        if (leave != null) {
            Attendance leaveAttendance = new Attendance();
            leaveAttendance.setUserName(userName);
            leaveAttendance.setDate(parsedDate);
            leaveAttendance.setStatus("Leave");
            leaveAttendance.setReason(leave.getReason());
            leaveAttendance.setOfficeName(leave.getOfficeName());
            return leaveAttendance;
        }

        // 6. Try to fetch attendance
        Attendance attendance = attendanceRepository.findByUserNameAndDate(userName, parsedDate);
        if (attendance != null) {
            return attendance;
        }

        // 7. If nothing found → Absent
        Attendance absent = new Attendance();
        absent.setUserName(userName);
        absent.setDate(parsedDate);
        absent.setStatus("Absent");
        absent.setReason("No attendance or leave record found");
        absent.setOfficeName(employee.getOfficeName());
        return absent;
    }


//    @Override
//    @Transactional
//    public ApiResponse<Object> saveLocationForTracking(
//            String userName,
//            String lat,
//            String lon,
//            String timestamp,
//            boolean isActive
//    ) {
//        try {
//            // 1️⃣ Parse timestamp (or default to now if null/blank)
//            LocalDateTime parsedTimestamp;
//            if (timestamp != null && !timestamp.isBlank()) {
//                parsedTimestamp = LocalDateTime.parse(timestamp, DateTimeFormatter.ISO_DATE_TIME);
//            } else {
//                parsedTimestamp = LocalDateTime.now();
//            }
//
//            // 2️⃣ Find employee by username
//            Employee employee = employeeRepository.findByUsername(userName);
//
//            // 3️⃣ Update isActive in employee
//            employee.setActive(isActive);
//            employeeRepository.save(employee);
//
//            // 4️⃣ Save location tracking record
//            WffLocationTracking tracking = WffLocationTracking.builder()
//                    .userName(userName)
//                    .lat(lat != null && !lat.isBlank() ? Double.valueOf(lat) : null)
//                    .lon(lon != null && !lon.isBlank() ? Double.valueOf(lon) : null)
//                    .date(LocalDate.now())
//                    .timestamp(parsedTimestamp)
//                    .build();
//
//            WffLocationTracking saved = locationTrackingRepository.save(tracking);
//
//            publisher.publish(saved);
//
//            Map<String, Object> responseData = new HashMap<>();
//            responseData.put("flag", "success");
//
//            return ApiResponse.builder()
//                    .message("Location saved and employee status updated successfully")
//                    .statusCode(HttpStatus.OK.value())
//                    .data(responseData)
//                    .build();
//
//        } catch (DateTimeParseException e) {
//            Map<String, Object> error = new HashMap<>();
//            error.put("error", "Invalid timestamp format. Use ISO format: yyyy-MM-dd'T'HH:mm:ss");
//
//            return ApiResponse.builder()
//                    .message("Invalid timestamp format")
//                    .statusCode(HttpStatus.BAD_REQUEST.value())
//                    .data(error)
//                    .build();
//        } catch (Exception e) {
//            Map<String, Object> error = new HashMap<>();
//            error.put("error", e.getMessage());
//
//            return ApiResponse.builder()
//                    .message("Failed to save location and update employee status")
//                    .statusCode(HttpStatus.INTERNAL_SERVER_ERROR.value())
//                    .data(error)
//                    .build();
//        }
//    }

    @Override
    @Transactional
    public ApiResponse<Object> saveLocationForTracking(
            String userName,
            String lat,
            String lon,
            String timestamp,
            boolean isActive
    ) {
        try {
            // 1️⃣ Parse timestamp
            LocalDateTime parsedTimestamp =
                    (timestamp != null && !timestamp.isBlank())
                            ? LocalDateTime.parse(timestamp, DateTimeFormatter.ISO_DATE_TIME)
                            : LocalDateTime.now();

            // 2️⃣ Find employee
            Employee employee = employeeRepository.findByUsername(userName);
            if (employee == null) {
                return ApiResponse.builder()
                        .message("Employee not found")
                        .statusCode(HttpStatus.NOT_FOUND.value())
                        .data(Map.of("error", "No employee with username: " + userName))
                        .build();
            }

            // 3️⃣ Update active flag
            employee.setActive(isActive);
            employeeRepository.save(employee);

            // 4️⃣ Save location
            WffLocationTracking tracking = WffLocationTracking.builder()
                    .userName(userName)
                    .lat(lat != null && !lat.isBlank() ? Double.valueOf(lat) : null)
                    .lon(lon != null && !lon.isBlank() ? Double.valueOf(lon) : null)
                    .date(LocalDate.now())
                    .timestamp(parsedTimestamp)
                    .build();

            WffLocationTracking saved = locationTrackingRepository.save(tracking);

            // 5️⃣ Try publishing, but don't break API if it fails
            try {
                if (publisher != null) {
                    publisher.publish(saved);
                }
            } catch (Exception pubEx) {
                // log it, but don't rollback the transaction
                log.error("Failed to publish location tracking", pubEx);
            }

            // ✅ Success response
            return ApiResponse.builder()
                    .message("Location saved and employee status updated successfully")
                    .statusCode(HttpStatus.OK.value())
                    .data(Map.of("flag", "success"))
                    .build();

        } catch (DateTimeParseException e) {
            return ApiResponse.builder()
                    .message("Invalid timestamp format")
                    .statusCode(HttpStatus.BAD_REQUEST.value())
                    .data(Map.of("error", "Invalid timestamp format. Use ISO format: yyyy-MM-dd'T'HH:mm:ss"))
                    .build();
        } catch (Exception e) {
            // ⚡ Use e.toString() so you don’t get "error": null
            return ApiResponse.builder()
                    .message("Failed to save location and update employee status")
                    .statusCode(HttpStatus.INTERNAL_SERVER_ERROR.value())
                    .data(Map.of("error", e.toString()))
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


    @Override
    public Map<String, Object> getMonthlyAttendanceCount(String employeeId, int year, int month) {

        Employee employee = employeeRepository.findByUsername(employeeId);
        if (employee == null) {
            Map<String, Object> response = new HashMap<>();
            response.put("flag", "error");
            response.put("statusCode", "404");
            response.put("message", "Username not found");
            return response;
        }

        // Fetch attendance records for the given month/year
        List<Attendance> records = attendanceRepository.findByUserNameAndMonthAndYear(employeeId, year, month);

        YearMonth ym = YearMonth.of(year, month);
        LocalDate endDate = ym.atEndOfMonth(); // <-- Always end of month

        List<LocalDate> workingDays = new ArrayList<>();
        List<LocalDate> holidays = new ArrayList<>();

        // Build working days and holidays (Sundays)
        for (int day = 1; day <= endDate.getDayOfMonth(); day++) {
            LocalDate date = LocalDate.of(year, month, day);
            if (date.getDayOfWeek() == DayOfWeek.SUNDAY) {
                holidays.add(date);
            } else {
                workingDays.add(date);
            }
        }

        // Fetch holidays from DB
        List<Holidays> dbHolidays = holidayRepository.findByHolidayDateBetween(ym.atDay(1), endDate);
        for (Holidays h : dbHolidays) {
            LocalDate holidayDate = h.getHolidayDate();
            if (workingDays.contains(holidayDate)) {
                workingDays.remove(holidayDate);
                holidays.add(holidayDate);
            }
        }

        // Get attended days
        Set<LocalDate> attendedDays = records.stream()
                .map(Attendance::getDate)
                .collect(Collectors.toSet());

        // Fetch pending leaves for the month
        List<Leave> pendingLeaves = leaveRepository.findByUsernameAndStatusAndDateRange(employeeId, "PENDING", ym.atDay(1), endDate);

        // Collect all leave dates excluding holidays
        Set<LocalDate> leaveDays = new HashSet<>();
        for (Leave leave : pendingLeaves) {
            LocalDate start = leave.getStartDate().isBefore(ym.atDay(1)) ? ym.atDay(1) : leave.getStartDate();
            LocalDate end = leave.getEndDate().isAfter(endDate) ? endDate : leave.getEndDate();

            for (LocalDate date = start; !date.isAfter(end); date = date.plusDays(1)) {
                if (!holidays.contains(date)) { // exclude holidays
                    leaveDays.add(date);
                }
            }
        }

        // Calculate absent days (working days without attendance or leave)
        long absent = workingDays.stream()
                .filter(day -> !attendedDays.contains(day) && !leaveDays.contains(day))
                .count();

        long present = attendedDays.size();

        // Count attendance by status
        long onTime = records.stream().filter(r -> "On Time".equalsIgnoreCase(r.getStatus())).count();
        long lateEntry = records.stream().filter(r -> "Late Entry".equalsIgnoreCase(r.getStatus())).count();
        long halfDay = records.stream().filter(r -> "Half Day".equalsIgnoreCase(r.getStatus())).count();
        long lateAndHalf = records.stream().filter(r -> "Late & Half".equalsIgnoreCase(r.getStatus())).count();

        // Count by attendance type
        long wfh = records.stream().filter(r -> "WFH".equalsIgnoreCase(r.getAttendanceType())).count();
        long wfo = records.stream().filter(r -> "WFO".equalsIgnoreCase(r.getAttendanceType())).count();
        long wff = records.stream().filter(r -> "WFF".equalsIgnoreCase(r.getAttendanceType())).count();

        // Prepare detailed data
        Map<String, Object> data = new HashMap<>();
        data.put("on_time", onTime);
        data.put("late_entry", lateEntry);
        data.put("late_and_half", lateAndHalf);
        data.put("half_day", halfDay);
        data.put("absent", absent);
        data.put("present", present);
        data.put("total_work_from_home", wfh);
        data.put("total_work_from_office", wfo);
        data.put("total_work_from_field", wff);
        data.put("total_days_in_month", ym.lengthOfMonth());
        data.put("working_days_in_month", workingDays.size());
        data.put("holidays", holidays.size());
        data.put("holiday_dates", holidays);

        // Add leave info
        data.put("leave_days_count", leaveDays.size());
        data.put("leave_dates", leaveDays);

        // Final response
        Map<String, Object> response = new HashMap<>();
        response.put("flag", "success");
        response.put("statusCode", "200");
        response.put("message", "Monthly attendance summary retrieved successfully");
        response.put("data", data);

        return response;
    }



//    @Override
//    public Map<String, Object> getMonthlyAttendanceCount(String employeeId, int year, int month) {
//
//        Employee employee = employeeRepository.findByUsername(employeeId);
//
//        if (employee == null) {
//            Map<String, Object> response = new HashMap<>();
//            response.put("flag", "error");
//            response.put("statusCode", "404");
//            response.put("message", "Username not found");
//            return response;
//        }
//
//        // Fetch records for the given month/year
//        List<Attendance> records = attendanceRepository.findByUserNameAndMonthAndYear(employeeId, year, month);
//
//        // Step 1: Generate working days list (exclude Sundays initially)
//        YearMonth ym = YearMonth.of(year, month);
//        LocalDate today = LocalDate.now();
//        LocalDate endDate = (year == today.getYear() && month == today.getMonthValue())
//                ? today
//                : ym.atEndOfMonth();
//
//        List<LocalDate> workingDays = new ArrayList<>();
//        List<LocalDate> holidays = new ArrayList<>();
//
//        for (int day = 1; day <= endDate.getDayOfMonth(); day++) {
//            LocalDate date = LocalDate.of(year, month, day);
//            if (date.getDayOfWeek() == DayOfWeek.SUNDAY) {
//                holidays.add(date); // Sundays
//            } else {
//                workingDays.add(date);
//            }
//        }
//
//        // Step 2: Fetch holidays from DB within the month
//        List<Holidays> dbHolidays = holidayRepository.findByHolidayDateBetween(
//                ym.atDay(1), endDate
//        );
//
//        for (Holidays h : dbHolidays) {
//            LocalDate holidayDate = h.getHolidayDate();
//            if (workingDays.contains(holidayDate)) {
//                workingDays.remove(holidayDate); // remove from working days
//                holidays.add(holidayDate);      // add to holidays
//            }
//        }
//
//        // Step 3: Get attended days from records
//        Set<LocalDate> attendedDays = records.stream()
//                .map(Attendance::getDate)
//                .collect(Collectors.toSet());
//
//        // Step 4: Calculate absent days (working days without attendance)
//        long absent = workingDays.stream()
//                .filter(day -> !attendedDays.contains(day))
//                .count();
//
//        // Step 5: Calculate present days (distinct attendance records)
//        long present = attendedDays.size();
//
//        // Step 6: Count attendance by status
//        long onTime = records.stream().filter(r -> "On Time".equalsIgnoreCase(r.getStatus())).count();
//        long lateEntry = records.stream().filter(r -> "Late Entry".equalsIgnoreCase(r.getStatus())).count();
//        long halfDay = records.stream().filter(r -> "Half Day".equalsIgnoreCase(r.getStatus())).count();
//        long lateAndHalf = records.stream().filter(r -> "Late & Half".equalsIgnoreCase(r.getStatus())).count();
//
//        // Step 7: Count by attendance type
//        long wfh = records.stream().filter(r -> "WFH".equalsIgnoreCase(r.getAttendanceType())).count();
//        long wfo = records.stream().filter(r -> "WFO".equalsIgnoreCase(r.getAttendanceType())).count();
//        long wff = records.stream().filter(r -> "WFF".equalsIgnoreCase(r.getAttendanceType())).count();
//
//        // Step 8: Prepare detailed data
//        Map<String, Object> data = new HashMap<>();
//        data.put("on_time", onTime);
//        data.put("late_entry", lateEntry);
//        data.put("late_and_half", lateAndHalf);
//        data.put("half_day", halfDay);
//        data.put("absent", absent);
//        data.put("present", present);
//        data.put("total_work_from_home", wfh);
//        data.put("total_work_from_office", wfo);
//        data.put("total_work_from_field", wff);
//        data.put("total_days_in_month", ym.lengthOfMonth());
//        data.put("working_days_in_month", workingDays.size());
//        data.put("holidays", holidays.size());
//        data.put("holiday_dates", holidays); // now includes Sundays + DB holidays
//
//        // Step 9: Final response
//        Map<String, Object> response = new HashMap<>();
//        response.put("flag", "success");
//        response.put("statusCode", "200");
//        response.put("message", "Monthly attendance summary retrieved successfully");
//        response.put("data", data);
//
//        return response;
//    }


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
    public WffLocationTracking getLatest(String userName) {

        return locationTrackingRepository.findTopByUserNameOrderByTimestampDesc(userName);

    }

    @Override
    public List<WffLocationTracking> findAllLocationHistory() {

        LocalDate today = LocalDate.now();

        return locationTrackingRepository.findLatestTrackingDataByDate(today);

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

        // Step 2: Fetch all attendance records for the month
        List<Attendance> records = attendanceRepository.findByUserNameAndMonthAndYear(employeeId, year, month);

        YearMonth ym = YearMonth.of(year, month);
        LocalDate endDate = ym.atEndOfMonth(); // Use end of month instead of today

        // Step 3: Build working days and holidays (Sundays + DB holidays)
        List<LocalDate> workingDays = new ArrayList<>();
        List<LocalDate> holidays = new ArrayList<>();

        for (int day = 1; day <= endDate.getDayOfMonth(); day++) {
            LocalDate date = LocalDate.of(year, month, day);
            if (date.getDayOfWeek() == DayOfWeek.SUNDAY) {
                holidays.add(date);
            } else {
                workingDays.add(date);
            }
        }

        // Fetch holidays from DB
        List<Holidays> dbHolidays = holidayRepository.findByHolidayDateBetween(ym.atDay(1), endDate);
        for (Holidays h : dbHolidays) {
            LocalDate holidayDate = h.getHolidayDate();
            if (workingDays.contains(holidayDate)) {
                workingDays.remove(holidayDate);
                holidays.add(holidayDate);
            }
        }

        // Get attended days
        Set<LocalDate> attendedDays = records.stream()
                .map(Attendance::getDate)
                .collect(Collectors.toSet());

        // Step 3.5: Fetch pending leaves and exclude holidays
        List<Leave> pendingLeaves = leaveRepository.findByUsernameAndStatusAndDateRange(
                employeeId, "PENDING", ym.atDay(1), endDate
        );

        Set<LocalDate> leaveDays = new HashSet<>();
        for (Leave leave : pendingLeaves) {
            LocalDate start = leave.getStartDate().isBefore(ym.atDay(1)) ? ym.atDay(1) : leave.getStartDate();
            LocalDate end = leave.getEndDate().isAfter(endDate) ? endDate : leave.getEndDate();
            for (LocalDate date = start; !date.isAfter(end); date = date.plusDays(1)) {
                if (!holidays.contains(date)) { // Exclude holidays from leave
                    leaveDays.add(date);
                }
            }
        }

        // Step 4: Filter based on category
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
                List<Attendance> absentList = new ArrayList<>();
                for (LocalDate day : workingDays) {
                    if (!attendedDays.contains(day) && !leaveDays.contains(day)) {
                        Attendance absentRecord = new Attendance();
                        absentRecord.setDate(day);
                        absentRecord.setStatus("Absent");
                        absentRecord.setAttendanceType("-");
                        absentList.add(absentRecord);
                    }
                }
                filtered = absentList;
                break;

            case "leave":
                // Use DB leave data for proper details
                filtered = pendingLeaves.stream()
                        .flatMap(leave -> {
                            LocalDate start = leave.getStartDate().isBefore(ym.atDay(1)) ? ym.atDay(1) : leave.getStartDate();
                            LocalDate end = leave.getEndDate().isAfter(endDate) ? endDate : leave.getEndDate();
                            List<Attendance> leaveRecords = new ArrayList<>();
                            for (LocalDate date = start; !date.isAfter(end); date = date.plusDays(1)) {
                                if (!holidays.contains(date)) {
                                    Attendance leaveRecord = new Attendance();
                                    leaveRecord.setDate(date);
                                    leaveRecord.setStatus("Leave");
                                    leaveRecord.setAttendanceType("-");
                                    leaveRecord.setReason(leave.getReason() != null ? leave.getReason() : "-");
                                    leaveRecords.add(leaveRecord);
                                }
                            }
                            return leaveRecords.stream();
                        })
                        .collect(Collectors.toList());
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

            case "present":
                filtered = records;
                break;

            case "holiday":
                filtered = holidays.stream().map(date -> {
                    Attendance holidayRecord = new Attendance();
                    holidayRecord.setDate(date);
                    Optional<Holidays> dbHoliday = dbHolidays.stream()
                            .filter(h -> h.getHolidayDate().equals(date))
                            .findFirst();
                    holidayRecord.setStatus(dbHoliday.map(Holidays::getName).orElse("Holiday"));
                    holidayRecord.setAttendanceType("-");
                    holidayRecord.setReason(dbHoliday.map(Holidays::getDescription).orElse(""));
                    return holidayRecord;
                }).collect(Collectors.toList());
                break;

            case "working_days":
                filtered = workingDays.stream().map(date -> {
                    Attendance workDay = new Attendance();
                    workDay.setDate(date);
                    workDay.setStatus("Working Day");
                    workDay.setAttendanceType("-");
                    return workDay;
                }).collect(Collectors.toList());
                break;

            default:
                response.put("flag", "error");
                response.put("message", "Invalid category");
                return response;
        }

        // Step 5: Build details
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

        // Step 6: Prepare final response
        response.put("flag", "success");
        response.put("message", "Details retrieved successfully");
        response.put("data", details);

        // Extra summary info
        response.put("present_days", attendedDays.size());
        response.put("working_days_in_month", workingDays.size());
        response.put("holidays", holidays.size());
        response.put("leave_days_count", leaveDays.size());
        response.put("leave_dates", leaveDays);

        return response;
    }

//    @Override
//    public Map<String, Object> getMonthlyCategoryDetails(String employeeId, int year, int month, String category) {
//        Map<String, Object> response = new HashMap<>();
//
//        // Step 1: Check if employee exists
//        Employee employee = employeeRepository.findByUsername(employeeId);
//        if (employee == null) {
//            response.put("flag", "error");
//            response.put("message", "Username not found");
//            return response;
//        }
//
//        // Step 2: Fetch all records for the month
//        List<Attendance> records = attendanceRepository.findByUserNameAndMonthAndYear(employeeId, year, month);
//
//        YearMonth ym = YearMonth.of(year, month);
//        LocalDate today = LocalDate.now();
//        LocalDate endDate = (year == today.getYear() && month == today.getMonthValue())
//                ? today
//                : ym.atEndOfMonth();
//
//        // Step 3: Build working days and holidays (Sundays + DB holidays)
//        List<LocalDate> workingDays = new ArrayList<>();
//        List<LocalDate> holidays = new ArrayList<>();
//
//        for (int day = 1; day <= endDate.getDayOfMonth(); day++) {
//            LocalDate date = LocalDate.of(year, month, day);
//            if (date.getDayOfWeek() == DayOfWeek.SUNDAY) {
//                holidays.add(date);
//            } else {
//                workingDays.add(date);
//            }
//        }
//
//        // Fetch holidays from DB
//        List<Holidays> dbHolidays = holidayRepository.findByHolidayDateBetween(
//                ym.atDay(1), endDate
//        );
//
//        for (Holidays h : dbHolidays) {
//            LocalDate holidayDate = h.getHolidayDate();
//            if (workingDays.contains(holidayDate)) {
//                workingDays.remove(holidayDate);
//                holidays.add(holidayDate);
//            }
//        }
//
//        Set<LocalDate> attendedDays = records.stream()
//                .map(Attendance::getDate)
//                .collect(Collectors.toSet());
//
//        // Count values
//        long present = attendedDays.size();
//        long totalWorkingDays = workingDays.size();
//        long holidayCount = holidays.size();
//
//        // Step 4: Filter based on category
//        List<Attendance> filtered;
//        switch (category.toLowerCase()) {
//            case "on_time":
//                filtered = records.stream()
//                        .filter(r -> "On Time".equalsIgnoreCase(r.getStatus()))
//                        .collect(Collectors.toList());
//                break;
//
//            case "late_entry":
//                filtered = records.stream()
//                        .filter(r -> "Late Entry".equalsIgnoreCase(r.getStatus()))
//                        .collect(Collectors.toList());
//                break;
//
//            case "half_day":
//                filtered = records.stream()
//                        .filter(r -> "Half Day".equalsIgnoreCase(r.getStatus()))
//                        .collect(Collectors.toList());
//                break;
//
//            case "late_and_half":
//                filtered = records.stream()
//                        .filter(r -> "Late & Half".equalsIgnoreCase(r.getStatus()))
//                        .collect(Collectors.toList());
//                break;
//
//            case "absent":
//                List<Attendance> absentList = new ArrayList<>();
//                for (LocalDate day : workingDays) {
//                    if (!attendedDays.contains(day)) {
//                        Attendance absentRecord = new Attendance();
//                        absentRecord.setDate(day);
//                        absentRecord.setStatus("Absent");
//                        absentRecord.setAttendanceType("-");
//                        absentList.add(absentRecord);
//                    }
//                }
//                filtered = absentList;
//                break;
//
//            case "wfh":
//                filtered = records.stream()
//                        .filter(r -> "WFH".equalsIgnoreCase(r.getAttendanceType()))
//                        .collect(Collectors.toList());
//                break;
//
//            case "wfo":
//                filtered = records.stream()
//                        .filter(r -> "WFO".equalsIgnoreCase(r.getAttendanceType()))
//                        .collect(Collectors.toList());
//                break;
//
//            case "wff":
//                filtered = records.stream()
//                        .filter(r -> "WFF".equalsIgnoreCase(r.getAttendanceType()))
//                        .collect(Collectors.toList());
//                break;
//
//            case "present":
//                filtered = records; // all attended records count as present
//                break;
//
//            case "holiday":
//                // Sundays + DB Holidays
//                filtered = holidays.stream().map(date -> {
//                    Attendance holidayRecord = new Attendance();
//                    holidayRecord.setDate(date);
//
//                    // If from DB holiday, use its name, else just "Holiday"
//                    Optional<Holidays> dbHoliday = dbHolidays.stream()
//                            .filter(h -> h.getHolidayDate().equals(date))
//                            .findFirst();
//
//                    holidayRecord.setStatus(dbHoliday.map(Holidays::getName).orElse("Holiday"));
//                    holidayRecord.setAttendanceType("-");
//                    holidayRecord.setReason(dbHoliday.map(Holidays::getDescription).orElse(""));
//                    return holidayRecord;
//                }).collect(Collectors.toList());
//                break;
//
//            case "working_days":
//                filtered = workingDays.stream().map(date -> {
//                    Attendance workDay = new Attendance();
//                    workDay.setDate(date);
//                    workDay.setStatus("Working Day");
//                    workDay.setAttendanceType("-");
//                    return workDay;
//                }).collect(Collectors.toList());
//                break;
//
//            default:
//                response.put("flag", "error");
//                response.put("message", "Invalid category");
//                return response;
//        }
//
//        // Step 5: Build details
//        List<Map<String, Object>> details = filtered.stream().map(r -> {
//            Map<String, Object> map = new HashMap<>();
//            map.put("date", r.getDate());
//            map.put("status", r.getStatus());
//            map.put("attendance_type", r.getAttendanceType());
//            map.put("in_time", r.getMorningTime() != null ? r.getMorningTime().toString() : "");
//            map.put("out_time", r.getEveningTime() != null ? r.getEveningTime().toString() : "");
//            map.put("remarks", r.getReason() != null ? r.getReason() : "");
//            return map;
//        }).collect(Collectors.toList());
//
//        // Step 6: Prepare final response
//        response.put("flag", "success");
//        response.put("message", "Details retrieved successfully");
//        response.put("data", details);
//
//        // Extra summary info
//        response.put("present_days", present);
//        response.put("working_days_in_month", totalWorkingDays);
//        response.put("holidays", holidayCount);
//
//        return response;
//    }
//

    @Override
    public List<WffLocationTracking> getHistory(LocalDateTime from, LocalDateTime to) {
        return locationTrackingRepository.findByTimestampBetweenOrderByTimestampDesc(from, to);
    }

    @Override
    public List<WffLocationTracking> getHistoryForUser(String userName, LocalDateTime from, LocalDateTime to) {
        return locationTrackingRepository.findByUserNameAndTimestampBetweenOrderByTimestampDesc(userName, from, to);
    }

    @Override
    public List<WffLocationTracking> getLatestPerUser() {
        return locationTrackingRepository.findLatestPerUser();
    }

    @Override
    public List<WffLocationTracking> getLatestForUser(String userName) {
        return locationTrackingRepository.findLatestForUser(userName);
    }

    @Override
    public WffLocationTracking getLatestForUserOne(String userName) {

        return locationTrackingRepository.findLatestForUserOne(userName);
    }

    @Override
    public List<String> fetchEmployeesUsernames() {

        return employeeRepository.fetchEmployeesUsernames();

    }

    @Override
    @Transactional
    public Attendance attendanceDeleteById(Long id) {

        Attendance attendance = attendanceRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Attendance not found with id: " + id));

        attendanceRepository.deleteById(id);

        return attendance; // return the deleted record for confirmation
    }

    @Override
    public List<WorkTypes> fetchWorkTypes() {

        return workTypesRepository.findAll();

    }

    @Transactional
    @Override
    public String createNewWorkType(WorkTypes workTypes) {

        try {
            workTypesRepository.save(workTypes);
            return "Work type created successfully!";
        } catch (Exception e) {
            e.printStackTrace(); // log for debugging
            return "Error while creating work type: " + e.getMessage();
        }
    }

    @Override
    public void deleteWorkType(Long id) {

        if (!workTypesRepository.existsById(id)) {
            throw new RuntimeException("Work type not found with id: " + id);
        }
        workTypesRepository.deleteById(id);
    }

    @Transactional
    @Override
    public WorkTypes updateWorkType(Long id, WorkTypes updatedWorkType) {

        Optional<WorkTypes> optional = workTypesRepository.findById(id);

        if (optional.isPresent()) {
            WorkTypes existing = optional.get();
            existing.setWorkType(updatedWorkType.getWorkType());
            return workTypesRepository.save(existing);
        } else {
            throw new RuntimeException("Work type not found with id: " + id);
        }
    }

    @Transactional
    @Override
    public Holidays saveHoliday(Holidays holiday) {

        try {
            return holidayRepository.save(holiday);
        } catch (DataIntegrityViolationException ex) {
            // Example: duplicate holiday_date (unique constraint violated)
            throw new RuntimeException("Holiday already exists for date: " + holiday.getHolidayDate(), ex);
        } catch (Exception ex) {
            // Catch all other exceptions
            throw new RuntimeException("Failed to save holiday. Please try again.", ex);
        }
    }

    @Override
    public List<Holidays> fetchAllHolidays() {

        return holidayRepository.findAll();

    }

    @Transactional
    @Override
    public Holidays updateHoliday(Long id, Holidays holiday) {

        return holidayRepository.findById(id).map(existing -> {
            existing.setName(holiday.getName());
            existing.setDescription(holiday.getDescription());
            existing.setHolidayDate(holiday.getHolidayDate());
            return holidayRepository.save(existing);
        }).orElseThrow(() -> new RuntimeException("Holiday not found with id " + id));

    }

    @Override
    public void deleteHoliday(Long id) {

        if (!holidayRepository.existsById(id)) {
            throw new RuntimeException("Holiday not found with id " + id);
        }
        holidayRepository.deleteById(id);

    }

    @Override
    public List<OfficeTime> fetchAllOfficeTiming() {

        return officeTimeRepository.findAll();

    }

    @Transactional
    @Override
    public void updateOfficeTime(Long id, OfficeTime officeTime) {
        officeTimeRepository.findById(id)
                .map(existing -> {
                    existing.setStartTime(officeTime.getStartTime());
                    existing.setEndTime(officeTime.getEndTime());
                    return officeTimeRepository.save(existing);
                })
                .orElseThrow(() -> new RuntimeException("Office Time not found with id " + id));
    }

    @Override
    public Map<String, Object> getAttendanceByFilters(String officeName, String district,
                                                      LocalDate startDate, LocalDate endDate) {
        Map<String, Object> response = new HashMap<>();
        try {
            // Call repository to fetch filtered data
            List<Attendance> records = attendanceRepository.findAttendanceByFilters(
                    officeName, district, startDate, endDate
            );

            if (records.isEmpty()) {
                response.put("flag", "not_found");
                response.put("message", "No attendance records found for the given filters");
                response.put("data", Collections.emptyList()); // safer than null
            } else {
                response.put("flag", "success");
                response.put("message", "Filtered attendance fetched successfully");
                response.put("data", records);
            }
        } catch (Exception e) {
            response.put("flag", "error");
            response.put("message", "Failed to fetch filtered attendance: " + e.getMessage());
            response.put("data", null);
        }
        return response;
    }

    @Transactional
    @Override
    public Leave applyLeaveRequest(LeaveRequestDto dto) {

        // 1. Basic validations
        if (dto.getUsername() == null || dto.getUsername().isEmpty()) {
            throw new IllegalArgumentException("Username is required");
        }
        if (dto.getStartDate() == null || dto.getEndDate() == null) {
            throw new IllegalArgumentException("Leave start and end dates are required");
        }
        if (dto.getEndDate().isBefore(dto.getStartDate())) {
            throw new IllegalArgumentException("End date cannot be before start date");
        }

        // 2. Check employee exists
        Employee employee = employeeRepository.findByUsername(dto.getUsername());
        if (employee == null) {
            throw new IllegalArgumentException("Employee not found for username: " + dto.getUsername());
        }

        // 3. Fetch all pending leaves for this user
        List<Leave> existingLeaves = leaveRepository.findByUsernameAndStatus(dto.getUsername(), "PENDING");

        // 4. Check for overlapping leave dates
        for (Leave leave : existingLeaves) {
            boolean overlaps = !(dto.getEndDate().isBefore(leave.getStartDate()) || dto.getStartDate().isAfter(leave.getEndDate()));
            if (overlaps) {
                throw new IllegalArgumentException("You already have a pending leave request overlapping with the selected dates");
            }
        }

        // 5. Map DTO → Entity
        Leave leave = new Leave();
        leave.setUsername(dto.getUsername());
        leave.setOfficeName(employee.getOfficeName()); // ✅ set office name from employee
        leave.setStartDate(dto.getStartDate());
        leave.setEndDate(dto.getEndDate());
        leave.setLeaveType(dto.getLeaveType());
        leave.setDurationType(dto.getDurationType() != null ? dto.getDurationType() : "FULL DAY");
        leave.setReason(dto.getReason());
        leave.setStatus("Approved"); // 👈 better default (then Admin/Manager can approve)
        leave.setAppliedOn(LocalDateTime.now());
        leave.setUpdatedOn(LocalDateTime.now());

        // 6. Save leave
        return leaveRepository.save(leave);
    }


    @Override
    public List<Leave> fetchAllLeaves() {

        return leaveRepository.findAll();

    }

}
