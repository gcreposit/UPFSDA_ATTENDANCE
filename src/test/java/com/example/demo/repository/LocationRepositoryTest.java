package com.example.demo.repository;

import com.example.demo.entity.DstTehVil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class LocationRepositoryTest {
    
    @Autowired
    private TestEntityManager entityManager;
    
    @Autowired
    private LocationRepository locationRepository;
    
    @BeforeEach
    void setUp() {
        // Create test data
        DstTehVil location1 = new DstTehVil(null, "Agra", "Agra", "Abhayapura");
        DstTehVil location2 = new DstTehVil(null, "Agra", "Agra", "Akbarpur");
        DstTehVil location3 = new DstTehVil(null, "Agra", "Fatehabad", "Akola");
        DstTehVil location4 = new DstTehVil(null, "Lucknow", "Lucknow", "Aliganj");
        
        entityManager.persist(location1);
        entityManager.persist(location2);
        entityManager.persist(location3);
        entityManager.persist(location4);
        entityManager.flush();
    }
    
    @Test
    void findAllDistricts_ShouldReturnDistinctDistricts() {
        List<String> districts = locationRepository.findAllDistricts();
        
        assertThat(districts).hasSize(2);
        assertThat(districts).containsExactlyInAnyOrder("Agra", "Lucknow");
    }
    
    @Test
    void findTehsilsByDistrict_ShouldReturnTehsilsForGivenDistrict() {
        List<String> tehsils = locationRepository.findTehsilsByDistrict("Agra");
        
        assertThat(tehsils).hasSize(2);
        assertThat(tehsils).containsExactlyInAnyOrder("Agra", "Fatehabad");
    }
    
    @Test
    void findTehsilsByDistrict_ShouldReturnEmptyListForNonExistentDistrict() {
        List<String> tehsils = locationRepository.findTehsilsByDistrict("NonExistent");
        
        assertThat(tehsils).isEmpty();
    }
}