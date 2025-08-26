package com.example.demo.service;

import com.example.demo.dto.ApiResponse;
import com.example.demo.entity.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public interface AttendanceService {

//    Attendance saveAttendance(String userName, MultipartFile image, String timeStr, String attendanceType, String reason, MultipartFile[] fieldImage) throws IOException;

    Attendance saveAttendance(String userName, MultipartFile image, String attendanceType, String reason) throws IOException;

    Attendance getDashboardData(String userName, String date);

    ApiResponse<Object> saveLocationForTracking(String userName, String lat, String lon, String timestamp,boolean isActive);

    String uploadFieldImages(String username, MultipartFile fieldImage,MultipartFile fieldImage1) throws IOException;

    Map<String, Object> getMonthlyAttendanceCount(String username, int year, int month);

    Optional<Map<String, Object>> getDashboardDataForAdmin();

    List<Attendance> getAttendanceByType(String type);

    List<Employee> fetchAllEmployeeDetails();

    Optional<Attendance> findById(Long id);

    Employee getDetailsByUsername(String username);

    List<String> getDistricts();

    List<String> getTehsilByDistrict(String district);

    List<String> fetchWffEmployees();

    List<WffLocationTracking> fetchWffEmployeesLocationHistory(String userName);

    WffLocationTracking getLatest(String userName);

    List<WffLocationTracking> findAllLocationHistory();

    Map<String, Object> getMonthlyCategoryDetails(String username, int year, int month, String category);

    List<WffLocationTracking> getHistoryForUser(String userName, LocalDateTime fromTs, LocalDateTime toTs);

    List<WffLocationTracking> getHistory(LocalDateTime fromTs, LocalDateTime toTs);

    List<WffLocationTracking> getLatestPerUser();

    List<WffLocationTracking> getLatestForUser(String userName);

    WffLocationTracking getLatestForUserOne(String userName);

    List<String> fetchEmployeesUsernames();

    Attendance attendanceDeleteById(Long id);

    List<WorkTypes> fetchWorkTypes();

    String createNewWorkType(WorkTypes workTypes);

    void deleteWorkType(Long id);

    WorkTypes updateWorkType(Long id, WorkTypes workTypes);

    Holidays saveHoliday(Holidays holiday);

    List<Holidays> fetchAllHolidays();

    Holidays updateHoliday(Long id, Holidays holiday);

    void deleteHoliday(Long id);

    List<OfficeTime> fetchAllOfficeTiming();

    void updateOfficeTime(Long id, OfficeTime officeTime);

    Map<String, Object> getAttendanceByFilters(String officeName, String district, LocalDate startDate, LocalDate endDate);

}
