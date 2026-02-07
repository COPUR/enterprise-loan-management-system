package com.bank.loan.application;

import com.bank.loan.application.dto.CreateLoanRequest;
import com.bank.loan.application.dto.LoanResponse;
import com.bank.loan.domain.*;
import com.bank.shared.kernel.domain.CustomerId;
import com.bank.shared.kernel.domain.Money;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Application Service for Loan Management
 * 
 * Implements functional requirements:
 * - FR-005: Loan Application & Origination
 * - FR-006: Loan Approval Workflow
 * - FR-007: Loan Disbursement
 * - FR-008: Loan Payment Processing
 */
@Service("loanService")
@Transactional
public class LoanManagementService {
    
    private final LoanRepository loanRepository;
    private final CustomerCreditService customerCreditService;
    
    public LoanManagementService(LoanRepository loanRepository, 
                                CustomerCreditService customerCreditService) {
        this.loanRepository = loanRepository;
        this.customerCreditService = customerCreditService;
    }
    
    /**
     * FR-005: Create a new loan application
     */
    public LoanResponse createLoanApplication(CreateLoanRequest request) {
        // Validate request
        request.validate();
        
        CustomerId customerId = CustomerId.of(request.customerId());
        Money principalAmount = request.getPrincipalAsMoney();
        
        // Check customer credit availability
        if (!customerCreditService.hasAvailableCredit(customerId, principalAmount)) {
            Money availableCredit = customerCreditService.getAvailableCredit(customerId);
            throw InsufficientCreditException.forCustomer(
                request.customerId(), 
                principalAmount.toString(), 
                availableCredit.toString());
        }
        
        // Create loan
        Loan loan = Loan.create(
            LoanId.generate(),
            customerId,
            principalAmount,
            InterestRate.of(request.annualInterestRate()),
            LoanTerm.ofMonths(request.termInMonths())
        );
        
        // Save loan
        Loan savedLoan = loanRepository.save(loan);
        
        return LoanResponse.from(savedLoan);
    }
    
    /**
     * FR-006: Approve a loan application
     */
    public LoanResponse approveLoan(String loanId) {
        LoanId id = LoanId.of(loanId);
        Loan loan = loanRepository.findById(id)
            .orElseThrow(() -> LoanNotFoundException.withId(loanId));
        
        loan.approve();
        Loan savedLoan = loanRepository.save(loan);
        
        return LoanResponse.from(savedLoan);
    }
    
    /**
     * FR-006: Reject a loan application
     */
    public LoanResponse rejectLoan(String loanId, String reason) {
        LoanId id = LoanId.of(loanId);
        Loan loan = loanRepository.findById(id)
            .orElseThrow(() -> LoanNotFoundException.withId(loanId));
        
        loan.reject(reason);
        Loan savedLoan = loanRepository.save(loan);
        
        return LoanResponse.from(savedLoan);
    }
    
    /**
     * FR-007: Disburse an approved loan
     */
    public LoanResponse disburseLoan(String loanId) {
        LoanId id = LoanId.of(loanId);
        Loan loan = loanRepository.findById(id)
            .orElseThrow(() -> LoanNotFoundException.withId(loanId));
        
        // Reserve credit for the customer
        customerCreditService.reserveCredit(loan.getCustomerId(), loan.getPrincipalAmount());
        
        loan.disburse();
        Loan savedLoan = loanRepository.save(loan);
        
        return LoanResponse.from(savedLoan);
    }
    
    /**
     * FR-008: Process a loan payment
     */
    public LoanResponse makePayment(String loanId, Money paymentAmount) {
        LoanId id = LoanId.of(loanId);
        Loan loan = loanRepository.findById(id)
            .orElseThrow(() -> LoanNotFoundException.withId(loanId));
        
        boolean wasFullyPaid = loan.getOutstandingBalance().equals(paymentAmount);
        
        loan.makePayment(paymentAmount);
        
        // If loan is fully paid, release the reserved credit
        if (wasFullyPaid) {
            customerCreditService.releaseCredit(loan.getCustomerId(), loan.getPrincipalAmount());
        }
        
        Loan savedLoan = loanRepository.save(loan);
        
        return LoanResponse.from(savedLoan);
    }
    
    /**
     * FR-005: Find loan by ID
     */
    @Transactional(readOnly = true)
    public LoanResponse findLoanById(String loanId) {
        LoanId id = LoanId.of(loanId);
        Loan loan = loanRepository.findById(id)
            .orElseThrow(() -> LoanNotFoundException.withId(loanId));
        
        return LoanResponse.from(loan);
    }

    /**
     * Resolve customer ID for authorization checks
     */
    @Transactional(readOnly = true)
    public String getCustomerIdForLoan(String loanId) {
        LoanId id = LoanId.of(loanId);
        Loan loan = loanRepository.findById(id)
            .orElseThrow(() -> LoanNotFoundException.withId(loanId));

        return loan.getCustomerId().getValue();
    }
    
    /**
     * FR-008: Cancel a loan application
     */
    public LoanResponse cancelLoan(String loanId, String reason) {
        LoanId id = LoanId.of(loanId);
        Loan loan = loanRepository.findById(id)
            .orElseThrow(() -> LoanNotFoundException.withId(loanId));
        
        loan.cancel(reason);
        Loan savedLoan = loanRepository.save(loan);
        
        return LoanResponse.from(savedLoan);
    }
}
