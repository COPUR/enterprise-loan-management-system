package com.amanahfi.platform.cbdc.domain;

/**
 * Status of Digital Dirham transactions
 */
public enum TransactionStatus {
    PENDING,        // Transaction initiated but not confirmed
    CONFIRMED,      // Transaction confirmed on Corda network
    COMPLETED,      // Transaction completed successfully
    FAILED,         // Transaction failed
    CANCELLED,      // Transaction cancelled
    REVERTED       // Transaction reverted
}