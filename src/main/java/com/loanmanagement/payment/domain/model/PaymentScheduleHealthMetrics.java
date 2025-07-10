package com.loanmanagement.payment.domain.model;

import java.time.LocalDateTime;

/**
 * Payment schedule health metrics
 */
public record PaymentScheduleHealthMetrics(
    String scheduleId,
    PaymentScheduleHealthStatus status,
    double onTimeRate,
    double overdueRate,
    int totalPayments,
    int onTimePayments,
    int overduePayments,
    LocalDateTime lastUpdated
) {
    public boolean isHealthy() {
        return status == PaymentScheduleHealthStatus.HEALTHY;
    }
}