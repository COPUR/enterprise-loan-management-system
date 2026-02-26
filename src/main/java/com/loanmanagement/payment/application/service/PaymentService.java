package com.loanmanagement.payment.application.service;

import com.loanmanagement.loan.application.port.out.LoanRepository;
import com.loanmanagement.loan.domain.model.Loan;
import com.loanmanagement.payment.application.port.in.ProcessPaymentUseCase;
import com.loanmanagement.payment.application.port.out.PaymentRepository;
import com.loanmanagement.payment.domain.model.Payment;
import com.loanmanagement.payment.domain.model.PaymentStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class PaymentService implements ProcessPaymentUseCase {
    
    private final PaymentRepository paymentRepository;
    private final LoanRepository loanRepository;
    
    public PaymentService(PaymentRepository paymentRepository, LoanRepository loanRepository) {
        this.paymentRepository = paymentRepository;
        this.loanRepository = loanRepository;
    }
    
    @Override
    public Payment processPayment(ProcessPaymentCommand command) {
        Loan loan = loanRepository.findById(command.loanId())
                .orElseThrow(() -> new IllegalArgumentException("Loan not found: " + command.loanId()));
        
        if (!loan.isActive()) {
            throw new IllegalArgumentException("Cannot process payment for inactive loan");
        }
        
        Payment payment = new Payment(
                command.loanId(),
                command.amount(),
                command.paymentDate(),
                command.type()
        );
        
        payment.processPayment(command.paymentDate());
        
        return paymentRepository.save(payment);
    }
}