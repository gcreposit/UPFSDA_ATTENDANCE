package com.example.demo.repository;

import com.example.demo.entity.OfficeTime;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface OfficeTimeRepository extends JpaRepository <OfficeTime,Long> {

}
