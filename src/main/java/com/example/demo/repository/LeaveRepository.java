package com.example.demo.repository;

import com.example.demo.entity.Leave;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface LeaveRepository extends JpaRepository<Leave, Long> {


    @Query(
            value = "SELECT * FROM leave_requests " +
                    "WHERE username = :username " +
                    "AND status = :status " +
                    "AND start_date <= :endDate " +
                    "AND end_date >= :startDate",
            nativeQuery = true
    )
    List<Leave> findByUsernameAndStatusAndDateRange(@Param("username") String username, @Param("status") String status,
            @Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate
    );

    List<Leave> findByUsernameAndStatus(String username, String status);

}
