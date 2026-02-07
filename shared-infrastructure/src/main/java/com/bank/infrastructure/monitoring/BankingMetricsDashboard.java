package com.bank.infrastructure.monitoring;

/**
 * Aggregates metrics collectors used by infrastructure dashboards.
 */
public class BankingMetricsDashboard {

    private final BankingMetricsConfiguration.BankingBusinessMetrics businessMetrics;
    private final BankingMetricsConfiguration.BankingTechnicalMetrics technicalMetrics;
    private final BankingMetricsConfiguration.BankingComplianceMetrics complianceMetrics;
    private final BankingMetricsConfiguration.IslamicBankingMetrics islamicBankingMetrics;

    public BankingMetricsDashboard(
            BankingMetricsConfiguration.BankingBusinessMetrics businessMetrics,
            BankingMetricsConfiguration.BankingTechnicalMetrics technicalMetrics,
            BankingMetricsConfiguration.BankingComplianceMetrics complianceMetrics,
            BankingMetricsConfiguration.IslamicBankingMetrics islamicBankingMetrics) {
        this.businessMetrics = businessMetrics;
        this.technicalMetrics = technicalMetrics;
        this.complianceMetrics = complianceMetrics;
        this.islamicBankingMetrics = islamicBankingMetrics;
    }

    public BankingMetricsConfiguration.BankingBusinessMetrics getBusinessMetrics() {
        return businessMetrics;
    }

    public BankingMetricsConfiguration.BankingTechnicalMetrics getTechnicalMetrics() {
        return technicalMetrics;
    }

    public BankingMetricsConfiguration.BankingComplianceMetrics getComplianceMetrics() {
        return complianceMetrics;
    }

    public BankingMetricsConfiguration.IslamicBankingMetrics getIslamicBankingMetrics() {
        return islamicBankingMetrics;
    }
}
