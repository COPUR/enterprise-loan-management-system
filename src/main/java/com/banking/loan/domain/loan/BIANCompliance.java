package com.banking.loan.domain.loan;

import java.util.Map;

public record BIANCompliance(
    String serviceId,
    String businessUnit,
    String status,
    Map<String, Object> bianData
) {
    public static BIANCompliance empty() {
        return new BIANCompliance("", "", "PENDING", Map.of());
    }
}