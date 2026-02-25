package com.loanmanagement.loan.domain.model;

import com.loanmanagement.loan.domain.event.*;
import com.loanmanagement.shared.domain.AggregateRoot;
import com.loanmanagement.shared.domain.Money;
import com.loanmanagement.payment.domain.model.PaymentRecord;
import com.loanmanagement.payment.domain.model.PaymentAllocation;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Loan Aggregate Root
 * Core domain entity representing a loan with its complete lifecycle
 */
@Getter
@FieldDefaults(level = AccessLevel.PRIVATE)
public class Loan extends AggregateRoot<LoanId> {

    // Core loan information (immutable)
    final CustomerId customerId;
    final Money principalAmount;
    final LoanPurpose purpose;
    final LoanTerms originalTerms;
    final LoanOfficerId assignedOfficerId;
    
    // Current state (mutable)
    LoanStatus status;
    Money currentBalance;
    LoanTerms currentTerms;
    
    // Approval information
    ApprovalConditions approvalConditions;
    LocalDateTime approvalDate;
    LoanOfficerId approvingOfficerId;
    
    // Rejection information
    RejectionReason rejectionReason;
    LocalDateTime rejectionDate;
    LoanOfficerId rejectingOfficerId;
    
    // Disbursement information
    DisbursementInstructions disbursementInstructions;
    LocalDateTime disbursementDate;
    
    // Payment information
    List<PaymentRecord> paymentHistory;
    LocalDateTime lastPaymentDate;
    Money totalPaid;
    
    // Default information
    DefaultReason defaultReason;
    LocalDateTime defaultDate;
    
    // Restructuring information
    RestructuringReason restructuringReason;
    LocalDateTime restructureDate;
    
    // Payoff information
    LocalDateTime paidOffDate;
    
    // Creation timestamps
    LocalDateTime createdAt;
    LocalDateTime lastModifiedAt;

    private Loan(LoanId id, CustomerId customerId, Money principalAmount, 
                LoanPurpose purpose, LoanTerms terms, LoanOfficerId officerId) {
        super(id);
        this.customerId = Objects.requireNonNull(customerId, "Customer ID cannot be null");
        this.principalAmount = Objects.requireNonNull(principalAmount, "Principal amount cannot be null");
        this.purpose = Objects.requireNonNull(purpose, "Loan purpose cannot be null");
        this.originalTerms = Objects.requireNonNull(terms, "Loan terms cannot be null");
        this.currentTerms = terms;
        this.assignedOfficerId = Objects.requireNonNull(officerId, "Loan officer ID cannot be null");
        
        // Validate business rules
        validateLoanCreation(principalAmount, terms);
        
        // Initialize state
        this.status = LoanStatus.PENDING;
        this.currentBalance = Money.of(principalAmount.getCurrency(), BigDecimal.ZERO);
        this.paymentHistory = new ArrayList<>();
        this.totalPaid = Money.of(principalAmount.getCurrency(), BigDecimal.ZERO);
        this.createdAt = LocalDateTime.now();
        this.lastModifiedAt = LocalDateTime.now();
    }

    /**
     * Factory method to create a new loan application
     */
    public static Loan createApplication(LoanId loanId, CustomerId customerId, 
                                       Money principalAmount, LoanPurpose purpose,
                                       LoanTerms terms, LoanOfficerId officerId) {
        Loan loan = new Loan(loanId, customerId, principalAmount, purpose, terms, officerId);
        
        // Emit domain event
        loan.registerEvent(LoanApplicationSubmittedEvent.builder()
                .eventId(generateEventId())
                .occurredOn(LocalDateTime.now())
                .loanId(loanId)
                .customerId(customerId)
                .requestedAmount(principalAmount)
                .loanPurpose(purpose)
                .requestedTerms(terms)
                .loanOfficerId(officerId)
                .applicationDate(loan.createdAt)
                .build());
        
        return loan;
    }

