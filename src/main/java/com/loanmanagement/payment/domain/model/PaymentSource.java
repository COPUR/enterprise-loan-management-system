package com.loanmanagement.payment.domain.model;

import lombok.Builder;
import lombok.Value;
import lombok.With;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * Value object representing payment source information.
 */
@Value
@Builder
@With
public class PaymentSource {
    
    String sourceId;
    PaymentSourceType sourceType;
    String accountNumber;
    String routingNumber;
    String bankName;
    String cardNumber;
    String cardHolderName;
    String expiryMonth;
    String expiryYear;
    String cvv;
    String digitalWalletProvider;
    String digitalWalletId;
    String currencyCode;
    boolean isVerified;
    boolean isActive;
    LocalDateTime lastUsed;
    LocalDateTime createdAt;
    LocalDateTime updatedAt;
    Map<String, String> metadata;

    public static class PaymentSourceBuilder {
        public PaymentSourceBuilder accountNumber(String accountNumber) {
            if (accountNumber != null && accountNumber.trim().isEmpty()) {
                throw new IllegalArgumentException("Account number cannot be empty");
            }
            this.accountNumber = accountNumber;
            return this;
        }

        public PaymentSourceBuilder routingNumber(String routingNumber) {
            if (routingNumber != null && routingNumber.trim().isEmpty()) {
                throw new IllegalArgumentException("Routing number cannot be empty");
            }
            this.routingNumber = routingNumber;
            return this;
        }

        public PaymentSourceBuilder cardNumber(String cardNumber) {
            if (cardNumber != null && cardNumber.trim().isEmpty()) {
                throw new IllegalArgumentException("Card number cannot be empty");
            }
            this.cardNumber = cardNumber;
            return this;
        }

        public PaymentSource build() {
            if (sourceId == null || sourceId.trim().isEmpty()) {
                throw new IllegalArgumentException("Source ID is required");
            }
            if (sourceType == null) {
                throw new IllegalArgumentException("Source type is required");
            }
            if (currencyCode == null || currencyCode.trim().isEmpty()) {
                this.currencyCode = "USD";
            }
            if (createdAt == null) {
                this.createdAt = LocalDateTime.now();
            }
            if (updatedAt == null) {
                this.updatedAt = LocalDateTime.now();
            }
            
            return new PaymentSource(
                sourceId, sourceType, accountNumber, routingNumber, bankName,
                cardNumber, cardHolderName, expiryMonth, expiryYear, cvv,
                digitalWalletProvider, digitalWalletId, currencyCode,
                isVerified, isActive, lastUsed, createdAt, updatedAt, metadata
            );
        }
    }

    public boolean isCardBased() {
        return sourceType == PaymentSourceType.CREDIT_CARD || sourceType == PaymentSourceType.DEBIT_CARD;
    }

    public boolean isAccountBased() {
        return sourceType.isAccountBased();
    }

    public boolean requiresVerification() {
        return sourceType.requiresVerification();
    }

    public String getMaskedAccountNumber() {
        if (accountNumber == null || accountNumber.length() < 4) {
            return accountNumber;
        }
        return "*".repeat(accountNumber.length() - 4) + accountNumber.substring(accountNumber.length() - 4);
    }

    public String getMaskedCardNumber() {
        if (cardNumber == null || cardNumber.length() < 4) {
            return cardNumber;
        }
        return "*".repeat(cardNumber.length() - 4) + cardNumber.substring(cardNumber.length() - 4);
    }
}