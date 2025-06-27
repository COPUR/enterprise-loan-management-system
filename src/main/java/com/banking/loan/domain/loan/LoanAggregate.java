package com.banking.loan.domain.loan;

import com.banking.loan.domain.shared.AggregateRoot;
import com.banking.loan.domain.shared.DomainEvent;
import com.banking.loan.domain.exceptions.LoanDomainException;
import com.banking.loan.domain.shared.EventMetadata;
import com.banking.loan.domain.events.*;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * Loan Aggregate Root - Core domain entity for loan management
 * Implements DDD patterns with event sourcing and business rules
 */
@Getter
public class LoanAggregate extends AggregateRoot<LoanId> {
    
    private CustomerId customerId;
    private LoanAmount amount;
    private LoanTerm term;
    private InterestRate interestRate;
    private LoanStatus status;
    private LoanType type;
    private CollateralInformation collateral;
    private CreditAssessment creditAssessment;
    private List<LoanInstallment> installments;
    private PaymentSchedule paymentSchedule;
    private LoanMetadata metadata;
    
    // AI and compliance related fields
    private AIRiskAssessment aiRiskAssessment;
    private ComplianceCheck complianceCheck;
    private FraudAssessment fraudAssessment;
    
    // Berlin Group / BIAN compliance
    private BerlinGroupCompliance berlinGroupData;
    private BIANCompliance bianData;
    
    protected LoanAggregate() {
        super();
    }
    
    /**
     * Create new loan application
     */
    public static LoanAggregate createApplication(
            LoanId loanId,
            CustomerId customerId,
            LoanAmount amount,
            LoanTerm term,
            LoanType type,
            String applicantId,
            String correlationId,
            String tenantId) {
        
        LoanAggregate loan = new LoanAggregate();
        loan.id = loanId;
        loan.customerId = customerId;
        loan.amount = amount;
        loan.term = term;
        loan.type = type;
        loan.status = LoanStatus.PENDING_ASSESSMENT;
        loan.createdBy = applicantId;
        loan.updatedBy = applicantId;
        
        // Add domain event
        loan.addDomainEvent(new LoanApplicationSubmittedEvent(
            loanId.value(),
            loan.version,
            applicantId,
            correlationId,
            tenantId,
            EventMetadata.of("loan-service", "1.0"),
            customerId.value(),
            amount.value(),
            term.months(),
            type.name()
        ));
        
        loan.incrementVersion();
        return loan;
    }
    
    /**
     * Perform AI-powered risk assessment
     */
    public void performAIRiskAssessment(AIRiskAssessment assessment, String assessedBy, String correlationId) {
        validateLoanForAssessment();
        
        this.aiRiskAssessment = assessment;
        
        addDomainEvent(new AIRiskAssessmentCompletedEvent(
            id.value(),
            version,
            assessedBy,
            correlationId,
            getTenantId(),
            EventMetadata.of("ai-service", "1.0"),
            assessment.getRiskScore(),
            assessment.getConfidenceLevel(),
            assessment.getRecommendation()
        ));
        
        updateModifiedInfo(assessedBy);
    }
    
    /**
     * Approve loan after all assessments
     */
    public void approve(InterestRate approvedRate, PaymentSchedule schedule, String approvedBy, String correlationId) {
        validateLoanForApproval();
        
        this.interestRate = approvedRate;
        this.paymentSchedule = schedule;
        this.status = LoanStatus.APPROVED;
        
        addDomainEvent(new LoanApprovedEvent(
            id.value(),
            version,
            approvedBy,
            correlationId,
            getTenantId(),
            EventMetadata.of("loan-service", "1.0"),
            customerId.value(),
            amount.value(),
            approvedRate.value(),
            schedule.getFirstPaymentDate()
        ));
        
        updateModifiedInfo(approvedBy);
    }
    
    /**
     * Reject loan application
     */
    public void reject(List<RejectionReason> reasons, String rejectedBy, String correlationId) {
        validateLoanForRejection();
        
        this.status = LoanStatus.REJECTED;
        
        addDomainEvent(new LoanRejectedEvent(
            id.value(),
            version,
            rejectedBy,
            correlationId,
            getTenantId(),
            EventMetadata.of("loan-service", "1.0"),
            customerId.value(),
            reasons.stream().map(RejectionReason::description).toList()
        ));
        
        updateModifiedInfo(rejectedBy);
    }
    
