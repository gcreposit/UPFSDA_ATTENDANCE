package com.example.demo.repository;

import com.example.demo.entity.WffLocationTracking;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface WffLocationTrackingRepository extends JpaRepository<WffLocationTracking,Long> {

}
