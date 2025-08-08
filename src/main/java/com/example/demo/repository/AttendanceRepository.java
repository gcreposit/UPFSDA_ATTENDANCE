package com.example.demo.repository;

import com.example.demo.entity.Attendance;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.Optional;

@Repository
public interface AttendanceRepository extends JpaRepository<Attendance,Long> {


    Optional<Attendance> findTopByUserNameAndDate(String userName, LocalDate date);

    @Query(value = "SELECT * FROM attendance WHERE user_name = ?1 AND attendance_date = ?2 " ,nativeQuery = true)
    Attendance findByUserNameAndDate(String userName, LocalDate parsedDate);

}
