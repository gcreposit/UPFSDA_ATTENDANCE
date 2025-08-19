package com.example.demo.repository;

import com.example.demo.entity.WffLocationTracking;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface WffLocationTrackingRepository extends JpaRepository<WffLocationTracking, Long> {

    List<WffLocationTracking> findByUserNameOrderByTimestampAsc(String userName);

    WffLocationTracking findTopByUserNameOrderByTimestampDesc(String userName);

    @Query(value = """
                SELECT w.*
                FROM wff_location_tracking w
                INNER JOIN (
                    SELECT user_name, MAX(timestamp) AS latest_time
                    FROM wff_location_tracking
                    WHERE date = ?1
                    GROUP BY user_name
                ) latest
                ON w.user_name = latest.user_name
                AND w.timestamp = latest.latest_time
            """, nativeQuery = true)
    List<WffLocationTracking> findLatestTrackingDataByDate(LocalDate today);

    // Get history for all users in a time window
    List<WffLocationTracking> findByTimestampBetweenOrderByTimestampDesc(
            LocalDateTime from,
            LocalDateTime to
    );

    // Get history for a specific user in a time window
    List<WffLocationTracking> findByUserNameAndTimestampBetweenOrderByTimestampDesc(
            String userName,
            LocalDateTime from,
            LocalDateTime to
    );

    // Latest record per user
    @Query("""
                SELECT w FROM WffLocationTracking w
                WHERE w.timestamp = (
                    SELECT MAX(w2.timestamp) FROM WffLocationTracking w2
                    WHERE w2.userName = w.userName
                )
            """)
    List<WffLocationTracking> findLatestPerUser();

    // All records for a user, newest first
    @Query("""
                SELECT w FROM WffLocationTracking w
                WHERE w.userName = :userName
                ORDER BY w.timestamp DESC
            """)
    List<WffLocationTracking> findLatestForUser(@Param("userName") String userName);

    @Query("""
                SELECT w FROM WffLocationTracking w
                WHERE w.userName = :userName
                ORDER BY w.timestamp DESC LIMIT 1
            """)
    WffLocationTracking findLatestForUserOne(@Param("userName") String userName);


}
