package com.amanahfi.payments.domain.payment;

/**
 * Payment types supported by AmanahFi platform
 * Following CBDC, Islamic banking, and cross-currency requirements
 */
public enum PaymentType {
    /**
     * Standard account-to-account transfer
     */
    TRANSFER,
    
    /**
     * CBDC transfer using UAE Digital Dirham for instant settlement
     */
    CBDC_TRANSFER,
    
    /**
     * Stablecoin transfer (USDC, USDT, etc.)
     */
    STABLECOIN_TRANSFER,
    
    /**
     * Cross-currency payment with conversion
     */
    CROSS_CURRENCY,
    
    /**
     * Islamic finance Murabaha payment
     */
    MURABAHA_PAYMENT,
    
    /**
     * Bill payment to external merchant
     */
    BILL_PAYMENT,
    
    /**
     * International wire transfer
     */
    WIRE_TRANSFER,
    
    /**
     * Direct debit payment
     */
    DIRECT_DEBIT
}