package com.example.demo.repository;

import com.example.demo.entity.Employee;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface EmployeeRepository extends JpaRepository<Employee, Long> {

    boolean existsByIdentityCardNo(String identityCardNo);

    Optional<Employee> findByIdentityCardNo(String identityCardNo);

    @Query(value = "SELECT COUNT(*) FROM employee;", nativeQuery = true)
    List<Employee> countAllEmployees();

    Employee findByUsername(String username);

    @Query(value = "SELECT DISTINCT dst FROM dst_teh_vil ORDER BY dst;", nativeQuery = true)
    List<String> fetchAllDistricts();

    @Query(value = "SELECT DISTINCT teh FROM dst_teh_vil WHERE dst = ?1 ORDER BY teh;", nativeQuery = true)
    List<String> fetchAllTehsilByDistrict(String district);

    boolean existsByUsername(String userName);

    boolean existsByDesignation(String designation);

    @Query(value = "SELECT employee.*, attendance.attendance_date FROM employee " +
            "JOIN attendance ON employee.username = attendance.user_name " +
            "WHERE attendance.attendance_date = ?1 ;", nativeQuery = true)
    List<Employee> findAllEmployeeDetailsForToday(LocalDate today);

    @Query(value = "SELECT username FROM employee ORDER BY username;;", nativeQuery = true)
    List<String> fetchEmployeesUsernames();

    @Query(value = "SELECT DISTINCT office_name FROM employee ORDER BY office_name;", nativeQuery = true)
    List<String> findAllOfficeNames();

    @Query(value = "SELECT * FROM employee e WHERE e.username = :username LIMIT 1", nativeQuery = true)
    Optional<Employee> findEmployeeByUsername(String username);

}