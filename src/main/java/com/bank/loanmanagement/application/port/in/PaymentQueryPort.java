package com.bank.loanmanagement.application.port.in;

import com.bank.loanmanagement.domain.payment.Payment;
import com.bank.loanmanagement.domain.payment.PaymentId;
import com.bank.loanmanagement.domain.loan.LoanId;
import com.bank.loanmanagement.domain.customer.CustomerId;

import java.util.List;
import java.util.Optional;

/**
 * Port for querying payment data
 * Following hexagonal architecture principles
 */
public interface PaymentQueryPort {
    
    Optional<Payment> findById(PaymentId paymentId);
    
    List<Payment> findAll();
    
    List<Payment> findByLoanId(LoanId loanId);
    
    List<Payment> findByCustomerId(CustomerId customerId);
    
    List<Payment> findByStatus(String status);
}