package com.loanmanagement.payment.domain.service;

import com.loanmanagement.payment.domain.model.*;
import com.loanmanagement.shared.domain.Money;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Domain Service for Payment Validation
 * Handles payment validation logic and business rules
 */
@Slf4j
@Service
public class PaymentValidationService {
    
    private static final Money MAX_PAYMENT_AMOUNT = Money.of("USD", new BigDecimal("1000000.00"));
    private static final Money MIN_PAYMENT_AMOUNT = Money.of("USD", new BigDecimal("1.00"));
    private final Set<String> processedIdempotencyKeys = new HashSet<>();

    /**
     * Validate payment request
     */
    public PaymentValidationResult validatePayment(PaymentRequest paymentRequest) {
        log.debug("Validating payment request for loan: {}", paymentRequest.getLoanId());
        
        List<PaymentViolation> violations = new ArrayList<>();
        List<String> warnings = new ArrayList<>();
        
        // Validate payment amount
        validatePaymentAmount(paymentRequest, violations);
        
        // Validate payment source
        validatePaymentSource(paymentRequest, violations);
        
        // Validate payment timing
        validatePaymentTiming(paymentRequest, violations, warnings);
        
        // Check for duplicate payments
        validateDuplicatePayment(paymentRequest, violations);
        
        // Validate loan-specific rules
        validateLoanSpecificRules(paymentRequest, violations);
        
        // Validate currency consistency
        validateCurrencyConsistency(paymentRequest, violations);
        
        boolean isValid = violations.isEmpty();
        
        return PaymentValidationResult.builder()
                .valid(isValid)
                .violations(violations)
                .warnings(warnings)
                .validatedDate(LocalDateTime.now())
                .build();
    }
    
    /**
     * Validate payment timing restrictions
     */
    public PaymentValidationResult validatePaymentTiming(PaymentRequest paymentRequest) {
        List<PaymentViolation> violations = new ArrayList<>();
        List<String> warnings = new ArrayList<>();
        
        validatePaymentTiming(paymentRequest, violations, warnings);
        
        return PaymentValidationResult.builder()
                .valid(violations.isEmpty())
                .violations(violations)
                .warnings(warnings)
                .validatedDate(LocalDateTime.now())
                .build();
    }
    
    /**
     * Create a failed validation result
     */
    public static PaymentValidationResult failed(PaymentFailureReason failureReason, String message) {
        PaymentViolation violation = PaymentViolation.builder()
                .violationType(mapFailureReasonToViolationType(failureReason))
                .message(message)
                .severity(PaymentViolationSeverity.ERROR)
                .field("general")
                .build();
        
        return PaymentValidationResult.builder()
                .valid(false)
                .violations(List.of(violation))
                .warnings(List.of())
                .validatedDate(LocalDateTime.now())
                .build();
    }
    
    // Private validation methods
    
    private void validatePaymentAmount(PaymentRequest paymentRequest, List<PaymentViolation> violations) {
        Money paymentAmount = paymentRequest.getPaymentAmount();
        
        if (paymentAmount == null) {
            violations.add(PaymentViolation.builder()
                    .violationType(PaymentViolationType.INVALID_AMOUNT)
                    .message("Payment amount cannot be null")
                    .severity(PaymentViolationSeverity.ERROR)
                    .field("paymentAmount")
                    .build());
            return;
        }
        
        if (paymentAmount.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
            violations.add(PaymentViolation.builder()
                    .violationType(PaymentViolationType.INVALID_AMOUNT)
                    .message("Payment amount must be positive")
                    .severity(PaymentViolationSeverity.ERROR)
                    .field("paymentAmount")
                    .build());
        }
        
        if (paymentAmount.getAmount().compareTo(MIN_PAYMENT_AMOUNT.getAmount()) < 0) {
            violations.add(PaymentViolation.builder()
                    .violationType(PaymentViolationType.AMOUNT_TOO_SMALL)
                    .message("Payment amount must be at least " + MIN_PAYMENT_AMOUNT)
                    .severity(PaymentViolationSeverity.ERROR)
                    .field("paymentAmount")
                    .build());
        }
        
        if (paymentAmount.getAmount().compareTo(MAX_PAYMENT_AMOUNT.getAmount()) > 0) {
            violations.add(PaymentViolation.builder()
                    .violationType(PaymentViolationType.AMOUNT_EXCEEDS_LIMIT)
                    .message("Payment amount cannot exceed " + MAX_PAYMENT_AMOUNT)
                    .severity(PaymentViolationSeverity.ERROR)
                    .field("paymentAmount")
                    .build());
        }
    }
    
