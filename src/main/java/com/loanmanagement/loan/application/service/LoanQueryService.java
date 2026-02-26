package com.loanmanagement.loan.application.service;

import com.loanmanagement.loan.application.port.in.GetLoanQuery;
import com.loanmanagement.loan.application.port.out.LoanEligibilityPort;
import com.loanmanagement.loan.application.port.out.LoanRepository;
import com.loanmanagement.loan.domain.model.Loan;
import com.loanmanagement.loan.domain.model.LoanStatus;
import com.loanmanagement.payment.application.port.out.PaymentRepository;
import com.loanmanagement.payment.domain.model.Payment;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * Application Service for Loan Queries
 * Implements the Get Loan Query following hexagonal architecture principles
 */
@Service
@Transactional(readOnly = true)
@Slf4j
public class LoanQueryService implements GetLoanQuery {
    
    private final LoanRepository loanRepository;
    private final PaymentRepository paymentRepository;
    private final LoanEligibilityPort loanEligibilityPort;
    
    public LoanQueryService(
            LoanRepository loanRepository, 
            PaymentRepository paymentRepository,
            LoanEligibilityPort loanEligibilityPort) {
        this.loanRepository = loanRepository;
        this.paymentRepository = paymentRepository;
        this.loanEligibilityPort = loanEligibilityPort;
    }
    
    @Override
    public Optional<Loan> getLoanById(Long loanId) {
        log.debug("Retrieving loan by ID: {}", loanId);
        return loanRepository.findById(loanId);
    }
    
    @Override
    public List<Loan> getLoansByCustomerId(Long customerId) {
        log.debug("Retrieving loans for customer: {}", customerId);
        return loanRepository.findByCustomerId(customerId);
    }
    
    @Override
    public List<Loan> getLoansByStatus(LoanStatus status) {
        log.debug("Retrieving loans with status: {}", status);
        return loanRepository.findByStatus(status);
    }
    
    @Override
    public LoanWithPaymentHistory getLoanWithPaymentHistory(Long loanId) {
        log.debug("Retrieving loan with payment history: {}", loanId);
        
        Loan loan = loanRepository.findById(loanId)
                .orElseThrow(() -> new IllegalArgumentException("Loan not found: " + loanId));
        
        List<Payment> payments = paymentRepository.findByLoanId(loanId);
        List<PaymentSummary> paymentHistory = payments.stream()
                .map(payment -> new PaymentSummary(
                        payment.getId(),
                        payment.getAmount().getAmount(),
                        payment.getPaymentDate(),
                        payment.getType().name(),
                        payment.getStatus().name()
                ))
                .toList();
        
        BigDecimal outstandingBalance = calculateOutstandingBalance(loan, payments);
        LocalDate nextPaymentDate = calculateNextPaymentDate(loan, payments);
        
        return new LoanWithPaymentHistory(
                loan,
                paymentHistory,
                outstandingBalance,
                nextPaymentDate
        );
    }
    
    @Override
    public LoanEligibilityResult calculateLoanEligibility(LoanEligibilityQuery query) {
        log.debug("Calculating loan eligibility for customer: {}", query.customerId());
        return loanEligibilityPort.assessEligibility(query);
    }
    
    private BigDecimal calculateOutstandingBalance(Loan loan, List<Payment> payments) {
        BigDecimal totalPaid = payments.stream()
                .filter(payment -> payment.getStatus().name().equals("COMPLETED"))
                .map(payment -> payment.getAmount().getAmount())
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        return loan.getTotalAmount().getAmount().subtract(totalPaid);
    }
    
    private LocalDate calculateNextPaymentDate(Loan loan, List<Payment> payments) {
        // Simplified calculation - in real implementation would consider payment schedule
        return LocalDate.now().plusMonths(1);
    }
}