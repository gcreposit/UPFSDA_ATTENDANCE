package com.example.demo.repository;

import com.example.demo.entity.OfficeTime;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface OfficeTimeRepository extends JpaRepository <OfficeTime,Long> {

    @Query(value = "SELECT * FROM office_time",nativeQuery = true)
    OfficeTime getOfficeTime();


}