    private void validatePaymentSource(PaymentRequest paymentRequest, List<PaymentViolation> violations) {
        PaymentSource paymentSource = paymentRequest.getPaymentSource();
        
        if (paymentSource == null) {
            violations.add(PaymentViolation.builder()
                    .violationType(PaymentViolationType.INVALID_PAYMENT_SOURCE)
                    .message("Payment source cannot be null")
                    .severity(PaymentViolationSeverity.ERROR)
                    .field("paymentSource")
                    .build());
            return;
        }
        
        // Validate account number
        if (paymentSource.getAccountNumber() == null || paymentSource.getAccountNumber().trim().isEmpty()) {
            violations.add(PaymentViolation.builder()
                    .violationType(PaymentViolationType.INVALID_PAYMENT_SOURCE)
                    .message("Account number cannot be empty")
                    .severity(PaymentViolationSeverity.ERROR)
                    .field("paymentSource.accountNumber")
                    .build());
        }
        
        // Validate routing number for bank accounts
        if (paymentSource.getSourceType() == PaymentSourceType.BANK_ACCOUNT) {
            validateRoutingNumber(paymentSource, violations);
        }
        
        // Validate credit card information
        if (paymentSource.getSourceType() == PaymentSourceType.CREDIT_CARD) {
            validateCreditCardInformation(paymentSource, violations);
        }
    }
    
    private void validateRoutingNumber(PaymentSource paymentSource, List<PaymentViolation> violations) {
        String routingNumber = paymentSource.getRoutingNumber();
        
        if (routingNumber == null || routingNumber.trim().isEmpty()) {
            violations.add(PaymentViolation.builder()
                    .violationType(PaymentViolationType.INVALID_PAYMENT_SOURCE)
                    .message("Routing number is required for bank account payments")
                    .severity(PaymentViolationSeverity.ERROR)
                    .field("paymentSource.routingNumber")
                    .build());
            return;
        }
        
        // Basic routing number validation (9 digits)
        if (!routingNumber.matches("\\d{9}")) {
            violations.add(PaymentViolation.builder()
                    .violationType(PaymentViolationType.INVALID_PAYMENT_SOURCE)
                    .message("Routing number must be 9 digits")
                    .severity(PaymentViolationSeverity.ERROR)
                    .field("paymentSource.routingNumber")
                    .build());
        }
        
        // Check for obviously invalid routing numbers
        if (routingNumber.equals("000000000") || routingNumber.equals("123456789")) {
            violations.add(PaymentViolation.builder()
                    .violationType(PaymentViolationType.INVALID_PAYMENT_SOURCE)
                    .message("Invalid routing number")
                    .severity(PaymentViolationSeverity.ERROR)
                    .field("paymentSource.routingNumber")
                    .build());
        }
    }
    
