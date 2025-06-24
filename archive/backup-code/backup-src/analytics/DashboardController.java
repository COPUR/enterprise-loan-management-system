package com.bank.loanmanagement.analytics;

import org.springframework.web.bind.annotation.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.http.MediaType;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.ObjectMapper;

@RestController
@RequestMapping("/api/dashboard")
@CrossOrigin(origins = "*")
public class DashboardController {
    
    @Autowired
    private RiskAnalyticsService riskAnalyticsService;
    
    @Autowired
    private ObjectMapper objectMapper;
    
    @GetMapping("/overview")
    public ResponseEntity<ObjectNode> getDashboardOverview() {
        try {
            ObjectNode overview = riskAnalyticsService.getDashboardOverview();
            return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_JSON)
                .body(overview);
        } catch (Exception e) {
            ObjectNode error = objectMapper.createObjectNode();
            error.put("status", "ERROR");
            error.put("message", e.getMessage());
            return ResponseEntity.internalServerError().body(error);
        }
    }
    
    @GetMapping("/portfolio-performance")
    public ResponseEntity<ObjectNode> getPortfolioPerformance() {
        try {
            ObjectNode performance = riskAnalyticsService.getPortfolioPerformance();
            return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_JSON)
                .body(performance);
        } catch (Exception e) {
            ObjectNode error = objectMapper.createObjectNode();
            error.put("status", "ERROR");
            error.put("message", e.getMessage());
            return ResponseEntity.internalServerError().body(error);
        }
    }
    
    @GetMapping("/alerts")
    public ResponseEntity<ObjectNode> getRealTimeAlerts() {
        try {
            ObjectNode alerts = riskAnalyticsService.getRealTimeAlerts();
            return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_JSON)
                .body(alerts);
        } catch (Exception e) {
            ObjectNode error = objectMapper.createObjectNode();
            error.put("status", "ERROR");
            error.put("message", e.getMessage());
            return ResponseEntity.internalServerError().body(error);
        }
    }
}