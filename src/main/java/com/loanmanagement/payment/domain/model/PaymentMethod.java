package com.loanmanagement.payment.domain.model;

/**
 * Enum representing different payment methods.
 */
public enum PaymentMethod {
    BANK_TRANSFER("Bank Transfer"),
    CREDIT_CARD("Credit Card"),
    DEBIT_CARD("Debit Card"),
    ACH("ACH Transfer"),
    WIRE_TRANSFER("Wire Transfer"),
    CHECK("Check"),
    CASH("Cash"),
    DIGITAL_WALLET("Digital Wallet"),
    CRYPTOCURRENCY("Cryptocurrency"),
    DIRECT_DEBIT("Direct Debit"),
    SEPA("SEPA Transfer"),
    SWIFT("SWIFT Transfer"),
    MOBILE_PAYMENT("Mobile Payment"),
    ONLINE_BANKING("Online Banking"),
    PAYPAL("PayPal"),
    APPLE_PAY("Apple Pay"),
    GOOGLE_PAY("Google Pay"),
    VENMO("Venmo"),
    ZELLE("Zelle"),
    MONEY_ORDER("Money Order");

    private final String displayName;

    PaymentMethod(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    public boolean isElectronic() {
        return this != CHECK && this != CASH && this != MONEY_ORDER;
    }

    public boolean requiresValidation() {
        return this == CREDIT_CARD || this == DEBIT_CARD || this == BANK_TRANSFER || this == ACH;
    }
}