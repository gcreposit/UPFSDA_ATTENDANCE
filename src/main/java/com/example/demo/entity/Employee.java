package com.example.demo.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.web.multipart.MultipartFile;

@Entity
@Table(name = "employee")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Employee {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    private String name;

    @Column(unique = true, nullable = false)
    private String identityCardNo;

    private String username;

    private String designation;

//    @NotBlank
    @Pattern(regexp = "\\d{2}/\\d{2}/\\d{4}", message = "Date format should be dd/MM/yyyy")
    private String dateOfBirth;

    private String labName;
    private String officeName;

    private String mobileNumber;
    private String bloodGroup;

    @Column(columnDefinition = "TEXT")
    private String officeAddress;

    @Column(name = "home_location")
    private String homeLocation;

    @NotBlank
    private String district;

    @NotBlank
    private String tehsil;

    @NotBlank
    @Column(name = "post")
    private String post;

    private String emailAddress;

    @Column(columnDefinition = "TEXT")
    private String permanantAddress;
    private String emergencyContactNo;

    //    FACE PHOTO AND UPLOAD SIGNATURE
    @Transient
    private MultipartFile uploadSignature;

    @Transient
    private MultipartFile uploadFacePhoto;

    private String uploadSignatureImgPath;

    private String uploadFacePhotoImgPath;

    private boolean isActive = false;

}
