package com.loanmanagement.loan.domain.model;

import lombok.Builder;
import lombok.Value;

import java.util.Objects;

/**
 * Restructuring Reason Value Object
 * Contains information about why a loan is being restructured
 */
@Value
@Builder(toBuilder = true)
public class RestructuringReason {
    
    String reason;
    String justification;
    boolean temporaryHardship;
    Integer expectedDuration; // Duration in months if temporary
    
    // Additional restructuring details
    RestructuringType restructuringType;
    String customerRequest;
    String regulatoryReason;
    boolean preventiveAction;
    String businessJustification;
    
    public RestructuringReason(String reason, String justification, boolean temporaryHardship,
                              Integer expectedDuration, RestructuringType restructuringType,
                              String customerRequest, String regulatoryReason, boolean preventiveAction,
                              String businessJustification) {
        
        // Validation
        Objects.requireNonNull(reason, "Restructuring reason cannot be null");
        if (reason.trim().isEmpty()) {
            throw new IllegalArgumentException("Restructuring reason cannot be empty");
        }
        
        if (temporaryHardship && (expectedDuration == null || expectedDuration <= 0)) {
            throw new IllegalArgumentException("Expected duration must be positive for temporary hardship");
        }
        
        if (expectedDuration != null && expectedDuration > 120) {
            throw new IllegalArgumentException("Expected duration cannot exceed 120 months");
        }
        
        this.reason = reason.trim();
        this.justification = justification;
        this.temporaryHardship = temporaryHardship;
        this.expectedDuration = expectedDuration;
        this.restructuringType = restructuringType != null ? restructuringType : RestructuringType.PAYMENT_MODIFICATION;
        this.customerRequest = customerRequest;
        this.regulatoryReason = regulatoryReason;
        this.preventiveAction = preventiveAction;
        this.businessJustification = businessJustification;
    }
    
    /**
     * Create financial hardship restructuring reason
     */
    public static RestructuringReason financialHardship(String hardshipDetails, int expectedDurationMonths) {
        return RestructuringReason.builder()
                .reason("Financial hardship")
                .justification(hardshipDetails)
                .temporaryHardship(true)
                .expectedDuration(expectedDurationMonths)
                .restructuringType(RestructuringType.HARDSHIP_MODIFICATION)
                .preventiveAction(false)
                .build();
    }
    
    /**
     * Create rate modification restructuring reason
     */
    public static RestructuringReason rateModification(String marketJustification) {
        return RestructuringReason.builder()
                .reason("Interest rate modification")
                .justification(marketJustification)
                .temporaryHardship(false)
                .restructuringType(RestructuringType.RATE_MODIFICATION)
                .preventiveAction(false)
                .businessJustification(marketJustification)
                .build();
    }
    
    /**
     * Create term extension restructuring reason
     */
    public static RestructuringReason termExtension(String justification, boolean isPreventive) {
        return RestructuringReason.builder()
                .reason("Loan term extension")
                .justification(justification)
                .temporaryHardship(false)
                .restructuringType(RestructuringType.TERM_EXTENSION)
                .preventiveAction(isPreventive)
                .businessJustification(justification)
                .build();
    }
    
    /**
     * Create payment deferral restructuring reason
     */
    public static RestructuringReason paymentDeferral(String hardshipReason, int deferralMonths) {
        return RestructuringReason.builder()
                .reason("Payment deferral")
                .justification(hardshipReason)
                .temporaryHardship(true)
                .expectedDuration(deferralMonths)
                .restructuringType(RestructuringType.PAYMENT_DEFERRAL)
                .preventiveAction(false)
                .build();
    }
    
    /**
     * Create regulatory compliance restructuring reason
     */
    public static RestructuringReason regulatoryCompliance(String regulatoryRequirement) {
        return RestructuringReason.builder()
                .reason("Regulatory compliance")
                .justification("Required by regulatory guidelines")
                .temporaryHardship(false)
                .restructuringType(RestructuringType.REGULATORY_MODIFICATION)
                .regulatoryReason(regulatoryRequirement)
                .preventiveAction(true)
                .build();
    }
    
    /**
     * Check if restructuring is customer-initiated
     */
    public boolean isCustomerInitiated() {
        return customerRequest != null && !customerRequest.trim().isEmpty();
    }
    
    /**
     * Check if restructuring is required by regulation
     */
    public boolean isRegulatoryRequired() {
        return restructuringType == RestructuringType.REGULATORY_MODIFICATION ||
               (regulatoryReason != null && !regulatoryReason.trim().isEmpty());
    }
    
    /**
     * Check if restructuring is a business decision
     */
    public boolean isBusinessDecision() {
        return !isCustomerInitiated() && !isRegulatoryRequired() && preventiveAction;
    }
    
    /**
     * Get restructuring priority (1=highest, 5=lowest)
     */
    public int getPriority() {
        if (isRegulatoryRequired()) return 1;
        if (temporaryHardship) return 2;
        if (preventiveAction) return 3;
        if (isCustomerInitiated()) return 4;
        return 5;
    }
    
    /**
     * Get expected resolution timeline
     */
    public String getExpectedResolution() {
        if (temporaryHardship && expectedDuration != null) {
            return String.format("Expected to resolve in %d months", expectedDuration);
        } else if (temporaryHardship) {
            return "Temporary modification, duration to be determined";
        } else {
            return "Permanent modification";
        }
    }
}