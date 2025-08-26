package com.example.demo.repository;

import com.example.demo.entity.Attendance;
import io.lettuce.core.dynamic.annotation.Param;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface AttendanceRepository extends JpaRepository<Attendance, Long> {

    Optional<Attendance> findTopByUserNameAndDate(String userName, LocalDate date);

    @Query(value = "SELECT * FROM attendance WHERE user_name = ?1 AND attendance_date = ?2 ", nativeQuery = true)
    Attendance findByUserNameAndDate(String userName, LocalDate parsedDate);

    @Query(value = "SELECT * FROM attendance " +
            "WHERE user_name = ?1 " +
            "AND YEAR(attendance_date) = ?2 " +
            "AND MONTH(attendance_date) = ?3",
            nativeQuery = true)
    List<Attendance> findByUserNameAndMonthAndYear(String userName, int year, int month);

    @Query(value = "SELECT * FROM attendance WHERE attendance_date = ?1 ", nativeQuery = true)
    List<Attendance> findAttendanceByDate(LocalDate date);

    @Query(value = "SELECT * FROM attendance WHERE status = 'On Time' AND attendance_date =?1 ", nativeQuery = true)
    List<Attendance> findOnTimeEmployees(LocalDate date);

    @Query(value = "SELECT * FROM attendance WHERE status = 'Late Entry' AND attendance_date =?1 ", nativeQuery = true)
    List<Attendance> findLateEmployees(LocalDate date);

    @Query(value = "SELECT * FROM attendance WHERE status = 'Absent' AND attendance_date =?1 ", nativeQuery = true)
    List<Attendance> findAbsentEmployees(LocalDate date);

    @Query(value = "SELECT * FROM attendance WHERE status = 'Half Day' AND attendance_date =?1 ", nativeQuery = true)
    List<Attendance> findHalfDayEmployees(LocalDate date);

    @Query(value = "SELECT * FROM attendance WHERE status = 'Late & Half' AND attendance_date =?1 ", nativeQuery = true)
    List<Attendance> findLateAndHalfDayEmployees(LocalDate date);

    @Query(value = "SELECT * FROM attendance WHERE attendance_type = 'WFH' AND attendance_date =?1 ", nativeQuery = true)
    List<Attendance> findWfhEmployees(LocalDate date);

    @Query(value = "SELECT * FROM attendance WHERE attendance_type = 'WFF' AND attendance_date =?1 ", nativeQuery = true)
    List<Attendance> findWffEmployees(LocalDate date);

    @Query(value = "SELECT * FROM attendance WHERE attendance_type = 'WFO' AND attendance_date =?1 ", nativeQuery = true)
    List<Attendance> findWfoEmployees(LocalDate date);

    @Query(value = "SELECT user_name FROM attendance WHERE attendance_date = ?1 ", nativeQuery = true)
    List<String> findUserNamesByDate(LocalDate date);

    @Query(value = "SELECT * FROM attendance WHERE attendance_date = ?1", nativeQuery = true)
    List<Attendance> findTodayPresentEmployees(LocalDate date);

    @Query(value = "SELECT * FROM attendance WHERE user_name = ?1", nativeQuery = true)
    Attendance findByUsername(String employeeId);


    @Query(value = "SELECT a.* " +
            "FROM attendance a " +
            "JOIN employee e ON a.user_name = e.username " +
            "WHERE (:officeName IS NULL OR e.office_name = :officeName) " +
            "AND (:district IS NULL OR e.district = :district) " +
            "AND (:startDate IS NULL OR a.attendance_date >= :startDate) " +
            "AND (:endDate IS NULL OR a.attendance_date <= :endDate)",
            nativeQuery = true)
    List<Attendance> findAttendanceByFilters(
            @Param("officeName") String officeName,
            @Param("district") String district,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate
    );

}
