package com.example.demo.entity;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
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

    private String userName;

    private Double lat;

    private Double lon;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss") // Ensures ISO string
    private LocalDateTime timestamp;

    // Optional: if you want live status in history
    private Boolean isActive;

    // Virtual property: sends { lat: ..., lng: ... } in JSON
    @JsonProperty("location")
    public Map<String, Double> getLocation() {
        return Map.of("lat", lat, "lng", lon);
    }

}