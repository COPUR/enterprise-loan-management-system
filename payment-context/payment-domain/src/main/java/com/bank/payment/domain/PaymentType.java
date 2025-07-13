package com.bank.payment.domain;

import com.bank.shared.kernel.domain.Money;

import java.math.BigDecimal;

/**
 * Enumeration representing different types of payments
 */
public enum PaymentType {
    TRANSFER("Transfer", BigDecimal.ZERO),
    WIRE_TRANSFER("Wire Transfer", BigDecimal.valueOf(25.00)),
    ACH("ACH Transfer", BigDecimal.valueOf(1.50)),
    CHECK("Check Payment", BigDecimal.valueOf(5.00)),
    CREDIT_CARD("Credit Card Payment", BigDecimal.valueOf(3.50)),
    DEBIT_CARD("Debit Card Payment", BigDecimal.valueOf(1.00)),
    MOBILE_PAYMENT("Mobile Payment", BigDecimal.valueOf(0.50)),
    LOAN_PAYMENT("Loan Payment", BigDecimal.ZERO),
    BILL_PAYMENT("Bill Payment", BigDecimal.valueOf(2.00));
    
    private final String displayName;
    private final BigDecimal baseFee;
    
    PaymentType(String displayName, BigDecimal baseFee) {
        this.displayName = displayName;
        this.baseFee = baseFee;
    }
    
    public String getDisplayName() {
        return displayName;
    }
    
    public BigDecimal getBaseFee() {
        return baseFee;
    }
    
    public Money calculateFee(Money amount) {
        // Simple fee calculation - can be enhanced with percentage-based fees
        return Money.of(baseFee, amount.getCurrency());
    }
    
    public boolean requiresEnhancedValidation() {
        return this == WIRE_TRANSFER || this == CHECK;
    }
    
    public boolean isElectronic() {
        return this != CHECK;
    }
}