package com.masrufi.framework.infrastructure.analytics;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * MasruFi Framework Analytics Configuration
 * 
 * Provides enterprise-grade analytics capabilities for Islamic banking:
 * - Sharia compliance scoring and monitoring
 * - Islamic finance product performance analytics
 * - Halal transaction pattern analysis
 * - Customer journey analytics for Islamic banking
 * - Business intelligence for Islamic finance operations
 * - Real-time monitoring and alerting
 * - Regulatory compliance reporting
 * - Risk analytics for Islamic banking products
 */
@Configuration
@EnableConfigurationProperties(MasrufiAnalyticsProperties.class)
@ConditionalOnProperty(name = "masrufi.framework.analytics.enabled", havingValue = "true", matchIfMissing = true)
public class MasrufiAnalyticsConfiguration {

    @Bean
    @ConditionalOnProperty(name = "masrufi.framework.analytics.islamic-banking.enabled", havingValue = "true")
    public IslamicBankingAnalyticsService islamicBankingAnalyticsService() {
        return new IslamicBankingAnalyticsService();
    }

    @Bean
    @ConditionalOnProperty(name = "masrufi.framework.analytics.sharia-compliance.enabled", havingValue = "true")
    public ShariaComplianceAnalyticsService shariaComplianceAnalyticsService(
            IslamicBankingAnalyticsService islamicBankingAnalyticsService) {
        return new ShariaComplianceAnalyticsService(islamicBankingAnalyticsService);
    }

    @Bean
    @ConditionalOnProperty(name = "masrufi.framework.analytics.business-intelligence.enabled", havingValue = "true")
    public IslamicBusinessIntelligenceService islamicBusinessIntelligenceService(
            IslamicBankingAnalyticsService islamicBankingAnalyticsService) {
        return new IslamicBusinessIntelligenceService(islamicBankingAnalyticsService);
    }

    @Bean
    @ConditionalOnProperty(name = "masrufi.framework.analytics.real-time-monitoring.enabled", havingValue = "true")
    public IslamicRealTimeMonitoringService islamicRealTimeMonitoringService(
            IslamicBankingAnalyticsService islamicBankingAnalyticsService) {
        return new IslamicRealTimeMonitoringService(islamicBankingAnalyticsService);
    }

    @Bean
    @ConditionalOnProperty(name = "masrufi.framework.analytics.regulatory-reporting.enabled", havingValue = "true")
    public IslamicRegulatoryReportingService islamicRegulatoryReportingService(
            IslamicBankingAnalyticsService islamicBankingAnalyticsService) {
        return new IslamicRegulatoryReportingService(islamicBankingAnalyticsService);
    }

    @Bean
    @ConditionalOnProperty(name = "masrufi.framework.analytics.risk-analytics.enabled", havingValue = "true")
    public IslamicRiskAnalyticsService islamicRiskAnalyticsService(
            IslamicBankingAnalyticsService islamicBankingAnalyticsService) {
        return new IslamicRiskAnalyticsService(islamicBankingAnalyticsService);
    }

    @Bean
    @ConditionalOnProperty(name = "masrufi.framework.analytics.customer-journey.enabled", havingValue = "true")
    public IslamicCustomerJourneyService islamicCustomerJourneyService(
            IslamicBankingAnalyticsService islamicBankingAnalyticsService) {
        return new IslamicCustomerJourneyService(islamicBankingAnalyticsService);
    }

    @Bean
    @ConditionalOnProperty(name = "masrufi.framework.analytics.product-performance.enabled", havingValue = "true")
    public IslamicProductPerformanceService islamicProductPerformanceService(
            IslamicBankingAnalyticsService islamicBankingAnalyticsService) {
        return new IslamicProductPerformanceService(islamicBankingAnalyticsService);
    }
}