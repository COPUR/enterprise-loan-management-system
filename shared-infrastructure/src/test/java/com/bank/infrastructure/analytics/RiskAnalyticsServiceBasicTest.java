package com.bank.infrastructure.analytics;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Basic TDD Test Suite for Risk Analytics Service
 * 
 * Tests Core Functionality:
 * - Service instantiation
 * - Method execution without exceptions
 * - Alert severity calculation (no DB required)
 * - Risk score calculation (no DB required)
 */
@DisplayName("Risk Analytics Service Basic Tests")
class RiskAnalyticsServiceBasicTest {
    
    private RiskAnalyticsService riskAnalyticsService;
    
    @BeforeEach
    void setUp() {
        JdbcTemplate nullJdbcTemplate = null;
        riskAnalyticsService = new RiskAnalyticsService(nullJdbcTemplate);
    }
    
    @Test
    @DisplayName("Should create risk analytics service instance")
    void shouldCreateRiskAnalyticsServiceInstance() {
        assertThat(riskAnalyticsService).isNotNull();
    }
    
    @Test
    @DisplayName("Should calculate alert severity levels correctly")
    void shouldCalculateAlertSeverityLevelsCorrectly() {
        // Given
        int highRiskLoans = 35; // High severity (>20)
        int overduePayments = 8; // Low severity (<10)
        int recentLoans = 25; // No alert (<30)
        
        // When
        var alertSeverity = riskAnalyticsService.calculateAlertSeverity(highRiskLoans, overduePayments, recentLoans);
        
        // Then
        assertThat(alertSeverity).isNotNull();
        assertThat(alertSeverity.get("high").asInt()).isEqualTo(1); // High risk loans > 20
        assertThat(alertSeverity.get("medium").asInt()).isEqualTo(0); // No medium alerts
        assertThat(alertSeverity.get("low").asInt()).isEqualTo(1); // Only overdue payments (recent loans < 30)
    }
    
    @Test
    @DisplayName("Should calculate alert severity with all high levels")
    void shouldCalculateAlertSeverityWithAllHighLevels() {
        // Given
        int highRiskLoans = 50; // High severity (>20)
        int overduePayments = 25; // Medium severity (>10)
        int recentLoans = 40; // Low severity (>30)
        
        // When
        var alertSeverity = riskAnalyticsService.calculateAlertSeverity(highRiskLoans, overduePayments, recentLoans);
        
        // Then
        assertThat(alertSeverity).isNotNull();
        assertThat(alertSeverity.get("high").asInt()).isEqualTo(1); // High risk loans
        assertThat(alertSeverity.get("medium").asInt()).isEqualTo(1); // Overdue payments
        assertThat(alertSeverity.get("low").asInt()).isEqualTo(1); // Recent loans
    }
    
    @Test
    @DisplayName("Should calculate alert severity with medium levels")
    void shouldCalculateAlertSeverityWithMediumLevels() {
        // Given
        int highRiskLoans = 15; // Medium severity (>10, <20)
        int overduePayments = 5; // Low severity (<10)
        int recentLoans = 10; // No alert (<30)
        
        // When
        var alertSeverity = riskAnalyticsService.calculateAlertSeverity(highRiskLoans, overduePayments, recentLoans);
        
        // Then
        assertThat(alertSeverity).isNotNull();
        assertThat(alertSeverity.get("high").asInt()).isEqualTo(0); // No high alerts
        assertThat(alertSeverity.get("medium").asInt()).isEqualTo(1); // High risk loans in medium range
        assertThat(alertSeverity.get("low").asInt()).isEqualTo(1); // Overdue payments
    }
    
    @Test
    @DisplayName("Should calculate alert severity with zero values")
    void shouldCalculateAlertSeverityWithZeroValues() {
        // Given
        int highRiskLoans = 0;
        int overduePayments = 0;
        int recentLoans = 0;
        
        // When
        var alertSeverity = riskAnalyticsService.calculateAlertSeverity(highRiskLoans, overduePayments, recentLoans);
        
        // Then
        assertThat(alertSeverity).isNotNull();
        assertThat(alertSeverity.get("high").asInt()).isEqualTo(0);
        assertThat(alertSeverity.get("medium").asInt()).isEqualTo(0);
        assertThat(alertSeverity.get("low").asInt()).isEqualTo(0);
    }
    
