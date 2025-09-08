package com.example.demo.service;

import aj.org.objectweb.asm.commons.Remapper;
import com.example.demo.dto.*;
import com.example.demo.entity.*;

import java.util.List;
import java.util.Optional;

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

    void toggleApproval(Long id);

    ExtraWork applyExtraWork(ExtraWork extraWork);

    Optional<Employee> getEmployeeByUsername(String username);

}