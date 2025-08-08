package com.example.demo.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "wff_location_tracking")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WffLocationTracking {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String userName;

    private String lat;

    private String lon;

    private LocalDateTime timestamp;

}