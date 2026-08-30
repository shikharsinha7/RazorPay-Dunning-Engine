package com.buildathon.dunning.controller;

import com.buildathon.dunning.dto.ComparisonResponse;
import com.buildathon.dunning.dto.FunnelResponse;
import com.buildathon.dunning.dto.TimelinePoint;
import com.buildathon.dunning.service.DashboardService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/dashboard")
public class DashboardController {

    private final DashboardService dashboardService;

    public DashboardController(DashboardService dashboardService) {
        this.dashboardService = dashboardService;
    }

    @GetMapping("/funnel")
    public FunnelResponse funnel() {
        return dashboardService.getFunnel();
    }

    @GetMapping("/comparison")
    public ComparisonResponse comparison() {
        return dashboardService.getComparison();
    }

    @GetMapping("/timeline")
    public List<TimelinePoint> timeline() {
        return dashboardService.getTimeline();
    }
}