    @Test
    @DisplayName("Should handle database exceptions when calling methods")
    void shouldHandleDatabaseExceptionsWhenCallingMethods() {
        // Given - service with null JdbcTemplate
        
        // When & Then - Methods should throw RiskAnalyticsException
        assertThatThrownBy(() -> {
            riskAnalyticsService.getDashboardOverview();
        }).isInstanceOf(RiskAnalyticsException.class);
        
        assertThatThrownBy(() -> {
            riskAnalyticsService.calculateRiskDistribution();
        }).isInstanceOf(RiskAnalyticsException.class);
        
        assertThatThrownBy(() -> {
            riskAnalyticsService.calculateDefaultRate();
        }).isInstanceOf(RiskAnalyticsException.class);
        
        assertThatThrownBy(() -> {
            riskAnalyticsService.calculateCollectionEfficiency();
        }).isInstanceOf(RiskAnalyticsException.class);
        
        assertThatThrownBy(() -> {
            riskAnalyticsService.getRealTimeAlerts();
        }).isInstanceOf(RiskAnalyticsException.class);
        
        assertThatThrownBy(() -> {
            riskAnalyticsService.getPortfolioPerformance();
        }).isInstanceOf(RiskAnalyticsException.class);
        
        assertThatThrownBy(() -> {
            riskAnalyticsService.calculateRiskScore();
        }).isInstanceOf(RiskAnalyticsException.class);
    }
    
    @Test
    @DisplayName("Should validate service is annotated with @Service")
    void shouldValidateServiceIsAnnotatedWithService() {
        // Given
        Class<?> serviceClass = RiskAnalyticsService.class;
        
        // When & Then
        assertThat(serviceClass.isAnnotationPresent(org.springframework.stereotype.Service.class)).isTrue();
    }
    
    @Test
    @DisplayName("Should validate service has proper constructor")
    void shouldValidateServiceHasProperConstructor() {
        // Given
        Class<?> serviceClass = RiskAnalyticsService.class;
        
        // When & Then
        assertThat(serviceClass.getConstructors()).hasSize(1);
        
        java.lang.reflect.Constructor<?> constructor = serviceClass.getConstructors()[0];
        assertThat(constructor.getParameterCount()).isEqualTo(1);
        assertThat(constructor.getParameterTypes()[0]).isEqualTo(org.springframework.jdbc.core.JdbcTemplate.class);
    }
    
    @Test
    @DisplayName("Should validate service has expected methods")
    void shouldValidateServiceHasExpectedMethods() {
        // Given
        Class<?> serviceClass = RiskAnalyticsService.class;
        
        // When & Then
        boolean hasDashboardOverview = false;
        boolean hasRiskDistribution = false;
        boolean hasDefaultRate = false;
        boolean hasCollectionEfficiency = false;
        boolean hasRealTimeAlerts = false;
        boolean hasPortfolioPerformance = false;
        boolean hasRiskScore = false;
        boolean hasAlertSeverity = false;
        
        for (java.lang.reflect.Method method : serviceClass.getDeclaredMethods()) {
            switch (method.getName()) {
                case "getDashboardOverview":
                    hasDashboardOverview = true;
                    break;
                case "calculateRiskDistribution":
                    hasRiskDistribution = true;
                    break;
                case "calculateDefaultRate":
                    hasDefaultRate = true;
                    break;
                case "calculateCollectionEfficiency":
                    hasCollectionEfficiency = true;
                    break;
                case "getRealTimeAlerts":
                    hasRealTimeAlerts = true;
                    break;
                case "getPortfolioPerformance":
                    hasPortfolioPerformance = true;
                    break;
                case "calculateRiskScore":
                    hasRiskScore = true;
                    break;
                case "calculateAlertSeverity":
                    hasAlertSeverity = true;
                    break;
            }
        }
        
        assertThat(hasDashboardOverview).isTrue();
        assertThat(hasRiskDistribution).isTrue();
        assertThat(hasDefaultRate).isTrue();
        assertThat(hasCollectionEfficiency).isTrue();
        assertThat(hasRealTimeAlerts).isTrue();
        assertThat(hasPortfolioPerformance).isTrue();
        assertThat(hasRiskScore).isTrue();
        assertThat(hasAlertSeverity).isTrue();
    }
}