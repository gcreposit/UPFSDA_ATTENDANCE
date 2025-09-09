package com.example.demo.dto;

import com.example.demo.entity.Attendance;
import com.example.demo.entity.ExtraWork;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class DashboardResponse {

    private Attendance attendance;
    private ExtraWork extraWork;  // nullable if no extra work

}