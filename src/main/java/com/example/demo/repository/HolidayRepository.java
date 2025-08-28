package com.example.demo.repository;

import com.example.demo.entity.Holidays;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface HolidayRepository extends JpaRepository<Holidays,Long> {


    List<Holidays> findByHolidayDateBetween(LocalDate startDate, LocalDate endDate);

}
