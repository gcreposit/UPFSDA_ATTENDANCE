package com.example.demo.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@Entity
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "user")
public class Users {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String approvalDate;
    private String approvedBy;
    private String createdBy;
    private String creationDate;
    private String postDescription;
    private String postName;
    private String postType;
    private String postedBy;
    private String postedOn;
    private String schedulePostBy;
    private String schedulePostDate;
    private String status;
    private String uploadPath;
    private String socialMediaPlatform;
    private String postLink;

    private String videoOfyear;



}
