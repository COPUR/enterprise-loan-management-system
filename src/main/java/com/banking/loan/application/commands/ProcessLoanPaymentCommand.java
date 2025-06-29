package com.banking.loan.application.commands;

import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Industry-Standard Loan Payment Processing Command
 * 
 * Comprehensive command for loan payment processing following banking industry standards.
 * Includes all necessary fields for proper payment allocation, regulatory compliance,
 * and audit trail generation.
 * 
 * Architecture Guardrails Compliance:
 * ✅ Request Parsing: Type-safe command object with validation
 * ✅ Validation: Jakarta Bean Validation annotations
 * ✅ Response Types: N/A (Command object)
 * ✅ Type Safety: Strong typing with BigDecimal for financial amounts
 * ✅ Dependency Inversion: Pure command object with no dependencies
 */
public record ProcessLoanPaymentCommand(
    
    @NotBlank(message = "Loan ID is required")
    String loanId,
    
    @NotNull(message = "Payment amount is required")
    @DecimalMin(value = "0.01", message = "Payment amount must be greater than zero")
    @Digits(integer = 10, fraction = 2, message = "Payment amount must have at most 2 decimal places")
    BigDecimal amount,
    
    @NotNull(message = "Payment date is required")
    @PastOrPresent(message = "Payment date cannot be in the future")
    LocalDate paymentDate,
    
    @NotBlank(message = "Payment method is required")
    @Pattern(regexp = "^(ACH|WIRE|CREDIT_CARD|DEBIT_CARD|CHECK|CASH|ONLINE|MOBILE)$", 
             message = "Invalid payment method")
    String paymentMethod,
    
    @NotBlank(message = "Payment reference is required")
    @Size(max = 100, message = "Payment reference must not exceed 100 characters")
    String paymentReference,
    
    @Size(max = 500, message = "Payment notes must not exceed 500 characters")
    String notes,
    
    @NotBlank(message = "Customer ID is required")
    String customerId,
    
    @NotBlank(message = "Payment channel is required")
    @Pattern(regexp = "^(BRANCH|ATM|ONLINE|MOBILE|PHONE|MAIL|THIRD_PARTY)$", 
             message = "Invalid payment channel")
    String paymentChannel,
    
    @NotBlank(message = "Paid by information is required")
    @Size(max = 100, message = "Paid by must not exceed 100 characters")
    String paidBy,
    
    @NotBlank(message = "Correlation ID is required")
    String correlationId,
    
    boolean fraudCheckRequired,
    
    /**
     * Payment allocation strategy
     * - FIFO: First installment due gets paid first
     * - LIFO: Last installment due gets paid first
     * - PRO_RATA: Proportional allocation across all due installments
     * - CURRENT_DUE: Pay only current due installments
     * - CUSTOMER_DIRECTED: Customer specifies allocation
     */
    @NotBlank(message = "Allocation strategy is required")
    @Pattern(regexp = "^(FIFO|LIFO|PRO_RATA|CURRENT_DUE|CUSTOMER_DIRECTED)$", 
             message = "Invalid allocation strategy")
    String allocationStrategy,
    
    /**
     * Whether to automatically apply late fees
     * Default: true for most payments
     */
    @NotNull(message = "Apply late fees flag is required")
    Boolean applyLateFees,
    
    /**
     * Whether to waive certain fees (requires authorization)
     */
    @NotNull(message = "Waive fees flag is required")
    Boolean waiveFees,
    
    /**
     * Authorization code for fee waivers
     */
    @Size(max = 50, message = "Fee waiver authorization must not exceed 50 characters")
    String feeWaiverAuthorization,
    
    /**
     * External transaction ID from payment processor
     */
    @Size(max = 100, message = "External transaction ID must not exceed 100 characters")
    String externalTransactionId,
    
    /**
     * IP address of the payment originator (for fraud detection)
     */
    @Pattern(regexp = "^(?:[0-9]{1,3}\\.){3}[0-9]{1,3}$|^(?:[0-9a-fA-F]{1,4}:){7}[0-9a-fA-F]{1,4}$", 
             message = "Invalid IP address format")
    String originatorIpAddress,
    
    /**
     * User agent information (for fraud detection)
     */
    @Size(max = 500, message = "User agent must not exceed 500 characters")
    String userAgent,
    
    /**
     * Whether this is a recurring/automatic payment
     */
    @NotNull(message = "Is recurring flag is required")
    Boolean isRecurring,
    
    /**
     * Recurring payment authorization ID if applicable
     */
    @Size(max = 100, message = "Recurring authorization ID must not exceed 100 characters")
    String recurringAuthorizationId
    
) {
    
    /**
     * Factory method for basic loan payment command
     */
    public static ProcessLoanPaymentCommand createBasic(
            String loanId,
            BigDecimal amount,
            String paymentMethod,
            String customerId,
            String correlationId) {
        
        return new ProcessLoanPaymentCommand(
            loanId,
            amount,
            LocalDate.now(),
            paymentMethod,
            generatePaymentReference(),
            null, // notes
            customerId,
            "ONLINE", // default channel
            customerId, // paid by
            correlationId,
            true, // fraud check required
            "FIFO", // default allocation strategy
            true, // apply late fees
            false, // waive fees
            null, // fee waiver authorization
            null, // external transaction ID
            null, // originator IP
            null, // user agent
            false, // is recurring
            null // recurring authorization ID
        );
    }
    
    /**
     * Factory method for recurring payment command
     */
    public static ProcessLoanPaymentCommand createRecurring(
            String loanId,
            BigDecimal amount,
            String paymentMethod,
            String customerId,
            String recurringAuthorizationId,
            String correlationId) {
        
        return new ProcessLoanPaymentCommand(
            loanId,
            amount,
            LocalDate.now(),
            paymentMethod,
            generatePaymentReference(),
            "Recurring payment", // notes
            customerId,
            "ONLINE", // default channel
            "SYSTEM", // paid by
            correlationId,
            false, // fraud check not required for recurring
            "FIFO", // default allocation strategy
            true, // apply late fees
            false, // waive fees
            null, // fee waiver authorization
            null, // external transaction ID
            null, // originator IP
            null, // user agent
            true, // is recurring
            recurringAuthorizationId
        );
    }
    
    /**
     * Generate unique payment reference
     */
    private static String generatePaymentReference() {
        return "PAY-" + System.currentTimeMillis() + "-" + 
               java.util.UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }
    
    /**
     * Validation method for business rules
     */
    public void validateBusinessRules() {
        
        // Fee waiver requires authorization
        if (waiveFees && (feeWaiverAuthorization == null || feeWaiverAuthorization.trim().isEmpty())) {
            throw new IllegalArgumentException("Fee waiver requires authorization code");
        }
        
        // Recurring payments require authorization
        if (isRecurring && (recurringAuthorizationId == null || recurringAuthorizationId.trim().isEmpty())) {
            throw new IllegalArgumentException("Recurring payments require authorization ID");
        }
        
        // Payment method validation for channels
        validatePaymentMethodForChannel();
    }
    
    /**
     * Validate payment method is allowed for the specified channel
     */
    private void validatePaymentMethodForChannel() {
        
        switch (paymentChannel) {
            case "BRANCH":
                if (!"CASH".equals(paymentMethod) && !"CHECK".equals(paymentMethod)) {
                    throw new IllegalArgumentException("Branch channel only accepts CASH or CHECK payments");
                }
                break;
                
            case "ATM":
                if (!"DEBIT_CARD".equals(paymentMethod)) {
                    throw new IllegalArgumentException("ATM channel only accepts DEBIT_CARD payments");
                }
                break;
                
            case "ONLINE":
            case "MOBILE":
                if ("CASH".equals(paymentMethod) || "CHECK".equals(paymentMethod)) {
                    throw new IllegalArgumentException("Online/Mobile channels cannot accept CASH or CHECK payments");
                }
                break;
                
            case "MAIL":
                if (!"CHECK".equals(paymentMethod)) {
                    throw new IllegalArgumentException("Mail channel only accepts CHECK payments");
                }
                break;
        }
    }
}