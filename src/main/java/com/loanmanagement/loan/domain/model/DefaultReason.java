package com.loanmanagement.loan.domain.model;

import com.loanmanagement.shared.domain.Money;
import lombok.Builder;
import lombok.Value;

import java.time.LocalDate;
import java.util.List;
import java.util.Objects;

/**
 * Default Reason Value Object
 * Contains detailed information about why a loan is in default
 */
@Value
@Builder(toBuilder = true)
public class DefaultReason {
    
    String reason;
    int daysPastDue;
    Money totalAmountPastDue;
    int missedPayments;
    LocalDate lastPaymentDate;
    List<String> collectionActions;
    
    // Additional default information
    DefaultType defaultType;
    LocalDate firstMissedPaymentDate;
    Money currentMonthlyPayment;
    String customerResponse;
    boolean hardshipDeclared;
    String hardshipReason;
    
    public DefaultReason(String reason, int daysPastDue, Money totalAmountPastDue, int missedPayments,
                        LocalDate lastPaymentDate, List<String> collectionActions, DefaultType defaultType,
                        LocalDate firstMissedPaymentDate, Money currentMonthlyPayment, String customerResponse,
                        boolean hardshipDeclared, String hardshipReason) {
        
        // Validation
        Objects.requireNonNull(reason, "Default reason cannot be null");
        if (reason.trim().isEmpty()) {
            throw new IllegalArgumentException("Default reason cannot be empty");
        }
        
        if (daysPastDue < 0) {
            throw new IllegalArgumentException("Days past due cannot be negative");
        }
        
        if (missedPayments < 0) {
            throw new IllegalArgumentException("Missed payments cannot be negative");
        }
        
        Objects.requireNonNull(totalAmountPastDue, "Total amount past due cannot be null");
        if (totalAmountPastDue.getAmount().compareTo(java.math.BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Total amount past due cannot be negative");
        }
        
        this.reason = reason.trim();
        this.daysPastDue = daysPastDue;
        this.totalAmountPastDue = totalAmountPastDue;
        this.missedPayments = missedPayments;
        this.lastPaymentDate = lastPaymentDate;
        this.collectionActions = collectionActions != null ? List.copyOf(collectionActions) : List.of();
        this.defaultType = defaultType != null ? defaultType : DefaultType.PAYMENT_DEFAULT;
        this.firstMissedPaymentDate = firstMissedPaymentDate;
        this.currentMonthlyPayment = currentMonthlyPayment;
        this.customerResponse = customerResponse;
        this.hardshipDeclared = hardshipDeclared;
        this.hardshipReason = hardshipReason;
    }
    
    /**
     * Create payment default reason
     */
    public static DefaultReason paymentDefault(int daysPastDue, Money totalAmountPastDue, int missedPayments) {
        String reason = String.format("Missed %d consecutive payments, %d days past due", 
                                    missedPayments, daysPastDue);
        
        return DefaultReason.builder()
                .reason(reason)
                .daysPastDue(daysPastDue)
                .totalAmountPastDue(totalAmountPastDue)
                .missedPayments(missedPayments)
                .defaultType(DefaultType.PAYMENT_DEFAULT)
                .build();
    }
    
    /**
     * Create covenant default reason
     */
    public static DefaultReason covenantDefault(String covenantViolation) {
        return DefaultReason.builder()
                .reason("Covenant violation: " + covenantViolation)
                .daysPastDue(0)
                .totalAmountPastDue(Money.of("USD", java.math.BigDecimal.ZERO))
                .missedPayments(0)
                .defaultType(DefaultType.COVENANT_DEFAULT)
                .build();
    }
    
    /**
     * Create financial hardship default
     */
    public static DefaultReason hardshipDefault(int daysPastDue, Money totalAmountPastDue, 
                                              int missedPayments, String hardshipReason) {
        String reason = String.format("Financial hardship: %s", hardshipReason);
        
        return DefaultReason.builder()
                .reason(reason)
                .daysPastDue(daysPastDue)
                .totalAmountPastDue(totalAmountPastDue)
                .missedPayments(missedPayments)
                .defaultType(DefaultType.HARDSHIP_DEFAULT)
                .hardshipDeclared(true)
                .hardshipReason(hardshipReason)
                .build();
    }
    
    /**
     * Check if default is due to payment issues
     */
    public boolean isPaymentRelated() {
        return defaultType == DefaultType.PAYMENT_DEFAULT || 
               defaultType == DefaultType.HARDSHIP_DEFAULT;
    }
    
    /**
     * Check if default is severe (90+ days or bankruptcy)
     */
    public boolean isSevere() {
        return daysPastDue >= 90 || 
               defaultType == DefaultType.BANKRUPTCY_DEFAULT ||
               defaultType == DefaultType.FRAUD_DEFAULT;
    }
    
    /**
     * Check if customer has responded to collection efforts
     */
    public boolean hasCustomerResponse() {
        return customerResponse != null && !customerResponse.trim().isEmpty();
    }
    
    /**
     * Calculate default severity score (0-100)
     */
    public int getDefaultSeverityScore() {
        int score = 0;
        
        // Days past due (0-40 points)
        score += Math.min(40, daysPastDue / 3);
        
        // Missed payments (0-30 points)
        score += Math.min(30, missedPayments * 10);
        
        // Default type (0-20 points)
        score += switch (defaultType) {
            case PAYMENT_DEFAULT -> 10;
            case COVENANT_DEFAULT -> 15;
            case HARDSHIP_DEFAULT -> 5;
            case BANKRUPTCY_DEFAULT -> 20;
            case FRAUD_DEFAULT -> 20;
        };
        
        // Collection response (0-10 points)
        if (!hasCustomerResponse()) {
            score += 10;
        }
        
        return Math.min(100, score);
    }
    
    /**
     * Get recommended collection action based on severity
     */
    public String getRecommendedAction() {
        int severity = getDefaultSeverityScore();
        
        if (severity <= 20) {
            return "Send payment reminder notice";
        } else if (severity <= 40) {
            return "Initiate phone contact";
        } else if (severity <= 60) {
            return "Send demand letter";
        } else if (severity <= 80) {
            return "Refer to collections agency";
        } else {
            return "Consider legal action";
        }
    }
}