    private void validateCreditCardInformation(PaymentSource paymentSource, List<PaymentViolation> violations) {
        String cardNumber = paymentSource.getCardNumber();
        
        if (cardNumber == null || cardNumber.trim().isEmpty()) {
            violations.add(PaymentViolation.builder()
                    .violationType(PaymentViolationType.INVALID_PAYMENT_SOURCE)
                    .message("Card number is required for credit card payments")
                    .severity(PaymentViolationSeverity.ERROR)
                    .field("paymentSource.cardNumber")
                    .build());
            return;
        }
        
        // Basic card number validation (remove spaces and check length)
        String cleanCardNumber = cardNumber.replaceAll("\\s+", "");
        if (!cleanCardNumber.matches("\\d{13,19}")) {
            violations.add(PaymentViolation.builder()
                    .violationType(PaymentViolationType.INVALID_PAYMENT_SOURCE)
                    .message("Invalid card number format")
                    .severity(PaymentViolationSeverity.ERROR)
                    .field("paymentSource.cardNumber")
                    .build());
        }
        
        // Validate expiry information
        if (paymentSource.getExpiryMonth() == null || paymentSource.getExpiryYear() == null) {
            violations.add(PaymentViolation.builder()
                    .violationType(PaymentViolationType.INVALID_PAYMENT_SOURCE)
                    .message("Card expiry information is required")
                    .severity(PaymentViolationSeverity.ERROR)
                    .field("paymentSource.expiry")
                    .build());
        }
        
        // Validate CVV
        if (paymentSource.getCvv() == null || !paymentSource.getCvv().matches("\\d{3,4}")) {
            violations.add(PaymentViolation.builder()
                    .violationType(PaymentViolationType.INVALID_PAYMENT_SOURCE)
                    .message("Invalid CVV")
                    .severity(PaymentViolationSeverity.ERROR)
                    .field("paymentSource.cvv")
                    .build());
        }
    }
    
    private void validatePaymentTiming(PaymentRequest paymentRequest, 
                                     List<PaymentViolation> violations, 
                                     List<String> warnings) {
        LocalDateTime paymentDate = paymentRequest.getPaymentDate();
        
        if (paymentDate == null) {
            violations.add(PaymentViolation.builder()
                    .violationType(PaymentViolationType.INVALID_DATE)
                    .message("Payment date cannot be null")
                    .severity(PaymentViolationSeverity.ERROR)
                    .field("paymentDate")
                    .build());
            return;
        }
        
        // Check if payment date is in the future (too far)
        LocalDateTime maxFutureDate = LocalDateTime.now().plusDays(30);
        if (paymentDate.isAfter(maxFutureDate)) {
            violations.add(PaymentViolation.builder()
                    .violationType(PaymentViolationType.INVALID_DATE)
                    .message("Payment date cannot be more than 30 days in the future")
                    .severity(PaymentViolationSeverity.ERROR)
                    .field("paymentDate")
                    .build());
        }
        
        // Check if payment date is too far in the past
        LocalDateTime minPastDate = LocalDateTime.now().minusDays(90);
        if (paymentDate.isBefore(minPastDate)) {
            violations.add(PaymentViolation.builder()
                    .violationType(PaymentViolationType.INVALID_DATE)
                    .message("Payment date cannot be more than 90 days in the past")
                    .severity(PaymentViolationSeverity.ERROR)
                    .field("paymentDate")
                    .build());
        }
        
        // Warn about weekend payments
        DayOfWeek dayOfWeek = paymentDate.getDayOfWeek();
        if (dayOfWeek == DayOfWeek.SATURDAY || dayOfWeek == DayOfWeek.SUNDAY) {
            warnings.add("Payment scheduled for weekend - processing may be delayed");
        }
        
        // Warn about late hour payments
        int hour = paymentDate.getHour();
        if (hour < 6 || hour > 22) {
            warnings.add("Payment scheduled outside business hours - processing may be delayed");
        }
    }
    
    private void validateDuplicatePayment(PaymentRequest paymentRequest, List<PaymentViolation> violations) {
        String idempotencyKey = paymentRequest.getIdempotencyKey();
        
        if (idempotencyKey == null || idempotencyKey.trim().isEmpty()) {
            // No idempotency key provided - this is okay for most payments
            return;
        }
        
        if (processedIdempotencyKeys.contains(idempotencyKey)) {
            violations.add(PaymentViolation.builder()
                    .violationType(PaymentViolationType.DUPLICATE_PAYMENT)
                    .message("Duplicate payment detected - this payment has already been processed")
                    .severity(PaymentViolationSeverity.ERROR)
                    .field("idempotencyKey")
                    .build());
        } else {
            // Add to processed keys
            processedIdempotencyKeys.add(idempotencyKey);
        }
    }
    
