package com.loanmanagement.loan.domain.model;

import lombok.Builder;
import lombok.Data;
import lombok.With;

import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Modified installment plan representing changes to an original plan.
 */
@Data
@Builder
@With
public class ModifiedInstallmentPlan {
    
    @NotNull
    private final InstallmentPlan originalPlan;
    
    @NotNull
    private final InstallmentPlan modifiedPlan;
    
    @NotNull
    private final String modificationId;
    
    @NotNull
    private final InstallmentModificationType modificationType;
    
    @NotNull
    private final LocalDate modificationDate;
    
    @NotNull
    private final LocalDate effectiveDate;
    
    @NotNull
    private final String modificationReason;
    
    @NotNull
    private final List<InstallmentModification> modifications;
    
    private final String approvedBy;
    
    private final LocalDate approvalDate;
    
    private final boolean isTemporary;
    
    private final LocalDate temporaryEndDate;
    
    private final BigDecimal modificationFee;
    
    private final String legalDocumentReference;
    
    private final String customerAgreementReference;
    
    private final boolean requiresCustomerConsent;
    
    private final boolean customerConsentObtained;
    
    private final LocalDate customerConsentDate;
    
    /**
     * Calculates the impact of modifications on total payment amount.
     */
    public BigDecimal calculatePaymentImpact() {
        BigDecimal originalTotal = originalPlan.getTotalPaymentAmount();
        BigDecimal modifiedTotal = modifiedPlan.getTotalPaymentAmount();
        
        return modifiedTotal.subtract(originalTotal);
    }
    
    /**
     * Calculates the impact of modifications on total interest amount.
     */
    public BigDecimal calculateInterestImpact() {
        BigDecimal originalInterest = originalPlan.getTotalInterestAmount();
        BigDecimal modifiedInterest = modifiedPlan.getTotalInterestAmount();
        
        return modifiedInterest.subtract(originalInterest);
    }
    
    /**
     * Calculates the change in monthly payment amount.
     */
    public BigDecimal calculateMonthlyPaymentChange() {
        BigDecimal originalMonthly = originalPlan.calculateMonthlyPayment();
        BigDecimal modifiedMonthly = modifiedPlan.calculateMonthlyPayment();
        
        return modifiedMonthly.subtract(originalMonthly);
    }
    
    /**
     * Calculates the change in loan term.
     */
    public Integer calculateTermChange() {
        return modifiedPlan.getTermInMonths() - originalPlan.getTermInMonths();
    }
    
    /**
     * Checks if modification is currently effective.
     */
    public boolean isCurrentlyEffective() {
        LocalDate currentDate = LocalDate.now();
        
        if (currentDate.isBefore(effectiveDate)) {
            return false;
        }
        
        if (isTemporary && temporaryEndDate != null) {
            return !currentDate.isAfter(temporaryEndDate);
        }
        
        return true;
    }
    
    /**
     * Validates the modification plan.
     */
    public boolean isValid() {
        if (originalPlan == null || modifiedPlan == null) {
            return false;
        }
        
        if (requiresCustomerConsent && !customerConsentObtained) {
            return false;
        }
        
        if (isTemporary && temporaryEndDate == null) {
            return false;
        }
        
        return effectiveDate != null && 
               !effectiveDate.isBefore(modificationDate) &&
               modifications != null && 
               !modifications.isEmpty();
    }
    
    /**
     * Gets the modification summary for reporting.
     */
    public String getModificationSummary() {
        StringBuilder summary = new StringBuilder();
        summary.append("Modification Type: ").append(modificationType).append("\n");
        summary.append("Payment Impact: ").append(calculatePaymentImpact()).append("\n");
        summary.append("Interest Impact: ").append(calculateInterestImpact()).append("\n");
        summary.append("Monthly Payment Change: ").append(calculateMonthlyPaymentChange()).append("\n");
        summary.append("Term Change: ").append(calculateTermChange()).append(" months\n");
        
        if (isTemporary) {
            summary.append("Temporary modification until: ").append(temporaryEndDate).append("\n");
        }
        
        return summary.toString();
    }
}