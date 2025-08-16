package com.example.demo.controller;

import com.example.demo.entity.Attendance;
import com.example.demo.entity.Employee;
import com.example.demo.service.AttendanceService;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.*;

@Controller
@RequestMapping("/attendance")
public class AttendanceController {
    private final AttendanceService attendanceService;

    public AttendanceController(AttendanceService attendanceService) {
        this.attendanceService = attendanceService;
    }

//    @GetMapping("/dashboard")
//    public String dashboard(Model model) {
//        // Get username from JWT token stored in localStorage (handled by client-side auth check)
//        // For now, we'll use mock data for display but real auth for access
//
//        // Mock user data for display purposes
//        Map<String, Object> user = getMockUserData("user"); // Default to employee view
//        String userRole = "employee"; // Default role
//
//        model.addAttribute("pageTitle", "Dashboard");
//        model.addAttribute("currentPage", "dashboard");
//        model.addAttribute("user", user);
//        model.addAttribute("userRole", userRole);
//        model.addAttribute("breadcrumbs", Arrays.asList(
//            Map.of("name", "Home", "url", "/attendance/dashboard"),
//            Map.of("name", "Dashboard", "url", "")
//        ));
//
//        return "dashboard/dashboard";
//    }

    @GetMapping("/dashboard")
    public String dashboard(Model model) {

        // Decide role dynamically (example: fetch from logged-in user's session or authentication)
        String username = "MasterAdmin"; // Change to dynamic retrieval later
        String userRole = username.equals("admin") ? "Administrator" : "employee";

        Map<String, Object> user = getMockUserData(username);

        model.addAttribute("pageTitle", "Dashboard");
        model.addAttribute("currentPage", "dashboard");
        model.addAttribute("user", user);
        model.addAttribute("userRole", userRole);

        // Map<String, Object> attendances = attendanceService.getDashboardDataForAdmin();
        // model.addAttribute("attendances", attendances);

        return "dashboard/dashboardNew";
    }

    @GetMapping("/attendance-details")
    public String attendanceDetails(@RequestParam("type") String type, Model model) {

        String username = "MasterAdmin"; // Change to dynamic retrieval later
        String userRole = username.equals("admin") ? "Administrator" : "employee";

        Map<String, Object> user = getMockUserData(username);

        model.addAttribute("pageTitle", "Dashboard");
        model.addAttribute("currentPage", "dashboard");
        model.addAttribute("user", user);
        model.addAttribute("userRole", userRole);

        List<Employee> records = new ArrayList<>(); // Use wildcard if mixing Employee and Attendance types

        List<Attendance> attendances = new ArrayList<>();
        ; // Use wildcard if mixing Employee and Attendance types

        if (type.equalsIgnoreCase("total_employees")) {
            records = attendanceService.fetchAllEmployeeDetails();
        } else {
            attendances = attendanceService.getAttendanceByType(type);
        }

        model.addAttribute("records", records);
        model.addAttribute("attendances", attendances);

        return "attendance/attendance-details"; // Thymeleaf view
    }

    @GetMapping("/monthly-report/{username}")
    public String showMonthlyReportPage(@PathVariable String username, Model model) {

        String usernames = "MasterAdmin"; // Change to dynamic retrieval later
        String userRole = usernames.equals("admin") ? "Administrator" : "employee";

        Map<String, Object> user = getMockUserData(usernames);

        model.addAttribute("pageTitle", "Dashboard");
        model.addAttribute("currentPage", "dashboard");
        model.addAttribute("user", user);
        model.addAttribute("userRole", userRole);

        model.addAttribute("username", username);
        return "attendance/monthly-report"; // Thymeleaf template name
    }

    @GetMapping("/projects")
    public String projects(Model model) {
        Map<String, Object> user = getMockUserData("user");
        String userRole = "employee";

        model.addAttribute("pageTitle", "Projects");
        model.addAttribute("currentPage", "projects");
        model.addAttribute("user", user);
        model.addAttribute("userRole", userRole);
        model.addAttribute("breadcrumbs", Arrays.asList(
                Map.of("name", "Home", "url", "/attendance/dashboard"),
                Map.of("name", "Projects", "url", "")
        ));

        return "projects/projects";
    }

    @GetMapping("/leave")
    public String leaveManagement(Model model) {
        Map<String, Object> user = getMockUserData("user");
        String userRole = "employee";

        model.addAttribute("pageTitle", "Leave Management");
        model.addAttribute("currentPage", "leave");
        model.addAttribute("user", user);
        model.addAttribute("userRole", userRole);
        model.addAttribute("breadcrumbs", Arrays.asList(
                Map.of("name", "Home", "url", "/attendance/dashboard"),
                Map.of("name", "Leave Management", "url", "")
        ));

        return "leave/leave-management";
    }