    /**
     * Approve the loan application
     */
    public void approve(ApprovalConditions conditions, LoanOfficerId approvingOfficer) {
        validateStateTransition(LoanStatus.PENDING, "approve");
        Objects.requireNonNull(conditions, "Approval conditions cannot be null");
        Objects.requireNonNull(approvingOfficer, "Approving officer cannot be null");
        
        this.status = LoanStatus.APPROVED;
        this.approvalConditions = conditions;
        this.approvalDate = LocalDateTime.now();
        this.approvingOfficerId = approvingOfficer;
        this.lastModifiedAt = LocalDateTime.now();
        
        // Update terms if modified during approval
        if (!conditions.getApprovedTerms().equals(originalTerms)) {
            this.currentTerms = conditions.getApprovedTerms();
        }
        
        // Emit domain event
        registerEvent(LoanApprovedEvent.builder()
                .eventId(generateEventId())
                .occurredOn(LocalDateTime.now())
                .loanId(getId())
                .customerId(customerId)
                .approvedAmount(conditions.getApprovedAmount())
                .approvedTerms(conditions.getApprovedTerms())
                .conditions(conditions.getConditions())
                .approvalDate(approvalDate)
                .approvingOfficerId(approvingOfficer)
                .approvalExpirationDate(conditions.getExpirationDate())
                .termsModified(!conditions.getApprovedTerms().equals(originalTerms))
                .build());
    }

    /**
     * Reject the loan application
     */
    public void reject(RejectionReason reason, LoanOfficerId rejectingOfficer) {
        validateStateTransition(LoanStatus.PENDING, "reject");
        Objects.requireNonNull(reason, "Rejection reason cannot be null");
        Objects.requireNonNull(rejectingOfficer, "Rejecting officer cannot be null");
        
        this.status = LoanStatus.REJECTED;
        this.rejectionReason = reason;
        this.rejectionDate = LocalDateTime.now();
        this.rejectingOfficerId = rejectingOfficer;
        this.lastModifiedAt = LocalDateTime.now();
        
        // Emit domain event
        registerEvent(LoanRejectedEvent.builder()
                .eventId(generateEventId())
                .occurredOn(LocalDateTime.now())
                .loanId(getId())
                .customerId(customerId)
                .primaryReason(reason.getPrimaryReason())
                .rejectionDetails(reason.getDetails())
                .appealable(reason.isAppealable())
                .appealDeadline(reason.getAppealDeadline())
                .rejectionDate(rejectionDate)
                .rejectingOfficerId(rejectingOfficer)
                .build());
    }

    /**
     * Disburse the approved loan
     */
    public void disburse(DisbursementInstructions instructions, LoanOfficerId disbursedBy) {
        validateStateTransition(LoanStatus.APPROVED, "disburse");
        Objects.requireNonNull(instructions, "Disbursement instructions cannot be null");
        Objects.requireNonNull(disbursedBy, "Disbursing officer cannot be null");
        
        validateDisbursementInstructions(instructions);
        
        this.status = LoanStatus.ACTIVE;
        this.disbursementInstructions = instructions;
        this.disbursementDate = LocalDateTime.now();
        this.currentBalance = approvalConditions.getApprovedAmount();
        this.lastModifiedAt = LocalDateTime.now();
        
        // Emit domain event
        registerEvent(LoanDisbursedEvent.builder()
                .eventId(generateEventId())
                .occurredOn(LocalDateTime.now())
                .loanId(getId())
                .customerId(customerId)
                .disbursedAmount(approvalConditions.getApprovedAmount())
                .disbursementMethod(instructions.getDisbursementMethod())
                .accountNumber(instructions.getAccountNumber())
                .routingNumber(instructions.getRoutingNumber())
                .disbursementDate(disbursementDate)
                .disbursedBy(disbursedBy)
                .specialInstructions(instructions.getSpecialInstructions())
                .build());
    }

    /**
     * Make a payment on the loan
     */
    public void makePayment(Money paymentAmount, PaymentAllocation allocation, 
                           LocalDateTime paymentDate) {
        validateStateTransition(LoanStatus.ACTIVE, "make payment");
        Objects.requireNonNull(paymentAmount, "Payment amount cannot be null");
        Objects.requireNonNull(allocation, "Payment allocation cannot be null");
        Objects.requireNonNull(paymentDate, "Payment date cannot be null");
        
        validatePaymentAmount(paymentAmount, allocation);
        
        // Update balances
        Money principalPayment = Money.of(currentBalance.getCurrency(), allocation.getPrincipalAmount());
        Money newBalance = currentBalance.subtract(principalPayment);
        if (newBalance.getAmount().compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Payment cannot exceed remaining balance");
        }
        
        this.currentBalance = newBalance;
        this.totalPaid = totalPaid.add(paymentAmount);
        this.lastPaymentDate = paymentDate;
        this.lastModifiedAt = LocalDateTime.now();
        
        // Record payment in history
        PaymentRecord paymentRecord = PaymentRecord.builder()
                .paymentId(com.loanmanagement.payment.domain.model.PaymentId.generate())
                .paymentAmount(paymentAmount)
                .allocation(allocation)
                .paymentDate(paymentDate)
                .remainingBalance(currentBalance)
                .build();
        
        paymentHistory.add(paymentRecord);
        
        // Emit payment event
        registerEvent(LoanPaymentMadeEvent.builder()
                .eventId(generateEventId())
                .occurredOn(LocalDateTime.now())
                .loanId(getId())
                .customerId(customerId)
                .paymentAmount(paymentAmount)
                .principalAmount(Money.of(paymentAmount.getCurrency(), allocation.getPrincipalAmount()))
                .interestAmount(Money.of(paymentAmount.getCurrency(), allocation.getInterestAmount()))
                .feesAmount(Money.of(paymentAmount.getCurrency(), allocation.getFeesAmount()))
                .paymentDate(paymentDate)
                .remainingBalance(currentBalance)
                .build());
        
        // Check if loan is paid off
        if (currentBalance.getAmount().compareTo(BigDecimal.ZERO) == 0) {
            markAsPaidOff(paymentAmount);
        }
    }

