package com.loanmanagement.loan.application.service;

import com.loanmanagement.loan.application.port.in.GetLoanQuery;
import com.loanmanagement.loan.application.port.out.LoanEligibilityPort;
import com.loanmanagement.loan.application.port.out.LoanRepository;
import com.loanmanagement.loan.domain.model.Loan;
import com.loanmanagement.loan.domain.model.LoanStatus;
import com.loanmanagement.payment.application.port.out.PaymentRepository;
import com.loanmanagement.payment.domain.model.Payment;
import com.loanmanagement.shared.application.port.out.LoggingFactory;
import com.loanmanagement.shared.application.port.out.LoggingPort;
import com.loanmanagement.shared.application.port.out.TransactionManager;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * Framework-Agnostic Application Service for Loan Queries
 * Implements the Get Loan Query following clean architecture principles
 * Does not depend on any specific framework
 */
public class LoanQueryServiceImpl implements GetLoanQuery {
    
    private final LoanRepository loanRepository;
    private final PaymentRepository paymentRepository;
    private final LoanEligibilityPort loanEligibilityPort;
    private final TransactionManager transactionManager;
    private final LoggingPort logger;
    
    public LoanQueryServiceImpl(
            LoanRepository loanRepository, 
            PaymentRepository paymentRepository,
            LoanEligibilityPort loanEligibilityPort,
            TransactionManager transactionManager,
            LoggingFactory loggingFactory) {
        this.loanRepository = loanRepository;
        this.paymentRepository = paymentRepository;
        this.loanEligibilityPort = loanEligibilityPort;
        this.transactionManager = transactionManager;
        this.logger = loggingFactory.getLogger(LoanQueryServiceImpl.class);
    }
    
    @Override
    public Optional<Loan> getLoanById(Long loanId) {
        logger.debug("Retrieving loan by ID: {}", loanId);
        return transactionManager.executeInReadOnlyTransaction(() -> 
                loanRepository.findById(loanId));
    }
    
    @Override
    public List<Loan> getLoansByCustomerId(Long customerId) {
        logger.debug("Retrieving loans for customer: {}", customerId);
        return transactionManager.executeInReadOnlyTransaction(() -> 
                loanRepository.findByCustomerId(customerId));
    }
    
    @Override
    public List<Loan> getLoansByStatus(LoanStatus status) {
        logger.debug("Retrieving loans with status: {}", status);
        return transactionManager.executeInReadOnlyTransaction(() -> 
                loanRepository.findByStatus(status));
    }
    
    @Override
    public LoanWithPaymentHistory getLoanWithPaymentHistory(Long loanId) {
        logger.debug("Retrieving loan with payment history: {}", loanId);
        
        return transactionManager.executeInReadOnlyTransaction(() -> {
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
        });
    }
    
    @Override
    public LoanEligibilityResult calculateLoanEligibility(LoanEligibilityQuery query) {
        logger.debug("Calculating loan eligibility for customer: {}", query.customerId());
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