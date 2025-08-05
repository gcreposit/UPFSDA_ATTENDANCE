package com.example.demo.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class WebLoginController {

    @GetMapping("/login")
    public String login(
            @RequestParam(value = "error", required = false) String error,
            @RequestParam(value = "message", required = false) String message,
            Model model) {
        
        if (error != null) {
            model.addAttribute("error", "Invalid username or password. Please try again.");
        }
        
        if (message != null) {
            model.addAttribute("message", message);
        }
        
        return "auth/login";
    }

    @GetMapping("/attendance")
    public String home() {
        return "redirect:/attendance/dashboard";
    }
}