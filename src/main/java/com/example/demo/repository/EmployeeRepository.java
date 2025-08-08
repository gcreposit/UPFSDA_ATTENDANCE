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

}