package com.bank.loanmanagement.loan.infrastructure.persistence;

import com.bank.loanmanagement.loan.domain.customer.CustomerId;
import com.bank.loanmanagement.loan.domain.loan.LoanId;
import com.bank.loanmanagement.loan.domain.payment.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public class PaymentRepositoryImpl implements PaymentRepository {
    
    private final JpaPaymentRepository jpaPaymentRepository;
    
    @Autowired
    public PaymentRepositoryImpl(JpaPaymentRepository jpaPaymentRepository) {
        this.jpaPaymentRepository = jpaPaymentRepository;
    }
    
    @Override
    public Payment save(Payment payment) {
        return jpaPaymentRepository.save(payment);
    }
    
    @Override
    public Optional<Payment> findById(PaymentId id) {
        return jpaPaymentRepository.findById(id);
    }
    
    @Override
    public Optional<Payment> findByPaymentReference(String paymentReference) {
        return jpaPaymentRepository.findByPaymentReference(paymentReference);
    }
    
    @Override
    public List<Payment> findByLoanId(LoanId loanId) {
        return jpaPaymentRepository.findByLoanId(loanId);
    }
    
    @Override
    public List<Payment> findByCustomerId(CustomerId customerId) {
        return jpaPaymentRepository.findByCustomerId(customerId);
    }
    
    @Override
    public List<Payment> findByStatus(PaymentStatus status) {
        return jpaPaymentRepository.findByStatus(status);
    }
    
    @Override
    public List<Payment> findByPaymentDateBetween(LocalDate startDate, LocalDate endDate) {
        return jpaPaymentRepository.findByPaymentDateBetween(startDate, endDate);
    }
    
    @Override
    public List<Payment> findAll() {
        return jpaPaymentRepository.findAll();
    }
    
    @Override
    public void delete(Payment payment) {
        jpaPaymentRepository.delete(payment);
    }
}