    /**
     * Mark the loan as defaulted
     */
    public void markAsDefaulted(DefaultReason reason, LoanOfficerId officerId) {
        if (status != LoanStatus.ACTIVE && status != LoanStatus.RESTRUCTURED) {
            throw new IllegalStateException(
                    "Cannot mark loan as defaulted. Current status: " + status);
        }
        
        Objects.requireNonNull(reason, "Default reason cannot be null");
        Objects.requireNonNull(officerId, "Officer ID cannot be null");
        
        this.status = LoanStatus.DEFAULTED;
        this.defaultReason = reason;
        this.defaultDate = LocalDateTime.now();
        this.lastModifiedAt = LocalDateTime.now();
        
        // Emit domain event
        registerEvent(LoanDefaultedEvent.builder()
                .eventId(generateEventId())
                .occurredOn(LocalDateTime.now())
                .loanId(getId())
                .customerId(customerId)
                .defaultReason(reason.getReason())
                .daysPastDue(reason.getDaysPastDue())
                .totalAmountPastDue(reason.getTotalAmountPastDue())
                .missedPayments(reason.getMissedPayments())
                .lastPaymentDate(reason.getLastPaymentDate())
                .collectionActions(reason.getCollectionActions())
                .defaultDate(defaultDate)
                .officerId(officerId)
                .build());
    }

    /**
     * Restructure the loan terms
     */
    public void restructure(LoanTerms newTerms, RestructuringReason reason, 
                           LoanOfficerId officerId) {
        if (status != LoanStatus.ACTIVE) {
            throw new IllegalStateException(
                    "Cannot restructure loan. Current status: " + status);
        }
        
        Objects.requireNonNull(newTerms, "New terms cannot be null");
        Objects.requireNonNull(reason, "Restructuring reason cannot be null");
        Objects.requireNonNull(officerId, "Officer ID cannot be null");
        
        LoanTerms previousTerms = this.currentTerms;
        this.status = LoanStatus.RESTRUCTURED;
        this.currentTerms = newTerms;
        this.restructuringReason = reason;
        this.restructureDate = LocalDateTime.now();
        this.lastModifiedAt = LocalDateTime.now();
        
        // Emit domain event
        registerEvent(LoanRestructuredEvent.builder()
                .eventId(generateEventId())
                .occurredOn(LocalDateTime.now())
                .loanId(getId())
                .customerId(customerId)
                .originalTerms(previousTerms)
                .newTerms(newTerms)
                .restructuringReason(reason.getReason())
                .justification(reason.getJustification())
                .temporaryHardship(reason.isTemporaryHardship())
                .expectedDuration(reason.getExpectedDuration())
                .restructureDate(restructureDate)
                .officerId(officerId)
                .termsComparison(calculateTermsComparison(previousTerms, newTerms))
                .build());
    }

    /**
     * Mark the loan as paid off
     */
    private void markAsPaidOff(Money finalPayment) {
        this.status = LoanStatus.PAID_OFF;
        this.paidOffDate = LocalDateTime.now();
        this.lastModifiedAt = LocalDateTime.now();
        
        // Emit domain event
        registerEvent(LoanPaidOffEvent.builder()
                .eventId(generateEventId())
                .occurredOn(LocalDateTime.now())
                .loanId(getId())
                .customerId(customerId)
                .finalPaymentAmount(finalPayment)
                .totalAmountPaid(totalPaid)
                .paidOffDate(paidOffDate)
                .originalAmount(principalAmount)
                .totalInterestPaid(calculateTotalInterestPaid())
                .build());
    }

    // Validation methods
    
