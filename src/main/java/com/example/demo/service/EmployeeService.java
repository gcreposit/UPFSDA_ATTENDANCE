package com.example.demo.service;

import com.example.demo.dto.EmployeeRequest;
import com.example.demo.dto.EmployeeResponse;
import com.example.demo.entity.Employee;

import java.util.List;

public interface EmployeeService {
    
    EmployeeResponse createEmployee(EmployeeRequest request);
    
    boolean isIdentityCardUnique(String identityCardNo);
    
    Employee findByIdentityCardNo(String identityCardNo);
    
    Employee findById(Long id);

    List<String> getDistinctWorkTypes();

}