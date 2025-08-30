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

    @Column(unique = true, nullable = false,name = "identity_card_no")
    private String identityCardNo;

    @Column(name = "username")
    private String username;

    @Column(name = "product_type")
    private String productType;

    @Column(name = "office_type")
    private String officeType;

    @Column(name = "designation")
    private String designation;

    //    @NotBlank
    @Pattern(regexp = "\\d{2}/\\d{2}/\\d{4}", message = "Date format should be dd/MM/yyyy")
    @Column(name = "date_of_birth")
    private String dateOfBirth;

    @Column(name = "lab_name")
    private String labName;

    @Column(name = "office_name")
    private String officeName;

    @Column(name = "mobile_number")
    private String mobileNumber;

    @Column(name = "blood_group")
    private String bloodGroup;

    @Column(columnDefinition = "TEXT",name = "office_address")
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

    @Column(name = "email_address")
    private String emailAddress;

    @Column(columnDefinition = "TEXT",name = "parmanent_address")
    private String permanantAddress;

    @Column(name = "emergency_contact_no")
    private String emergencyContactNo;

    //    FACE PHOTO AND UPLOAD SIGNATURE
    @Transient
    private MultipartFile uploadSignature;

    @Transient
    private MultipartFile uploadFacePhoto;

    @Column(name = "upload_signature_img_path")
    private String uploadSignatureImgPath;

    @Column(name = "upload_face_photo_img_path")
    private String uploadFacePhotoImgPath;

    @Column(name = "is_active")
    private boolean isActive = false;

}
