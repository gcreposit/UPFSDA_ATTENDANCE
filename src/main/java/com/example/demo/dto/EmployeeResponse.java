package com.example.demo.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class EmployeeResponse {

    private Long id;
    private String name;
    private String identityCardNo;
    private String message;
    private boolean success;

    public static EmployeeResponse success(Long id, String name, String identityCardNo, String message) {
        return new EmployeeResponse(id, name, identityCardNo, message, true);
    }

    public static EmployeeResponse error(String message) {
        return new EmployeeResponse(null, null, null, message, false);
    }

}