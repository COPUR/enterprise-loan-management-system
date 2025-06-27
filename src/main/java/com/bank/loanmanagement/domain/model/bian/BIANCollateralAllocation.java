package com.bank.loanmanagement.domain.model.bian;

import lombok.Data;
import lombok.Builder;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
public class BIANCollateralAllocation {
    private String allocationId;
    private String collateralType;
    private String collateralDescription;
    private BigDecimal collateralValue;
    private BigDecimal allocatedAmount;
    private String valuationMethod;
    private LocalDateTime valuationDate;
    private String status;
}