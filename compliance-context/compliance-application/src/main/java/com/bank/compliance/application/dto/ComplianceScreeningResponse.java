package com.bank.compliance.application.dto;

import com.bank.compliance.domain.ComplianceResult;

import java.time.Instant;
import java.util.List;

public record ComplianceScreeningResponse(
        String screeningId,
        String transactionId,
        String customerId,
        String decision,
        List<String> reasons,
        Instant checkedAt
) {
    public static ComplianceScreeningResponse from(ComplianceResult result) {
        return new ComplianceScreeningResponse(
                result.getId().getValue(),
                result.getTransactionId(),
                result.getCustomerId(),
                result.getDecision().name(),
                result.getReasons(),
                result.getCheckedAt()
        );
    }
}
