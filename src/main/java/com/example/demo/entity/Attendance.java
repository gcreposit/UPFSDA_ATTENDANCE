package com.example.demo.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "attendance")
public class Attendance {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "attendance_type", nullable = false)
    private String attendanceType;

    @Column(name = "reason")
    private String reason;

    @Column(name = "user_name", nullable = false)
    private String userName;

    @Column(name = "attendance_date")
    private LocalDate date;

    @Column(name = "status")
    private String status;

    @Transient
    private MultipartFile morningImage;

    @Column(name = "morning_image_path")
    private String morningImagePath;

    @Column(name = "morning_time")
    private LocalDateTime morningTime;

    @Transient
    private MultipartFile eveningImage;

    @Column(name = "evening_image_path")
    private String eveningImagePath;

    @Column(name = "evening_time")
    private LocalDateTime eveningTime;

    @Transient
    private String fieldImage;

    @Column(name = "field_image_time")
    private LocalDateTime fieldImageTime;

    @Column(name = "field_image_path")
    private String fieldImagePath;

    @Column(name = "field_image_path1")
    private String fieldImagePath1;

    @Column(name = "field_image_uploaded")
    private String fieldImageUploaded;

}