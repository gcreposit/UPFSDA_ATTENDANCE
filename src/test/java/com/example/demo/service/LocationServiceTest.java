package com.example.demo.service;

import com.example.demo.repository.LocationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LocationServiceTest {
    
    @Mock
    private LocationRepository locationRepository;
    
    @InjectMocks
    private LocationService locationService;
    
    private List<String> mockDistricts;
    private List<String> mockTehsils;
    
    @BeforeEach
    void setUp() {
        mockDistricts = Arrays.asList("Agra", "Lucknow", "Kanpur");
        mockTehsils = Arrays.asList("Agra", "Fatehabad", "Kiraoli");
    }
    
    @Test
    void getAllDistricts_ShouldReturnAllDistricts() {
        when(locationRepository.findAllDistricts()).thenReturn(mockDistricts);
        
        List<String> result = locationService.getAllDistricts();
        
        assertThat(result).hasSize(3);
        assertThat(result).containsExactlyElementsOf(mockDistricts);
    }
    
    @Test
    void getTehsilsByDistrict_ShouldReturnTehsilsForValidDistrict() {
        when(locationRepository.findTehsilsByDistrict("Agra")).thenReturn(mockTehsils);
        
        List<String> result = locationService.getTehsilsByDistrict("Agra");
        
        assertThat(result).hasSize(3);
        assertThat(result).containsExactlyElementsOf(mockTehsils);
    }
    
    @Test
    void getTehsilsByDistrict_ShouldReturnEmptyListForNullDistrict() {
        List<String> result = locationService.getTehsilsByDistrict(null);
        
        assertThat(result).isEmpty();
    }
    
    @Test
    void getTehsilsByDistrict_ShouldReturnEmptyListForEmptyDistrict() {
        List<String> result = locationService.getTehsilsByDistrict("");
        
        assertThat(result).isEmpty();
    }
    
    @Test
    void isValidDistrict_ShouldReturnTrueForValidDistrict() {
        when(locationRepository.findAllDistricts()).thenReturn(mockDistricts);
        
        boolean result = locationService.isValidDistrict("Agra");
        
        assertThat(result).isTrue();
    }
    
    @Test
    void isValidDistrict_ShouldReturnFalseForInvalidDistrict() {
        when(locationRepository.findAllDistricts()).thenReturn(mockDistricts);
        
        boolean result = locationService.isValidDistrict("InvalidDistrict");
        
        assertThat(result).isFalse();
    }
    
    @Test
    void isValidTehsilForDistrict_ShouldReturnTrueForValidTehsil() {
        when(locationRepository.findTehsilsByDistrict("Agra")).thenReturn(mockTehsils);
        
        boolean result = locationService.isValidTehsilForDistrict("Agra", "Fatehabad");
        
        assertThat(result).isTrue();
    }
    
    @Test
    void isValidTehsilForDistrict_ShouldReturnFalseForInvalidTehsil() {
        when(locationRepository.findTehsilsByDistrict("Agra")).thenReturn(mockTehsils);
        
        boolean result = locationService.isValidTehsilForDistrict("Agra", "InvalidTehsil");
        
        assertThat(result).isFalse();
    }
}