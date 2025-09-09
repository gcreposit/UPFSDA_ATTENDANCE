package com.example.demo.dto;


import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;

@Data
public class AttendanceFilterRequest {

    private String officeName;
    private String attendanceType; // Morning/Evening/Field etc.
    private String userName;
    private String status; // Present/Absent/etc.

    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private LocalDate fromDate;

    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private LocalDate toDate;

}
