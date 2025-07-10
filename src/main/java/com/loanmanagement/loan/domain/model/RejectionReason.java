package com.loanmanagement.loan.domain.model;

import lombok.Builder;
import lombok.Value;

import java.time.LocalDate;
import java.util.List;
import java.util.Objects;

/**
 * Rejection Reason Value Object
 * Contains detailed information about why a loan was rejected
 */
@Value
@Builder(toBuilder = true)
public class RejectionReason {
    
    String primaryReason;
    List<String> details;
    boolean appealable;
    LocalDate appealDeadline;
    
    // Additional rejection metadata
    String category;
    int riskScore;
    String officerNotes;
    List<String> improvementSuggestions;
    boolean eligibleForReapplication;
    LocalDate earliestReapplicationDate;
    
    public RejectionReason(String primaryReason, List<String> details, boolean appealable,
                          LocalDate appealDeadline, String category, int riskScore,
                          String officerNotes, List<String> improvementSuggestions,
                          boolean eligibleForReapplication, LocalDate earliestReapplicationDate) {
        
        // Validation
        Objects.requireNonNull(primaryReason, "Primary reason cannot be null");
        if (primaryReason.trim().isEmpty()) {
            throw new IllegalArgumentException("Primary reason cannot be empty");
        }
        Objects.requireNonNull(details, "Details list cannot be null");
        
        if (appealable && appealDeadline == null) {
            throw new IllegalArgumentException("Appeal deadline must be provided for appealable rejections");
        }
        
        if (appealDeadline != null && appealDeadline.isBefore(LocalDate.now())) {
            throw new IllegalArgumentException("Appeal deadline cannot be in the past");
        }
        
        this.primaryReason = primaryReason.trim();
        this.details = List.copyOf(details); // Immutable copy
        this.appealable = appealable;
        this.appealDeadline = appealDeadline;
        this.category = category;
        this.riskScore = riskScore;
        this.officerNotes = officerNotes;
        this.improvementSuggestions = improvementSuggestions != null ? List.copyOf(improvementSuggestions) : List.of();
        this.eligibleForReapplication = eligibleForReapplication;
        this.earliestReapplicationDate = earliestReapplicationDate;
    }
    
    /**
     * Create standard rejection reason
     */
    public static RejectionReason create(String primaryReason, List<String> details) {
        return RejectionReason.builder()
                .primaryReason(primaryReason)
                .details(details)
                .appealable(true)
                .appealDeadline(LocalDate.now().plusDays(30))
                .eligibleForReapplication(true)
                .earliestReapplicationDate(LocalDate.now().plusDays(90))
                .build();
    }
    
    /**
     * Create non-appealable rejection
     */
    public static RejectionReason nonAppealable(String primaryReason, List<String> details) {
        return RejectionReason.builder()
                .primaryReason(primaryReason)
                .details(details)
                .appealable(false)
                .eligibleForReapplication(false)
                .build();
    }
    
    /**
     * Create credit-related rejection
     */
    public static RejectionReason creditRelated(String primaryReason, List<String> details, int riskScore) {
        return RejectionReason.builder()
                .primaryReason(primaryReason)
                .details(details)
                .category("CREDIT")
                .riskScore(riskScore)
                .appealable(false)
                .eligibleForReapplication(true)
                .earliestReapplicationDate(LocalDate.now().plusMonths(6))
                .improvementSuggestions(List.of(
                    "Improve credit score",
                    "Reduce existing debt obligations",
                    "Increase income or employment stability"
                ))
                .build();
    }
    
    /**
     * Create income-related rejection
     */
    public static RejectionReason incomeRelated(String primaryReason, List<String> details) {
        return RejectionReason.builder()
                .primaryReason(primaryReason)
                .details(details)
                .category("INCOME")
                .appealable(true)
                .appealDeadline(LocalDate.now().plusDays(30))
                .eligibleForReapplication(true)
                .earliestReapplicationDate(LocalDate.now().plusDays(90))
                .improvementSuggestions(List.of(
                    "Provide additional income documentation",
                    "Include co-signer with sufficient income",
                    "Apply for a smaller loan amount"
                ))
                .build();
    }
    
    /**
     * Check if appeal period has expired
     */
    public boolean isAppealPeriodExpired() {
        return appealable && appealDeadline != null && LocalDate.now().isAfter(appealDeadline);
    }
    
    /**
     * Check if customer can reapply now
     */
    public boolean canReapplyNow() {
        return eligibleForReapplication && 
               (earliestReapplicationDate == null || !LocalDate.now().isBefore(earliestReapplicationDate));
    }
    
    /**
     * Get days remaining for appeal
     */
    public long getDaysRemainingForAppeal() {
        if (!appealable || appealDeadline == null) {
            return 0;
        }
        return Math.max(0, java.time.temporal.ChronoUnit.DAYS.between(LocalDate.now(), appealDeadline));
    }
    
    /**
     * Get days until reapplication is allowed
     */
    public long getDaysUntilReapplication() {
        if (!eligibleForReapplication || earliestReapplicationDate == null) {
            return -1;
        }
        return Math.max(0, java.time.temporal.ChronoUnit.DAYS.between(LocalDate.now(), earliestReapplicationDate));
    }
}