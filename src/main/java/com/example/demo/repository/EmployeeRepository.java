package com.example.demo.repository;

import com.example.demo.entity.Employee;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface EmployeeRepository extends JpaRepository<Employee, Long> {
    
    boolean existsByIdentityCardNo(String identityCardNo);
    
    Optional<Employee> findByIdentityCardNo(String identityCardNo);

    @Query(value = "SELECT DISTINCT work_type FROM employee;",nativeQuery = true)
    List<String> findDistinctWorkType();

    @Query(value = "SELECT COUNT(*) FROM employee;",nativeQuery = true)
    List<Employee> countAllEmployees();

    Employee findByUsername(String username);

    @Query(value = "SELECT DISTINCT dst FROM dst_teh_vil ORDER BY dst;",nativeQuery = true)
    List<String> fetchAllDistricts();

    @Query(value = "SELECT DISTINCT teh FROM dst_teh_vil WHERE dst = ?1 ORDER BY teh;",nativeQuery = true)
    List<String> fetchAllTehsilByDistrict(String district);

}