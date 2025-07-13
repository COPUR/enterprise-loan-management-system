package com.amanahfi.payments.domain.payment;

/**
 * Payment fee types for different charges
 */
public enum PaymentFeeType {
    PROCESSING_FEE,
    NETWORK_FEE,
    CURRENCY_CONVERSION_FEE,
    EXPEDITED_PROCESSING_FEE,
    INTERNATIONAL_WIRE_FEE,
    COMPLIANCE_FEE,
    
    // Islamic banking compliant fees
    SERVICE_CHARGE, // Halal alternative to interest
    ADMINISTRATIVE_FEE,
    
    // Non-Islamic fees (not allowed for Islamic accounts)
    INTEREST_CHARGE,
    LATE_PAYMENT_INTEREST
}