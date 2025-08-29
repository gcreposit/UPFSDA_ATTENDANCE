package com.example.demo.repository;

import com.example.demo.entity.AllAlertNotification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AllAlertNotificationRepo extends JpaRepository<AllAlertNotification,Integer> {
}
