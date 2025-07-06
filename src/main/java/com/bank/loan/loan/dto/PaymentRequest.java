package com.bank.loan.loan.dto;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;

/**
 * Payment Request DTO
 * 
 * Request object for payment processing with comprehensive validation
 * for banking regulatory compliance and payment method details.
 */
public class PaymentRequest {
    
    @NotNull(message = "Payment amount is required")
    @DecimalMin(value = "0.01", message = "Minimum payment amount is $0.01")
    @DecimalMax(value = "1000000.00", message = "Maximum payment amount is $1,000,000.00")
    private BigDecimal amount;
    
    @NotBlank(message = "Currency is required")
    @Pattern(regexp = "USD|EUR|GBP|CAD", message = "Supported currencies: USD, EUR, GBP, CAD")
    private String currency = "USD";
    
    @NotBlank(message = "Payment type is required")
    @Pattern(regexp = "REGULAR|EARLY_PAYMENT|LATE|PARTIAL|EXTRA_PRINCIPAL", 
             message = "Valid payment types: REGULAR, EARLY_PAYMENT, LATE, PARTIAL, EXTRA_PRINCIPAL")
    private String paymentType;
    
    @NotNull(message = "Payment method is required")
    private PaymentMethod paymentMethod;
    
    @NotBlank(message = "Payment channel is required")
    @Pattern(regexp = "ONLINE|MOBILE|BRANCH|ATM|PHONE|AUTO_DEBIT", 
             message = "Valid channels: ONLINE, MOBILE, BRANCH, ATM, PHONE, AUTO_DEBIT")
    private String paymentChannel;
    
    @Size(max = 500, message = "Payment notes cannot exceed 500 characters")
    private String notes;

    // Constructors
    public PaymentRequest() {}

    public PaymentRequest(BigDecimal amount, String paymentType, PaymentMethod paymentMethod, String paymentChannel) {
        this.amount = amount;
        this.paymentType = paymentType;
        this.paymentMethod = paymentMethod;
        this.paymentChannel = paymentChannel;
    }

    // Getters and Setters
    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public String getPaymentType() {
        return paymentType;
    }

    public void setPaymentType(String paymentType) {
        this.paymentType = paymentType;
    }

    public PaymentMethod getPaymentMethod() {
        return paymentMethod;
    }

    public void setPaymentMethod(PaymentMethod paymentMethod) {
        this.paymentMethod = paymentMethod;
    }

    public String getPaymentChannel() {
        return paymentChannel;
    }

    public void setPaymentChannel(String paymentChannel) {
        this.paymentChannel = paymentChannel;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    /**
     * Payment Method Details
     */
    public static class PaymentMethod {
        
        @NotBlank(message = "Payment method type is required")
        @Pattern(regexp = "BANK_TRANSFER|CREDIT_CARD|DEBIT_CARD|ACH|WIRE_TRANSFER|CHECK", 
                 message = "Valid types: BANK_TRANSFER, CREDIT_CARD, DEBIT_CARD, ACH, WIRE_TRANSFER, CHECK")
        private String type;
        
        @Size(max = 50, message = "Account number cannot exceed 50 characters")
        private String accountNumber;
        
        @Size(max = 20, message = "Routing number cannot exceed 20 characters")
        private String routingNumber;
        
        @Size(max = 100, message = "Bank name cannot exceed 100 characters")
        private String bankName;

        // Constructors
        public PaymentMethod() {}

        public PaymentMethod(String type, String accountNumber, String routingNumber) {
            this.type = type;
            this.accountNumber = accountNumber;
            this.routingNumber = routingNumber;
        }

        // Getters and Setters
        public String getType() {
            return type;
        }

        public void setType(String type) {
            this.type = type;
        }

        public String getAccountNumber() {
            return accountNumber;
        }

        public void setAccountNumber(String accountNumber) {
            this.accountNumber = accountNumber;
        }

        public String getRoutingNumber() {
            return routingNumber;
        }

        public void setRoutingNumber(String routingNumber) {
            this.routingNumber = routingNumber;
        }

        public String getBankName() {
            return bankName;
        }

        public void setBankName(String bankName) {
            this.bankName = bankName;
        }

        @Override
        public String toString() {
            return "PaymentMethod{" +
                    "type='" + type + '\'' +
                    ", accountNumber='" + (accountNumber != null ? "****" + accountNumber.substring(Math.max(0, accountNumber.length() - 4)) : null) + '\'' +
                    ", bankName='" + bankName + '\'' +
                    '}';
        }
    }

    @Override
    public String toString() {
        return "PaymentRequest{" +
                "amount=" + amount +
                ", currency='" + currency + '\'' +
                ", paymentType='" + paymentType + '\'' +
                ", paymentMethod=" + paymentMethod +
                ", paymentChannel='" + paymentChannel + '\'' +
                '}';
    }
}