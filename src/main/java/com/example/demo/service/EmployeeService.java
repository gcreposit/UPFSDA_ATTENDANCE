package com.example.demo.service;

import com.example.demo.dto.EmployeeRequest;
import com.example.demo.dto.EmployeeResponse;
import com.example.demo.entity.Employee;

public interface EmployeeService {
    
    EmployeeResponse createEmployee(EmployeeRequest request);
    
    boolean isIdentityCardUnique(String identityCardNo);
    
    Employee findByIdentityCardNo(String identityCardNo);
    
    Employee findById(Long id);
}