    @GetMapping("/team")
    public String teamManagement(Model model) {
        // Admin page - will be protected by client-side auth check
        Map<String, Object> user = getMockUserData("admin");
        String userRole = "admin";

        model.addAttribute("pageTitle", "Team Management");
        model.addAttribute("currentPage", "team");
        model.addAttribute("user", user);
        model.addAttribute("userRole", userRole);
        model.addAttribute("breadcrumbs", Arrays.asList(
                Map.of("name", "Home", "url", "/attendance/dashboard"),
                Map.of("name", "Team Management", "url", "")
        ));

        return "team/team-management";
    }

    @GetMapping("/todo")
    public String todo(Model model) {
        Map<String, Object> user = getMockUserData("user");
        String userRole = "employee";

        model.addAttribute("pageTitle", "To-Do List");
        model.addAttribute("currentPage", "todo");
        model.addAttribute("user", user);
        model.addAttribute("userRole", userRole);
        model.addAttribute("breadcrumbs", Arrays.asList(
                Map.of("name", "Home", "url", "/attendance/dashboard"),
                Map.of("name", "To-Do List", "url", "")
        ));

        return "attendance/todo";
    }

    @GetMapping("/calendar")
    public String calendar(Model model) {
        Map<String, Object> user = getMockUserData("user");
        String userRole = "employee";

        model.addAttribute("pageTitle", "Calendar");
        model.addAttribute("currentPage", "calendar");
        model.addAttribute("user", user);
        model.addAttribute("userRole", userRole);
        model.addAttribute("breadcrumbs", Arrays.asList(
                Map.of("name", "Home", "url", "/attendance/dashboard"),
                Map.of("name", "Calendar", "url", "")
        ));

        return "attendance/calendar";
    }

    @GetMapping("/location-tracking")
    public String locationTracking(Model model) {

        List<String> wffEmployees = attendanceService.fetchWffEmployees();
        model.addAttribute("wffEmployees", wffEmployees);

        String username = "MasterAdmin"; // Change to dynamic retrieval later
        String userRole = username.equals("admin") ? "Administrator" : "employee";

        Map<String, Object> user = getMockUserData(username);

        model.addAttribute("pageTitle", "Location Tracking");
        model.addAttribute("currentPage", "location");
        model.addAttribute("user", user);
        model.addAttribute("userRole", userRole);
        model.addAttribute("breadcrumbs", Arrays.asList(
                Map.of("name", "Home", "url", "/attendance/dashboard"),
                Map.of("name", "Location Tracking", "url", "")
        ));

        return "attendance/location-tracking";
    }

    // Helper methods for mock data
    private Map<String, Object> getMockUserData(String username) {
        Map<String, Object> user = new HashMap<>();

        if ("MasterAdmin".equals(username)) {
            user.put("id", "admin");
            user.put("name", "System Administrator");
            user.put("role", "Administrator");
            user.put("profileImage", "https://images.unsplash.com/photo-1472099645785-5658abf4ff4e?w=150&h=150&fit=crop&crop=face&auto=format");

            // Admin doesn't have attendance data
            Map<String, Object> attendanceToday = new HashMap<>();
            attendanceToday.put("punchIn", null);
            attendanceToday.put("punchOut", null);
            attendanceToday.put("workingHours", "00:00:00");
            attendanceToday.put("status", "Not Required");
            user.put("attendanceToday", attendanceToday);

            Map<String, Object> leaveBalance = new HashMap<>();
            leaveBalance.put("remaining", 25);
            leaveBalance.put("used", 0);
            leaveBalance.put("total", 25);
            user.put("leaveBalance", leaveBalance);

        } else {
            // Employee data (Lokesh Kumar)
            user.put("id", 1);
            user.put("name", "Lokesh Kumar");
            user.put("role", "UI/UX Designer");
            user.put("department", "Design");
            user.put("email", "lokesh@UPFSDA.com");
            user.put("profileImage", "https://images.unsplash.com/photo-1507003211169-0a1dd7228f2d?w=150&h=150&fit=crop&crop=face&auto=format");

            Map<String, Object> attendanceToday = new HashMap<>();
            attendanceToday.put("punchIn", "10:05");
            attendanceToday.put("punchOut", null);
            attendanceToday.put("workingHours", "06:43:37");
            attendanceToday.put("status", "Working");
            user.put("attendanceToday", attendanceToday);

            Map<String, Object> leaveBalance = new HashMap<>();
            leaveBalance.put("remaining", 16);
            leaveBalance.put("used", 4);
            leaveBalance.put("total", 20);
            leaveBalance.put("annual", 12);
            leaveBalance.put("sick", 6);
            leaveBalance.put("personal", 2);
            user.put("leaveBalance", leaveBalance);
        }

        return user;
    }

    private String getUserRole(String username) {
        return "MasterAdmin".equals(username) ? "admin" : "employee";
    }
}