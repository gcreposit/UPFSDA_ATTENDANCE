package com.example.demo.dto;


import lombok.*;

@Getter
@Setter
@ToString
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ApiResponse<T> {

    private String username;
    private String message;
    private Integer statusCode;
    private T data;

}
