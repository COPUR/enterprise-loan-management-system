package com.enterprise.openfinance.paymentinitiation.domain.service;

import com.enterprise.openfinance.paymentinitiation.domain.model.PaymentStatus;
import com.enterprise.openfinance.paymentinitiation.domain.model.RiskAssessmentDecision;

import java.time.LocalDate;

public class PaymentStatusPolicy {

    public PaymentStatus decide(
            LocalDate processingDate,
            LocalDate requestedExecutionDate,
            RiskAssessmentDecision riskDecision
    ) {
        if (riskDecision == RiskAssessmentDecision.REJECT) {
            return PaymentStatus.REJECTED;
        }
        if (requestedExecutionDate != null && requestedExecutionDate.isAfter(processingDate)) {
            return PaymentStatus.PENDING;
        }
        return PaymentStatus.ACCEPTED_SETTLEMENT_IN_PROCESS;
    }
}
