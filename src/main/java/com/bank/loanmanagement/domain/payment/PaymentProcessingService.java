package com.bank.loanmanagement.domain.payment;

import com.bank.loanmanagement.domain.loan.Loan;
import com.bank.loanmanagement.domain.loan.LoanRepository;
import com.bank.loanmanagement.domain.shared.Money;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

@Service
public class PaymentProcessingService {
    
    private final PaymentRepository paymentRepository;
    private final LoanRepository loanRepository;
    
    public PaymentProcessingService(PaymentRepository paymentRepository, LoanRepository loanRepository) {
        this.paymentRepository = paymentRepository;
        this.loanRepository = loanRepository;
    }
    
    @Transactional
    public void processPayment(Payment payment) {
        // Validate payment
        if (!isValidPayment(payment)) {
            payment.fail("Invalid payment details");
            paymentRepository.save(payment);
            return;
        }
        
        // Find the loan
        Loan loan = loanRepository.findById(payment.getLoanId())
            .orElseThrow(() -> new IllegalArgumentException("Loan not found: " + payment.getLoanId()));
        
        try {
            // Process the payment against the loan
            loan.makePayment(payment.getAmount(), payment.getPaymentDate());
            
            // Mark payment as processed
            payment.process("SYSTEM");
            
            // Save both entities
            loanRepository.save(loan);
            paymentRepository.save(payment);
            
        } catch (Exception e) {
            payment.fail("Payment processing failed: " + e.getMessage());
            paymentRepository.save(payment);
            throw e;
        }
    }
    
    @Transactional
    public Payment createPayment(Loan loan, Money amount, PaymentMethod paymentMethod, LocalDate paymentDate) {
        // Validate payment amount
        if (amount.isZero() || amount.isNegative()) {
            throw new IllegalArgumentException("Payment amount must be positive");
        }
        
        if (amount.isGreaterThan(loan.getOutstandingBalance())) {
            throw new IllegalArgumentException("Payment amount cannot exceed outstanding balance");
        }
        
        // Create payment
        Payment payment = Payment.builder()
            .id(PaymentId.generate())
            .loanId(loan.getId())
            .customerId(loan.getCustomerId())
            .amount(amount)
            .paymentDate(paymentDate)
            .paymentMethod(paymentMethod)
            .status(PaymentStatus.PENDING)
            .paymentReference(generatePaymentReference())
            .description("Loan payment for loan " + loan.getId().getValue())
            .build();
        
        return paymentRepository.save(payment);
    }
    
    @Transactional
    public void reversePayment(Payment payment, String reason) {
        if (!payment.isSuccessful()) {
            throw new IllegalStateException("Only successful payments can be reversed");
        }
        
        // Find the loan and reverse the payment
        Loan loan = loanRepository.findById(payment.getLoanId())
            .orElseThrow(() -> new IllegalArgumentException("Loan not found: " + payment.getLoanId()));
        
        // Add the payment amount back to outstanding balance
        loan.setOutstandingBalance(loan.getOutstandingBalance().add(payment.getAmount()));
        
        // Mark payment as reversed
        payment.reverse(reason);
        
        // Save both entities
        loanRepository.save(loan);
        paymentRepository.save(payment);
    }
    
    private boolean isValidPayment(Payment payment) {
        if (payment.getAmount() == null || payment.getAmount().isZero() || payment.getAmount().isNegative()) {
            return false;
        }
        
        if (payment.getLoanId() == null || payment.getCustomerId() == null) {
            return false;
        }
        
        if (payment.getPaymentDate() == null || payment.getPaymentDate().isAfter(LocalDate.now())) {
            return false;
        }
        
        return true;
    }
    
    private String generatePaymentReference() {
        // In a real system, this would generate a unique reference number
        return "PAY-" + System.currentTimeMillis();
    }
}