    private void validateLoanSpecificRules(PaymentRequest paymentRequest, List<PaymentViolation> violations) {
        // Validate payment type compatibility
        if (paymentRequest.getPaymentType() == PaymentType.PARTIAL_PAYMENT && 
            !Boolean.TRUE.equals(paymentRequest.getAllowPartialPayment())) {
            violations.add(PaymentViolation.builder()
                    .violationType(PaymentViolationType.PAYMENT_TYPE_NOT_ALLOWED)
                    .message("Partial payments are not allowed for this loan")
                    .severity(PaymentViolationSeverity.ERROR)
                    .field("paymentType")
                    .build());
        }
        
        // Validate recurring payment setup
        if (paymentRequest.getPaymentType() == PaymentType.RECURRING_SETUP) {
            if (paymentRequest.getRecurringSchedule() == null) {
                violations.add(PaymentViolation.builder()
                        .violationType(PaymentViolationType.MISSING_RECURRING_INFO)
                        .message("Recurring schedule information is required for recurring payment setup")
                        .severity(PaymentViolationSeverity.ERROR)
                        .field("recurringSchedule")
                        .build());
            }
        }
    }
    
    private void validateCurrencyConsistency(PaymentRequest paymentRequest, List<PaymentViolation> violations) {
        String paymentCurrency = paymentRequest.getPaymentAmount().getCurrency();
        
        // In a real implementation, we would check against the loan's currency
        // For now, we'll just validate that it's a supported currency
        if (!isSupportedCurrency(paymentCurrency)) {
            violations.add(PaymentViolation.builder()
                    .violationType(PaymentViolationType.UNSUPPORTED_CURRENCY)
                    .message("Currency " + paymentCurrency + " is not supported")
                    .severity(PaymentViolationSeverity.ERROR)
                    .field("paymentAmount.currency")
                    .build());
        }
        
        // Validate currency conversion if applicable
        if (paymentRequest.getCurrencyConversion() != null) {
            validateCurrencyConversion(paymentRequest, violations);
        }
    }
    
    private void validateCurrencyConversion(PaymentRequest paymentRequest, List<PaymentViolation> violations) {
        CurrencyConversionRate conversionRate = paymentRequest.getCurrencyConversion();
        
        if (conversionRate.getRate().compareTo(BigDecimal.ZERO) <= 0) {
            violations.add(PaymentViolation.builder()
                    .violationType(PaymentViolationType.INVALID_CONVERSION_RATE)
                    .message("Currency conversion rate must be positive")
                    .severity(PaymentViolationSeverity.ERROR)
                    .field("currencyConversion.rate")
                    .build());
        }
        
        // Check if conversion rate is reasonable (not too extreme)
        if (conversionRate.getRate().compareTo(new BigDecimal("100")) > 0 || 
            conversionRate.getRate().compareTo(new BigDecimal("0.01")) < 0) {
            violations.add(PaymentViolation.builder()
                    .violationType(PaymentViolationType.INVALID_CONVERSION_RATE)
                    .message("Currency conversion rate appears unrealistic")
                    .severity(PaymentViolationSeverity.WARNING)
                    .field("currencyConversion.rate")
                    .build());
        }
    }
    
    private boolean isSupportedCurrency(String currency) {
        // List of supported currencies
        Set<String> supportedCurrencies = Set.of("USD", "EUR", "GBP", "CAD", "AUD", "JPY", "CHF");
        return supportedCurrencies.contains(currency);
    }
    
    private static PaymentViolationType mapFailureReasonToViolationType(PaymentFailureReason failureReason) {
        return switch (failureReason) {
            case INSUFFICIENT_FUNDS -> PaymentViolationType.INSUFFICIENT_FUNDS;
            case INVALID_PAYMENT_METHOD -> PaymentViolationType.INVALID_PAYMENT_SOURCE;
            case AMOUNT_LIMIT_EXCEEDED -> PaymentViolationType.AMOUNT_EXCEEDS_LIMIT;
            case DUPLICATE_TRANSACTION -> PaymentViolationType.DUPLICATE_PAYMENT;
            default -> PaymentViolationType.GENERAL_VALIDATION_ERROR;
        };
    }
}