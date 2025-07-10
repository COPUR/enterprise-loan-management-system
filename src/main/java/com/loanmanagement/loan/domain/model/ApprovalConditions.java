package com.loanmanagement.loan.domain.model;

import com.loanmanagement.shared.domain.Money;
import lombok.Builder;
import lombok.Value;

import java.time.LocalDate;
import java.util.List;
import java.util.Objects;

/**
 * Approval Conditions Value Object
 * Contains the conditions under which a loan is approved
 */
@Value
@Builder(toBuilder = true)
public class ApprovalConditions {
    
    Money approvedAmount;
    LoanTerms approvedTerms;
    List<String> conditions;
    LocalDate expirationDate;
    
    // Additional approval metadata
    String approvalNote;
    List<String> requiredDocuments;
    Money maximumApprovedAmount;
    boolean requiresCollateral;
    String collateralRequirements;
    
    public ApprovalConditions(Money approvedAmount, LoanTerms approvedTerms, List<String> conditions,
                             LocalDate expirationDate, String approvalNote, List<String> requiredDocuments,
                             Money maximumApprovedAmount, boolean requiresCollateral, String collateralRequirements) {
        
        // Validation
        Objects.requireNonNull(approvedAmount, "Approved amount cannot be null");
        Objects.requireNonNull(approvedTerms, "Approved terms cannot be null");
        Objects.requireNonNull(conditions, "Conditions list cannot be null");
        Objects.requireNonNull(expirationDate, "Expiration date cannot be null");
        
        if (expirationDate.isBefore(LocalDate.now())) {
            throw new IllegalArgumentException("Expiration date cannot be in the past");
        }
        
        this.approvedAmount = approvedAmount;
        this.approvedTerms = approvedTerms;
        this.conditions = List.copyOf(conditions); // Immutable copy
        this.expirationDate = expirationDate;
        this.approvalNote = approvalNote;
        this.requiredDocuments = requiredDocuments != null ? List.copyOf(requiredDocuments) : List.of();
        this.maximumApprovedAmount = maximumApprovedAmount != null ? maximumApprovedAmount : approvedAmount;
        this.requiresCollateral = requiresCollateral;
        this.collateralRequirements = collateralRequirements;
    }
    
    /**
     * Create standard approval conditions
     */
    public static ApprovalConditions standard(Money approvedAmount, LoanTerms approvedTerms) {
        return ApprovalConditions.builder()
                .approvedAmount(approvedAmount)
                .approvedTerms(approvedTerms)
                .conditions(List.of())
                .expirationDate(LocalDate.now().plusDays(30))
                .requiresCollateral(false)
                .build();
    }
    
    /**
     * Create conditional approval
     */
    public static ApprovalConditions conditional(Money approvedAmount, LoanTerms approvedTerms, 
                                                List<String> conditions) {
        return ApprovalConditions.builder()
                .approvedAmount(approvedAmount)
                .approvedTerms(approvedTerms)
                .conditions(conditions)
                .expirationDate(LocalDate.now().plusDays(30))
                .requiresCollateral(false)
                .build();
    }
    
    /**
     * Check if approval has expired
     */
    public boolean isExpired() {
        return LocalDate.now().isAfter(expirationDate);
    }
    
    /**
     * Check if approval is unconditional
     */
    public boolean isUnconditional() {
        return conditions.isEmpty();
    }
    
    /**
     * Check if approval requires additional documentation
     */
    public boolean requiresDocumentation() {
        return requiredDocuments != null && !requiredDocuments.isEmpty();
    }
    
    /**
     * Get days until expiration
     */
    public long getDaysUntilExpiration() {
        return java.time.temporal.ChronoUnit.DAYS.between(LocalDate.now(), expirationDate);
    }
}