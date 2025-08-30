package com.example.demo.entity;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.Map;

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

    @Column(name = "user_name")
    private String userName;

    private Double lat;

    private Double lon;

    @Column(name = "date")
    private LocalDate date;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss") // Ensures ISO string
    private LocalDateTime timestamp;

    @JsonProperty("location")
    public Map<String, Double> getLocation() {
        if (lat == null || lon == null) {
            return null;
        }
        return Map.of("lat", lat, "lng", lon);
    }


}