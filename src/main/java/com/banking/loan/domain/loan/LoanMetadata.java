package com.banking.loan.domain.loan;

import java.time.LocalDateTime;
import java.util.Map;

public record LoanMetadata(
    String tenantId,
    String originatingChannel,
    LocalDateTime createdAt,
    LocalDateTime updatedAt,
    String createdBy,
    String updatedBy,
    Map<String, Object> additionalData
) {
    public static LoanMetadata create(String tenantId, String createdBy) {
        return new LoanMetadata(
            tenantId,
            "API",
            LocalDateTime.now(),
            LocalDateTime.now(),
            createdBy,
            createdBy,
            Map.of()
        );
    }
    
    public String getTenantId() {
        return tenantId;
    }
}