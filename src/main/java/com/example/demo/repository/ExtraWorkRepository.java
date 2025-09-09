package com.example.demo.repository;

import com.example.demo.entity.ExtraWork;
import jakarta.validation.constraints.NotNull;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface ExtraWorkRepository extends JpaRepository<ExtraWork, Long> {


    // Check if already applied on same date
    boolean existsByUsernameAndDate(String username, LocalDate date);

    // Find all records of a user on that date (for overlap check)
    List<ExtraWork> findByUsernameAndDate(String username, LocalDate date);

}
