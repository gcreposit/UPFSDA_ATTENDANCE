package com.example.demo.controller;

import com.example.demo.service.AllAlertNotificationService;
import com.example.demo.service.AllLabRtspUrlService;
import com.example.demo.service.LabDetectionService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.Optional;

@Slf4j
@Controller
@RequestMapping("/upfsdaMonitoring")
public class MonitoringController {

    // This class will handle page navigation and rendering
    // You can add methods to return views or redirect to other pages
    // For example, you might have a method like this:

    private final AllAlertNotificationService allAlertNotificationService;
    private final AllLabRtspUrlService allLabRtspUrlService;
    private final LabDetectionService labDetectionService;


    public MonitoringController(
            AllAlertNotificationService allAlertNotificationService,
            AllLabRtspUrlService allLabRtspUrlService,
            LabDetectionService labDetectionService
    ) {
        this.allAlertNotificationService = allAlertNotificationService;
        this.allLabRtspUrlService = allLabRtspUrlService;
        this.labDetectionService = labDetectionService;

    }

    @GetMapping("/login")
    public String homePage() {
        return "Home/Login"; // This would return the home view
    }

    @GetMapping("/dashboard")
    public String dashboardPage() {
        return "Home/Dashboard"; // This would return the dashboard view
    }

    @GetMapping("/labProfilePage")
    public String labProfilePage(@RequestParam(name = "labName", required = false) String labName,
                                 @RequestParam(name = "id", required = false) Long id, Model model) {


        labDetectionService.findAll();
        model.addAttribute("allLabDetectionData", labDetectionService.findAll());
        return "Home/LabProfilePage"; // This would return the lab profile view
    }

    @GetMapping("/labDetectionSummary")
    public String labDetectionSummary() {
        return "Home/LabDetetctionSummary"; // This would return the lab detection summary view
    }

    @GetMapping("/timeLineNavigation")
    public String timeLineNavigation(@RequestParam(name = "id") Optional<Long> id) {

        return "Home/DetectionTimelinepage"; // This would return the timeline navigation view
    }


    @GetMapping("/crowdDetedtion")
    public String crowdDetection() {
        return "Home/CrowdDetection"; // This would return the crowd detection view
    }

    @GetMapping("/labDashboard")
    public String labDashboard(Model model) {


        allLabRtspUrlService.findAll();
        model.addAttribute("allLabRtspUrlData", allLabRtspUrlService.findAll());
        return "Home/LabDashboard"; // This would return the lab dashboard view
    }

    @GetMapping("/allAlertDashboard")
    public String allAlertDashboard() {
        return "Home/AllAlertDashboard"; // This would return the all alert dashboard view
    }

}
