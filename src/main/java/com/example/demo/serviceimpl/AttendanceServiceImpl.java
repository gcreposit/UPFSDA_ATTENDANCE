package com.example.demo.serviceimpl;

import com.example.demo.dto.ApiResponse;
import com.example.demo.entity.Attendance;
import com.example.demo.entity.WffLocationTracking;
import com.example.demo.repository.AttendanceRepository;
import com.example.demo.repository.WffLocationTrackingRepository;
import com.example.demo.service.AttendanceService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import java.io.File;
import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class AttendanceServiceImpl implements AttendanceService {


    @Value("${file.storage.path}")
    private String uploadPath;

    @Autowired
    private AttendanceRepository attendanceRepository;

    @Autowired
    private WffLocationTrackingRepository locationTrackingRepository;

    @Override
    @Transactional
    public Attendance saveAttendance(String userName,
                                     MultipartFile image,
                                     String timeStr,
                                     String attendanceTypeFromRequest,
                                     String reason,
                                     MultipartFile[] fieldImagePaths) throws IOException {

        LocalDate today = LocalDate.now();
        Optional<Attendance> existingAttendanceOpt = attendanceRepository.findTopByUserNameAndDate(userName, today);
        Attendance attendance = existingAttendanceOpt.orElse(new Attendance());

        // Set attendance type only once (when creating new record)
        if (attendance.getId() == null) {
            attendance.setAttendanceType(attendanceTypeFromRequest);
        }

        String effectiveAttendanceType = attendance.getAttendanceType(); // Always use DB value

        // Prevent double attendance marking
        if (attendance.getMorningImagePath() != null && attendance.getEveningImagePath() != null &&
                attendance.getMorningTime() != null && attendance.getEveningTime() != null) {
            throw new IllegalStateException("User has already marked attendance for today.");
        }

        attendance.setUserName(userName);
        attendance.setReason(reason);

        // Parse incoming time (if any)
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        LocalDateTime parsedTime = (timeStr != null && !timeStr.isEmpty())
                ? LocalDateTime.parse(timeStr, formatter)
                : null;

        // Office timing constants
        LocalTime officeStart = LocalTime.of(10, 0); // 10:00 AM
        LocalTime officeEnd = LocalTime.of(18, 0);   // 6:00 PM

        // 🟢 Mark Morning Attendance
        if (attendance.getMorningImagePath() == null || attendance.getMorningTime() == null) {
            if (image != null && !image.isEmpty()) {
                String morningPath = saveImageToDisk(image);
                LocalDateTime morningTime = (parsedTime != null) ? parsedTime : LocalDateTime.now();

                attendance.setMorningImagePath(morningPath);
                attendance.setMorningTime(morningTime);
                attendance.setStatus(
                        morningTime.toLocalTime().isAfter(officeStart) ? "Late Entry" : "On Time"
                );
            }
        }

        // 🟢 Mark Evening Attendance
        else if (image != null && !image.isEmpty()) {
            String eveningPath = saveImageToDisk(image);
            LocalDateTime eveningTime = (parsedTime != null) ? parsedTime : LocalDateTime.now();

            attendance.setEveningImagePath(eveningPath);
            attendance.setEveningTime(eveningTime);

            if (eveningTime.toLocalTime().isBefore(officeEnd)) {
                if ("Late Entry".equals(attendance.getStatus())) {
                    attendance.setStatus("Late & Half");
                } else if ("On Time".equals(attendance.getStatus())) {
                    attendance.setStatus("Half Day");
                }
            }
        }

        // 🟢 Field Images Handling for WFF
        if ("WFF".equalsIgnoreCase(effectiveAttendanceType)) {

            // ❌ Validation: Require morning attendance first
            if ((attendance.getMorningImagePath() == null || attendance.getMorningTime() == null)
                    && fieldImagePaths != null && fieldImagePaths.length > 0) {
                throw new IllegalStateException("You must mark morning attendance before uploading field images.");
            }

            // ✅ Upload field images only if present
            if (fieldImagePaths != null && fieldImagePaths.length > 0) {
                List<String> savedPaths = new ArrayList<>();

                for (MultipartFile fieldImg : fieldImagePaths) {
                    if (fieldImg != null && !fieldImg.isEmpty()) {
                        String path = saveImageToDisk(fieldImg);
                        savedPaths.add(path);
                    }
                }

                if (!savedPaths.isEmpty()) {
                    attendance.setFieldImagePath(String.join(",", savedPaths));
                    attendance.setFieldImageUploaded("Images Added");
                } else {
                    attendance.setFieldImageUploaded("Not Added");
                }
            } else {
                attendance.setFieldImageUploaded("Not Added");
            }
        }

        // Set date only on creation
        if (attendance.getDate() == null) {
            attendance.setDate(today);
        }

        return attendanceRepository.save(attendance);
    }

//    private String saveImageToDisk(MultipartFile file) throws IOException {
//        String filename = UUID.randomUUID() + "_" + file.getOriginalFilename();
//        File dest = new File(uploadPath + filename);
//        dest.getParentFile().mkdirs();
//        file.transferTo(dest);
//        return dest.getAbsolutePath();
//    }

    private String saveImageToDisk(MultipartFile file) throws IOException {
        String filename = UUID.randomUUID() + "_" + file.getOriginalFilename();
        File dest = new File(uploadPath + filename);
        dest.getParentFile().mkdirs();
        file.transferTo(dest);
        return filename;
    }


    @Override
    public Attendance getDashboardData(String userName, String date) {

        // Parse date string to LocalDate
        LocalDate parsedDate = LocalDate.parse(date, DateTimeFormatter.ofPattern("yyyy-MM-dd"));

        return attendanceRepository.findByUserNameAndDate(userName, parsedDate);

    }

    @Override
    public ApiResponse<Object> saveLocationForTracking(String userName, String lat, String lon, String timestamp) {

        try {
            LocalDateTime parsedTimestamp = LocalDateTime.parse(timestamp, DateTimeFormatter.ISO_DATE_TIME);

            WffLocationTracking tracking = WffLocationTracking.builder()
                    .userName(userName)
                    .lat(lat)
                    .lon(lon)
                    .timestamp(parsedTimestamp)
                    .build();

            locationTrackingRepository.save(tracking);

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

}
