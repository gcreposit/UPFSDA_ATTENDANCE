package com.example.demo.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Entity
@Table(name = "extra_work")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ExtraWork {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull(message = "Username is required")
    private String username;

    @Column(name = "office_name")
    @NotNull(message = "Office Name is required")
    private String officeName;

    @Column(name = "date", nullable = false)
    private LocalDate date;

    private LocalTime startTime;  // Changed from LocalDateTime
    private LocalTime endTime;    // Changed from LocalDateTime

    @Column(name = "reason", length = 500)
    private String reason;

}
