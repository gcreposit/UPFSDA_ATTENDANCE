package com.example.demo.service;

import com.example.demo.dto.*;
import com.example.demo.entity.Employee;
import com.example.demo.entity.LeaveType;
import com.example.demo.entity.OfficeName;
import com.example.demo.entity.OfficeType;

import java.util.List;

public interface EmployeeService {
    
    EmployeeResponse createEmployee(EmployeeRequest request);
    
    boolean isIdentityCardUnique(String identityCardNo);
    
    Employee findByIdentityCardNo(String identityCardNo);
    
    Employee findById(Long id);

    List<String> getDistinctWorkTypes();

    List<Employee> findAllEmployeeDetails();

    Employee updateEmployeeProfile(Long id, String dateOfBirth, String labName, String officeName, String mobileNumber, String bloodGroup, String officeAddress, String homeLocation, String emailAddress, String permanantAddress, String emergencyContactNo);

    List<String> getOfficeNames();

    OfficeType saveOfficeType(OfficeTypeDto officeType);

    List<OfficeType> getAllOfficeTypes();

    OfficeName saveOfficeName(OfficeNameDto dto);

    List<OfficeName> getAllOfficeNames();

    LeaveType saveLeaveType(LeaveTypeDto dto);

    List<LeaveType> getAllLeaveTypes();

    List<String> getPosts();

    List<String> getAllEmployees();


}