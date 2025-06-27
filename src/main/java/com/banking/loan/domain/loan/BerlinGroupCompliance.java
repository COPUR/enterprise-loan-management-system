package com.banking.loan.domain.loan;

import java.util.Map;

public record BerlinGroupCompliance(
    String tppId,
    String consentId,
    String status,
    Map<String, Object> complianceData
) {
    public static BerlinGroupCompliance empty() {
        return new BerlinGroupCompliance("", "", "PENDING", Map.of());
    }
}