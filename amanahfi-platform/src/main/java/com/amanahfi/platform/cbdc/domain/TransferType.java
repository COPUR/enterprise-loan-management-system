package com.amanahfi.platform.cbdc.domain;

/**
 * Types of Digital Dirham transfers
 */
public enum TransferType {
    SEND,                   // Standard outgoing transfer
    RECEIVE,                // Incoming transfer
    ISLAMIC_FINANCE,        // Islamic finance related transfer
    CROSS_BORDER,          // Cross-border payment
    GOVERNMENT_PAYMENT,     // Government payment
    SALARY_PAYMENT,        // Salary payment
    MERCHANT_PAYMENT,      // Merchant payment
    UTILITY_PAYMENT,       // Utility bill payment
    REMITTANCE,            // Remittance transfer
    ESCROW_RELEASE,        // Escrow release
    REGULATORY_FREEZE,     // Regulatory freeze
    MINT,                  // Central Bank minting
    BURN                   // Central Bank burning
}