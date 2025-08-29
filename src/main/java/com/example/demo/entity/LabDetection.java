package com.example.demo.entity;


import com.fasterxml.jackson.annotation.JsonManagedReference;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@Entity
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "labDetection")
public class LabDetection {


    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    private String timestamp;

    private Integer personCount;

    @Column(columnDefinition = "LONGTEXT")
    private String personImgPath;

    private String objectCount;

    @Column(columnDefinition = "LONGTEXT")
    private String objectImgPath;


    private Boolean fireDetected = false;

    @Column(columnDefinition = "LONGTEXT")
    private String fireImgPath;

    private String frameDetectedImgPath;

    private Integer objectTotalCount;

    private String objectName;

    @Column(columnDefinition = "LONGTEXT")
    private String frameImgPath;

    @Transient
    private List<byte[]> frameData;

    @Transient
    private List<byte[]> personData;

    @Transient
    private List<byte[]> objectData;

    private String personStatus;



    @ManyToOne
    @JoinColumn(name = "lab_id", nullable = false)
    @JsonManagedReference
    private AllLabRtspUrl lab;


}
