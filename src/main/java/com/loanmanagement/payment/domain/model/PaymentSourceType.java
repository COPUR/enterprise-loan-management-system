package com.loanmanagement.payment.domain.model;

/**
 * Enum representing different types of payment sources.
 */
public enum PaymentSourceType {
    CHECKING_ACCOUNT("Checking Account"),
    SAVINGS_ACCOUNT("Savings Account"),
    MONEY_MARKET_ACCOUNT("Money Market Account"),
    CERTIFICATE_OF_DEPOSIT("Certificate of Deposit"),
    CREDIT_CARD("Credit Card"),
    DEBIT_CARD("Debit Card"),
    PREPAID_CARD("Prepaid Card"),
    DIGITAL_WALLET("Digital Wallet"),
    EXTERNAL_BANK_ACCOUNT("External Bank Account"),
    INTERNAL_TRANSFER("Internal Transfer"),
    ESCROW_ACCOUNT("Escrow Account"),
    TRUST_ACCOUNT("Trust Account"),
    BUSINESS_ACCOUNT("Business Account"),
    JOINT_ACCOUNT("Joint Account"),
    STUDENT_ACCOUNT("Student Account"),
    RETIREMENT_ACCOUNT("Retirement Account"),
    INVESTMENT_ACCOUNT("Investment Account"),
    CRYPTOCURRENCY_WALLET("Cryptocurrency Wallet"),
    CASH("Cash"),
    CHECK("Check");

    private final String displayName;

    PaymentSourceType(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    public boolean isAccountBased() {
        return this != CASH && this != CHECK && this != CREDIT_CARD && this != DEBIT_CARD;
    }

    public boolean requiresVerification() {
        return this == EXTERNAL_BANK_ACCOUNT || this == CRYPTOCURRENCY_WALLET || this == DIGITAL_WALLET;
    }
}