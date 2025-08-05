package com.example.demo.repository;

import com.example.demo.entity.DstTehVil;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface LocationRepository extends JpaRepository<DstTehVil, Long> {
    
    @Query("SELECT DISTINCT d.dst FROM DstTehVil d ORDER BY d.dst")
    List<String> findAllDistricts();
    
    @Query("SELECT DISTINCT d.teh FROM DstTehVil d WHERE d.dst = :district ORDER BY d.teh")
    List<String> findTehsilsByDistrict(@Param("district") String district);
}