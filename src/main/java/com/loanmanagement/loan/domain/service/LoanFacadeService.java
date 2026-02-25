package com.loanmanagement.loan.domain.service;

import com.loanmanagement.loan.domain.model.*;
import com.loanmanagement.payment.domain.model.PaymentAllocation;
import com.loanmanagement.shared.domain.Money;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Domain Service Facade for Loan Operations
 * Provides simplified interfaces for complex domain operations
 */
@Component
@Slf4j
public class LoanFacadeService {
    
    /**
     * Create a simple loan with basic parameters
     */
    public Loan createSimpleLoan(Long customerId, Money principalAmount, BigDecimal interestRate, Integer termMonths) {
        log.debug("Creating simple loan for customer: {}", customerId);
        
        CustomerId customerDomainId = CustomerId.of(customerId.toString());
        LoanId loanId = LoanId.generate();
        LoanOfficerId officerId = LoanOfficerId.generate();
        
        LoanTerms terms = LoanTerms.builder()
                .interestRate(interestRate)
                .termInMonths(termMonths)
                .paymentFrequency(PaymentFrequency.MONTHLY)
                .build();
        
        return Loan.createApplication(
                loanId,
                customerDomainId,
                principalAmount,
                LoanPurpose.PERSONAL,
                terms,
                officerId
        );
    }
    
    /**
     * Simple loan approval
     */
    public void approveLoan(Loan loan) {
        log.debug("Approving loan: {}", loan.getId());
        
        ApprovalConditions conditions = ApprovalConditions.builder()
                .approvedAmount(loan.getPrincipalAmount())
                .approvedTerms(loan.getOriginalTerms())
                .expirationDate(java.time.LocalDate.now().plusDays(30))
                .conditions(java.util.List.of())
                .build();
        
        LoanOfficerId defaultOfficer = LoanOfficerId.of("DEFAULT-OFFICER-1"); // Default officer
        
        loan.approve(conditions, defaultOfficer);
    }
    
    /**
     * Simple loan rejection
     */
    public void rejectLoan(Loan loan, String reason) {
        log.debug("Rejecting loan: {} with reason: {}", loan.getId(), reason);
        
        RejectionReason rejectionReason = RejectionReason.builder()
                .primaryReason(reason)
                .details(java.util.List.of(reason))
                .appealable(true)
                .appealDeadline(LocalDateTime.now().plusDays(30).toLocalDate())
                .build();
        
        LoanOfficerId defaultOfficer = LoanOfficerId.of("DEFAULT-OFFICER-1"); // Default officer
        
        loan.reject(rejectionReason, defaultOfficer);
    }
    
    /**
     * Simple loan disbursement
     */
    public void disburseLoan(Loan loan) {
        log.debug("Disbursing loan: {}", loan.getId());
        
        DisbursementInstructions instructions = DisbursementInstructions.builder()
                .accountNumber("DEFAULT-ACCOUNT")
                .routingNumber("DEFAULT-ROUTING")
                .bankName("Default Bank")
                .disbursementMethod(DisbursementMethod.ACH_TRANSFER)
                .specialInstructions("")
                .build();
        
        LoanOfficerId defaultOfficer = LoanOfficerId.of("DEFAULT-OFFICER-1"); // Default officer
        
        loan.disburse(instructions, defaultOfficer);
    }
    
    /**
     * Simple loan completion
     */
    public void completeLoan(Loan loan) {
        log.debug("Completing loan: {}", loan.getId());
        
        // Create a final payment to complete the loan
        Money finalPayment = loan.getRemainingBalance();
        
        PaymentAllocation allocation = PaymentAllocation.builder()
                .allocationId(java.util.UUID.randomUUID().toString())
                .loanId(loan.getId().getValue())
                .totalAmount(finalPayment.getAmount())
                .principalAmount(finalPayment.getAmount())
                .interestAmount(BigDecimal.ZERO)
                .feesAmount(BigDecimal.ZERO)
                .currencyCode(finalPayment.getCurrency())
                .build();
        
        loan.makePayment(finalPayment, allocation, LocalDateTime.now());
    }
    
    /**
     * Simple loan default marking
     */
    public void markLoanAsDefaulted(Loan loan) {
        log.debug("Marking loan as defaulted: {}", loan.getId());
        
        DefaultReason defaultReason = DefaultReason.builder()
                .reason("Loan marked as defaulted")
                .daysPastDue(30)
                .totalAmountPastDue(loan.getRemainingBalance())
                .missedPayments(3)
                .lastPaymentDate(java.time.LocalDate.now().minusDays(30))
                .collectionActions(java.util.List.of("Email notification"))
                .build();
        
        LoanOfficerId defaultOfficer = LoanOfficerId.of("DEFAULT-OFFICER-1"); // Default officer
        loan.markAsDefaulted(defaultReason, defaultOfficer);
    }
    
    /**
     * Check if loan can be activated (disbursed)
     */
    public boolean canLoanBeActivated(Loan loan) {
        return loan.getStatus() == LoanStatus.APPROVED;
    }
    
    /**
     * Check if loan can be completed
     */
    public boolean canLoanBeCompleted(Loan loan) {
        return loan.isActive() && loan.getRemainingBalance().getAmount().compareTo(BigDecimal.ZERO) <= 0;
    }
    
    /**
     * Check if loan can be restructured
     */
    public boolean canLoanBeRestructured(Loan loan) {
        return loan.isActive() || loan.getStatus() == LoanStatus.RESTRUCTURED;
    }
    
    /**
     * Simple loan restructuring
     */
    public void restructureLoan(Loan loan, BigDecimal newInterestRate, Integer newTermMonths) {
        log.debug("Restructuring loan: {} with new rate: {} and term: {}", 
                loan.getId(), newInterestRate, newTermMonths);
        
        LoanTerms newTerms = LoanTerms.builder()
                .interestRate(newInterestRate)
                .termInMonths(newTermMonths)
                .paymentFrequency(PaymentFrequency.MONTHLY)
                .build();
        
        RestructuringReason reason = RestructuringReason.builder()
                .reason("Customer request")
                .justification("Financial hardship")
                .temporaryHardship(false)
                .build();
        
        LoanOfficerId defaultOfficer = LoanOfficerId.of("DEFAULT-OFFICER-1"); // Default officer
        
        loan.restructure(newTerms, reason, defaultOfficer);
    }
}