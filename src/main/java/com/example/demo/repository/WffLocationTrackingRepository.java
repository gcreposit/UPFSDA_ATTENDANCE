package com.example.demo.repository;

import com.example.demo.entity.WffLocationTracking;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface WffLocationTrackingRepository extends JpaRepository<WffLocationTracking,Long> {

    List<WffLocationTracking> findByUserNameOrderByTimestampAsc(String userName);


    WffLocationTracking findTopByUserNameOrderByTimestampDesc(String userName);


}