    private void validateLoanCreation(Money amount, LoanTerms terms) {
        if (amount.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Loan amount must be greater than zero");
        }
        
        if (terms.getTermInMonths() <= 0) {
            throw new IllegalArgumentException("Loan term must be greater than zero");
        }
        
        if (terms.getInterestRate().compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Interest rate cannot be negative");
        }
    }
    
    private void validateStateTransition(LoanStatus expectedStatus, String operation) {
        if (status != expectedStatus) {
            throw new IllegalStateException(
                    String.format("Cannot %s loan. Expected status: %s, Current status: %s",
                            operation, expectedStatus, status));
        }
    }
    
    private void validateDisbursementInstructions(DisbursementInstructions instructions) {
        if (instructions.getAccountNumber() == null || instructions.getAccountNumber().trim().isEmpty()) {
            throw new IllegalArgumentException("Account number is required for disbursement");
        }
        
        if (instructions.getRoutingNumber() == null || instructions.getRoutingNumber().trim().isEmpty()) {
            throw new IllegalArgumentException("Routing number is required for disbursement");
        }
        
        if (instructions.getDisbursementDate().isBefore(LocalDate.now())) {
            throw new IllegalArgumentException("Disbursement date cannot be in the past");
        }
    }
    
    private void validatePaymentAmount(Money paymentAmount, PaymentAllocation allocation) {
        BigDecimal totalAllocation = allocation.getPrincipalAmount()
                .add(allocation.getInterestAmount())
                .add(allocation.getFeesAmount());
        
        if (paymentAmount.getAmount().compareTo(totalAllocation) != 0) {
            throw new IllegalArgumentException(
                    "Payment amount must equal total allocation amount");
        }
    }
    
    // Calculation methods
    
    public Money calculateTotalInterestPaid() {
        return paymentHistory.stream()
                .map(payment -> Money.of(principalAmount.getCurrency(), payment.getAllocation().getInterestAmount()))
                .reduce(Money.of(principalAmount.getCurrency(), BigDecimal.ZERO), Money::add);
    }
    
    private java.util.Map<String, Object> calculateTermsComparison(LoanTerms oldTerms, LoanTerms newTerms) {
        java.util.Map<String, Object> comparison = new java.util.HashMap<>();
        
        comparison.put("termExtension", newTerms.getTermInMonths() - oldTerms.getTermInMonths());
        comparison.put("rateChange", newTerms.getInterestRate().subtract(oldTerms.getInterestRate()));
        comparison.put("frequencyChanged", !newTerms.getPaymentFrequency().equals(oldTerms.getPaymentFrequency()));
        
        return comparison;
    }
    
    // Utility methods
    
    public boolean isActive() {
        return status == LoanStatus.ACTIVE || status == LoanStatus.RESTRUCTURED;
    }
    
    public boolean isPaidOff() {
        return status == LoanStatus.PAID_OFF;
    }
    
    public boolean isInDefault() {
        return status == LoanStatus.DEFAULTED;
    }
    
    public List<PaymentRecord> getPaymentHistory() {
        return Collections.unmodifiableList(paymentHistory);
    }
    
    public Money getRemainingBalance() {
        return currentBalance;
    }
    
    // Additional missing methods
    
    public boolean canBeCompleted() {
        return status == LoanStatus.ACTIVE && 
               currentBalance.getAmount().compareTo(java.math.BigDecimal.ZERO) == 0;
    }
    
    public void markAsCompleted() {
        if (!canBeCompleted()) {
            throw new IllegalStateException("Loan cannot be completed in current status: " + status);
        }
        this.status = LoanStatus.PAID_OFF;
        this.paidOffDate = LocalDateTime.now();
        this.lastModifiedAt = LocalDateTime.now();
    }
    
    public Money getTotalAmount() {
        return principalAmount;
    }
    
    public Money getTotalPaid() {
        return totalPaid;
    }
    
    public boolean canBeRestructured() {
        return status == LoanStatus.ACTIVE;
    }
    
    public boolean canBeActivated() {
        return status == LoanStatus.APPROVED;
    }
    
    public LocalDateTime getApplicationDate() {
        return createdAt;
    }
    
    public BigDecimal getInterestRate() {
        return currentTerms.getInterestRate();
    }
    
    public Integer getTermMonths() {
        return currentTerms.getTermInMonths();
    }
    
    private static String generateEventId() {
        return java.util.UUID.randomUUID().toString();
    }
    
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        Loan loan = (Loan) obj;
        return Objects.equals(getId(), loan.getId());
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(getId());
    }
    
    @Override
    public String toString() {
        return String.format("Loan{id=%s, customerId=%s, amount=%s, status=%s}",
                getId(), customerId, principalAmount, status);
    }
}