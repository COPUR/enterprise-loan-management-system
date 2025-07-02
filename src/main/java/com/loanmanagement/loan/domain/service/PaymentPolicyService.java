package com.loanmanagement.loan.domain.service;

import java.math.BigDecimal;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * Domain service for payment policy configuration.
 * Externalizes business rules per 12-Factor App principles.
 */
@Service
public class PaymentPolicyService {

    @Value("${loan.payment.early-discount-rate:0.001}")
    private BigDecimal earlyPaymentDiscountRate;

    @Value("${loan.payment.late-penalty-rate:0.001}")
    private BigDecimal latePaymentPenaltyRate;

    public BigDecimal getEarlyPaymentDiscountRate() {
        return earlyPaymentDiscountRate;
    }

    public BigDecimal getLatePaymentPenaltyRate() {
        return latePaymentPenaltyRate;
    }
}
