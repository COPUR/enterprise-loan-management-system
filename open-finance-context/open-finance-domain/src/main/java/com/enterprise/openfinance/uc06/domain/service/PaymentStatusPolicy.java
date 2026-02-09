package com.enterprise.openfinance.uc06.domain.service;

import com.enterprise.openfinance.uc06.domain.model.PaymentStatus;
import com.enterprise.openfinance.uc06.domain.model.RiskAssessmentDecision;

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
