package com.example.demo.entity;


import com.fasterxml.jackson.annotation.JsonManagedReference;
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
@Table(name = "allAlertNotification")
public class AllAlertNotification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;


    private String alertMessage;

    private String generatedTimeStamp;

    private String frameImgPath;

    private String date;  // Format: dd/mm/yyyy

    @Transient
    private Byte[] frameData;


    @ManyToOne
    @JoinColumn(name = "lab_id", nullable = false)
    @JsonManagedReference
    private AllLabRtspUrl lab;


}
