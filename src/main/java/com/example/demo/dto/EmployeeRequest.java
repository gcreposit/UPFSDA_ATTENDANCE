package com.example.demo.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import org.springframework.web.multipart.MultipartFile;

@Data
public class EmployeeRequest {
    
    @NotBlank(message = "Employee name is required")
    private String name;
    
//    @NotBlank(message = "Date of birth is required")
    private String dateOfBirth;
    
    @NotBlank(message = "Identity card number is required")
    private String identityCardNo;
    
//    @NotBlank(message = "Address is required")
    private String address;
    
    @NotBlank(message = "Work Type is required")
    private String workType;

    private String homeLocation;

    @NotBlank(message = "District is required")
    private String district;
    
    @NotBlank(message = "Tehsil is required")
    private String tehsil;

    private String designation;

    private String mobileNumber;
    private String bloodGroup;
    private String emailAddress;
    private String emergencyContactNo;
    private String labName;
    private String officeName;
    
    @NotNull(message = "Face photo is required")
    private MultipartFile uploadFacePhoto;

    private MultipartFile uploadSignature;

}