    /**
     * Process loan payment
     */
    public PaymentResult processPayment(PaymentAmount paymentAmount, PaymentMethod method, String paidBy, String correlationId) {
        validateLoanForPayment();
        
        // Find next installment to pay
        LoanInstallment nextInstallment = findNextUnpaidInstallment();
        if (nextInstallment == null) {
            throw new LoanDomainException("No outstanding installments found");
        }
        
        // Process payment at aggregate level (DDD principle: aggregate controls its invariants)
        PaymentResult result = processInstallmentPayment(nextInstallment, paymentAmount, method);
        
        addDomainEvent(new PaymentProcessedEvent(
            id.value(),
            version,
            paidBy,
            correlationId,
            getTenantId(),
            EventMetadata.of("payment-service", "1.0"),
            customerId.value(),
            paymentAmount.value(),
            nextInstallment.paymentNumber(),
            result.status()
        ));
        
        // Check if loan is fully paid
        if (areAllInstallmentsPaid()) {
            this.status = LoanStatus.FULLY_PAID;
            addDomainEvent(new LoanFullyPaidEvent(
                id.value(),
                version,
                paidBy,
                correlationId,
                getTenantId(),
                EventMetadata.of("loan-service", "1.0"),
                customerId.value(),
                Instant.now()
            ));
        }
        
        updateModifiedInfo(paidBy);
        return result;
    }
    
    /**
     * Flag loan for fraud investigation
     */
    public void flagForFraud(FraudIndicators indicators, String flaggedBy, String correlationId) {
        this.status = LoanStatus.UNDER_INVESTIGATION;
        this.fraudAssessment = FraudAssessment.fromIndicators(indicators);
        
        addDomainEvent(new FraudDetectedEvent(
            id.value(),
            version,
            flaggedBy,
            correlationId,
            getTenantId(),
            EventMetadata.of("fraud-service", "1.0"),
            customerId.value(),
            indicators.getRiskLevel().name(),
            indicators.getIndicators()
        ));
        
        updateModifiedInfo(flaggedBy);
    }
    
    /**
     * Update Berlin Group compliance data
     */
    public void updateBerlinGroupCompliance(BerlinGroupCompliance compliance, String updatedBy) {
        this.berlinGroupData = compliance;
        updateModifiedInfo(updatedBy);
    }
    
    /**
     * Update BIAN compliance data
     */
    public void updateBIANCompliance(BIANCompliance compliance, String updatedBy) {
        this.bianData = compliance;
        updateModifiedInfo(updatedBy);
    }
    
    // Private validation methods
    private void validateLoanForAssessment() {
        if (status != LoanStatus.PENDING_ASSESSMENT) {
            throw new LoanDomainException("Loan must be in PENDING_ASSESSMENT status for AI assessment");
        }
    }
    
    private void validateLoanForApproval() {
        if (status != LoanStatus.PENDING_ASSESSMENT && status != LoanStatus.UNDER_REVIEW) {
            throw new LoanDomainException("Loan must be assessed before approval");
        }
        if (aiRiskAssessment == null) {
            throw new LoanDomainException("AI risk assessment must be completed before approval");
        }
    }
    
    private void validateLoanForRejection() {
        if (status == LoanStatus.APPROVED || status == LoanStatus.ACTIVE || status == LoanStatus.FULLY_PAID) {
            throw new LoanDomainException("Cannot reject an already processed loan");
        }
    }
    
    private void validateLoanForPayment() {
        if (status != LoanStatus.ACTIVE) {
            throw new LoanDomainException("Loan must be active to process payments");
        }
    }
    
    private LoanInstallment findNextUnpaidInstallment() {
        if (installments != null && !installments.isEmpty()) {
            return installments.stream()
                .filter(installment -> !installment.isPaid())
                .findFirst()
                .orElse(null);
        }
        
        // Fallback to payment schedule if installments not yet created
        if (paymentSchedule != null && !paymentSchedule.installments().isEmpty()) {
            return paymentSchedule.installments().get(0);
        }
        
        return null;
    }
    
    private boolean areAllInstallmentsPaid() {
        if (installments != null && !installments.isEmpty()) {
            return installments.stream().allMatch(LoanInstallment::isPaid);
        }
        
        // Simplified logic for compilation
        return false;
    }
    
    private String getTenantId() {
        return metadata != null ? metadata.getTenantId() : "default";
    }
    
    /**
     * Process installment payment with business rules enforcement
     */
    private PaymentResult processInstallmentPayment(LoanInstallment installment, PaymentAmount paymentAmount, PaymentMethod method) {
        // Simplified payment processing logic following DDD principles
        // In real implementation, would include complex payment allocation logic
        
        boolean isFullPayment = paymentAmount.value().compareTo(installment.getTotalPayment()) >= 0;
        String paymentId = java.util.UUID.randomUUID().toString();
        
        if (isFullPayment) {
            return PaymentResult.successful(paymentId, "TXN-" + paymentId.substring(0, 8), paymentAmount.value());
        } else {
            return PaymentResult.failed(paymentId, "Insufficient payment amount");
        }
    }
}

