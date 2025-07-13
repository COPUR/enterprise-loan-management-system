package com.bank.loan.domain.service;

import com.bank.loan.domain.dto.*;
import com.bank.loan.domain.model.Loan;
import com.bank.loan.domain.model.PaymentResult;

import java.math.BigDecimal;
import java.util.List;

/**
 * Service interface for loan operations
 */
public interface LoanService {
    
    /**
     * Create a new loan
     */
    LoanResponse createLoan(LoanRequest request);
    
    /**
     * Get loans by customer ID
     */
    List<LoanResponse> getLoansByCustomer(String customerId);
    
    /**
     * Get loans by customer ID with filters
     */
    List<LoanResponse> getLoansByCustomer(Long customerId, Integer numberOfInstallments, Boolean isPaid);
    
    /**
     * Get loan by ID
     */
    LoanResponse getLoanById(String loanId);
    
    /**
     * Get installments for a loan
     */
    List<InstallmentResponse> getInstallmentsByLoan(String loanId);
    
    /**
     * Make a payment against a loan
     */
    PaymentResponse payLoan(String loanId, PaymentRequest request);
    
    /**
     * Pay a specific installment
     */
    PaymentResponse payInstallment(String loanId, Integer installmentNumber, PaymentRequest request);
    
    /**
     * Get loan by ID (entity)
     */
    Loan findLoanById(Long loanId);
}