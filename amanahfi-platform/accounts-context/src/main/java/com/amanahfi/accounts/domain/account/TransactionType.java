package com.amanahfi.accounts.domain.account;

/**
 * Transaction types for account operations
 */
public enum TransactionType {
    DEPOSIT,
    WITHDRAWAL,
    TRANSFER_IN,
    TRANSFER_OUT,
    PROFIT_SHARING,
    FEE_DEDUCTION,
    REFUND,
    CBDC_SETTLEMENT,
    STABLECOIN_CONVERSION
}