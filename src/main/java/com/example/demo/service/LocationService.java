package com.example.demo.service;

import com.example.demo.repository.LocationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class LocationService {
    
    private final LocationRepository locationRepository;
    
    @Cacheable(value = "districts", unless = "#result.isEmpty()")
    public List<String> getAllDistricts() {
        log.debug("Fetching all districts from database");
        List<String> districts = locationRepository.findAllDistricts();
        log.debug("Found {} districts", districts.size());
        return districts;
    }
    
    @Cacheable(value = "tehsils", key = "#district", unless = "#result.isEmpty()")
    public List<String> getTehsilsByDistrict(String district) {
        log.debug("Fetching tehsils for district: {}", district);
        if (district == null || district.trim().isEmpty()) {
            log.warn("District parameter is null or empty");
            return List.of();
        }
        
        List<String> tehsils = locationRepository.findTehsilsByDistrict(district.trim());
        log.debug("Found {} tehsils for district: {}", tehsils.size(), district);
        return tehsils;
    }
    
    public boolean isValidDistrict(String district) {
        if (district == null || district.trim().isEmpty()) {
            return false;
        }
        return getAllDistricts().contains(district.trim());
    }
    
    public boolean isValidTehsilForDistrict(String district, String tehsil) {
        if (district == null || tehsil == null || 
            district.trim().isEmpty() || tehsil.trim().isEmpty()) {
            return false;
        }
        return getTehsilsByDistrict(district.trim()).contains(tehsil.trim());
    }
}