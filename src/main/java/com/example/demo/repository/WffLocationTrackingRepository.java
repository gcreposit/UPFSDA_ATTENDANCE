package com.example.demo.repository;

import com.example.demo.entity.WffLocationTracking;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface WffLocationTrackingRepository extends JpaRepository<WffLocationTracking,Long> {

    List<WffLocationTracking> findByUserNameOrderByTimestampAsc(String userName);

    WffLocationTracking findTopByUserNameOrderByTimestampDesc(String userName);

    @Query(value = "SELECT * FROM wff_location_tracking WHERE date = ?1 ;",nativeQuery = true)
    List<WffLocationTracking> findTrackingDataByDate(LocalDate today);


}
