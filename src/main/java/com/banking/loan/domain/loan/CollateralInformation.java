package com.banking.loan.domain.loan;

import java.math.BigDecimal;

public record CollateralInformation(
    String type,
    String description,
    BigDecimal value,
    String status
) {
    public static CollateralInformation none() {
        return new CollateralInformation("NONE", "No collateral", BigDecimal.ZERO, "NONE");
    }
}