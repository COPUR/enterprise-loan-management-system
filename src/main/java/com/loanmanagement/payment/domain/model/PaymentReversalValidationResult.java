package com.loanmanagement.payment.domain.model;

import lombok.Builder;
import lombok.Value;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * Result of payment reversal validation.
 * Contains detailed validation outcomes and recommendations.
 */
@Value
@Builder
public class PaymentReversalValidationResult {
    
    boolean valid;
    ValidationStatus status;
    
    // Payment validation
    boolean paymentExists;
    boolean paymentSettled;
    boolean paymentReversible;
    
    // Amount validation
    boolean amountValid;
    BigDecimal maxReversibleAmount;
    BigDecimal previouslyReversedAmount;
    BigDecimal remainingReversibleAmount;
    
    // Time window validation
    boolean withinReversalWindow;
    Instant reversalDeadline;
    Integer daysUntilDeadline;
    
    // Business rule validation
    boolean businessRulesPass;
    List<String> failedRules;
    List<String> warnings;
    
    // Risk assessment
    RiskLevel riskLevel;
    List<String> riskFactors;
    boolean requiresApproval;
    String approvalReason;
    
    // Compliance checks
    boolean complianceCheckPassed;
    List<String> complianceIssues;
    
    // Technical validation
    boolean technicallyFeasible;
    List<String> technicalIssues;
    
    // Recommendations
    List<String> recommendations;
    Map<String, Object> validationContext;
    
    Instant validatedAt;
    String validatedBy;
    
    /**
     * Creates a successful validation result.
     */
    public static PaymentReversalValidationResult success(
            BigDecimal maxReversibleAmount,
            RiskLevel riskLevel) {
        
        return PaymentReversalValidationResult.builder()
                .valid(true)
                .status(ValidationStatus.APPROVED)
                .paymentExists(true)
                .paymentSettled(true)
                .paymentReversible(true)
                .amountValid(true)
                .maxReversibleAmount(maxReversibleAmount)
                .remainingReversibleAmount(maxReversibleAmount)
                .withinReversalWindow(true)
                .businessRulesPass(true)
                .riskLevel(riskLevel)
                .requiresApproval(riskLevel.requiresApproval())
                .complianceCheckPassed(true)
                .technicallyFeasible(true)
                .validatedAt(Instant.now())
                .build();
    }
    
    /**
     * Creates a failed validation result.
     */
    public static PaymentReversalValidationResult failure(
            ValidationStatus status,
            List<String> failedRules,
            List<String> issues) {
        
        return PaymentReversalValidationResult.builder()
                .valid(false)
                .status(status)
                .businessRulesPass(false)
                .failedRules(failedRules)
                .technicalIssues(issues)
                .validatedAt(Instant.now())
                .build();
    }
    
    /**
     * Creates a validation result for non-existent payment.
     */
    public static PaymentReversalValidationResult paymentNotFound() {
        return PaymentReversalValidationResult.builder()
                .valid(false)
                .status(ValidationStatus.PAYMENT_NOT_FOUND)
                .paymentExists(false)
                .failedRules(List.of("Payment does not exist"))
                .validatedAt(Instant.now())
                .build();
    }
    
    /**
     * Checks if the validation passed all checks.
     */
    public boolean isFullyValid() {
        return valid && 
               status == ValidationStatus.APPROVED &&
               businessRulesPass &&
               complianceCheckPassed &&
               technicallyFeasible;
    }
    
    /**
     * Checks if validation failed due to business rules.
     */
    public boolean failedBusinessRules() {
        return !businessRulesPass && failedRules != null && !failedRules.isEmpty();
    }
    
    /**
     * Checks if manual approval is required.
     */
    public boolean requiresManualApproval() {
        return valid && requiresApproval;
    }
    
    /**
     * Gets the effective reversible amount.
     */
    public BigDecimal getEffectiveReversibleAmount() {
        if (!valid || remainingReversibleAmount == null) {
            return BigDecimal.ZERO;
        }
        return remainingReversibleAmount;
    }
    
    /**
     * Checks if partial reversal is allowed.
     */
    public boolean allowsPartialReversal() {
        return valid && 
               remainingReversibleAmount != null &&
               remainingReversibleAmount.compareTo(BigDecimal.ZERO) > 0;
    }
    
    /**
     * Gets a summary of the validation result.
     */
    public String getSummary() {
        StringBuilder summary = new StringBuilder();
        summary.append("Validation Status: ").append(status);
        
        if (valid) {
            summary.append(" - VALID");
            if (maxReversibleAmount != null) {
                summary.append(" - Max reversible: ").append(maxReversibleAmount);
            }
            if (requiresApproval) {
                summary.append(" - APPROVAL REQUIRED");
            }
        } else {
            summary.append(" - INVALID");
            if (failedRules != null && !failedRules.isEmpty()) {
                summary.append(" - Failed rules: ").append(String.join(", ", failedRules));
            }
        }
        
        if (warnings != null && !warnings.isEmpty()) {
            summary.append(" - Warnings: ").append(warnings.size());
        }
        
        return summary.toString();
    }
    
    /**
     * Gets all issues preventing reversal.
     */
    public List<String> getAllIssues() {
        List<String> allIssues = new java.util.ArrayList<>();
        
        if (failedRules != null) {
            allIssues.addAll(failedRules);
        }
        
        if (complianceIssues != null) {
            allIssues.addAll(complianceIssues);
        }
        
        if (technicalIssues != null) {
            allIssues.addAll(technicalIssues);
        }
        
        return allIssues;
    }
    
    /**
     * Validation status types.
     */
    public enum ValidationStatus {
        APPROVED,                   // Validation passed
        REJECTED,                   // Validation failed
        PENDING_APPROVAL,          // Requires manual approval
        PAYMENT_NOT_FOUND,         // Original payment not found
        PAYMENT_NOT_SETTLED,       // Payment not yet settled
        AMOUNT_EXCEEDS_LIMIT,      // Reversal amount too high
        OUTSIDE_REVERSAL_WINDOW,   // Too late to reverse
        ALREADY_REVERSED,          // Payment already reversed
        INSUFFICIENT_FUNDS,        // Not enough funds for reversal
        COMPLIANCE_VIOLATION,      // Compliance rules violated
        TECHNICAL_ERROR            // Technical issue prevents reversal
    }
    
    /**
     * Risk levels for reversals.
     */
    public enum RiskLevel {
        LOW(false),
        MEDIUM(false),
        HIGH(true),
        CRITICAL(true);
        
        private final boolean requiresApproval;
        
        RiskLevel(boolean requiresApproval) {
            this.requiresApproval = requiresApproval;
        }
        
        public boolean requiresApproval() {
            return requiresApproval;
        }
    }
}