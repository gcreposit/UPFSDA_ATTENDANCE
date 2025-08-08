package com.example.demo.service;

import com.example.demo.dto.ApiResponse;
import com.example.demo.entity.Attendance;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

public interface AttendanceService {

    Attendance saveAttendance(String userName, MultipartFile image, String timeStr,String attendanceType,String reason,MultipartFile[] fieldImage) throws IOException;

    Attendance getDashboardData(String userName, String date);

    ApiResponse<Object> saveLocationForTracking(String userName, String lat, String lon, String timestamp);

}
