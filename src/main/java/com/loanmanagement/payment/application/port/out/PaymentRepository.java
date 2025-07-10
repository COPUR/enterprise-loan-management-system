package com.loanmanagement.payment.application.port.out;

import com.loanmanagement.payment.domain.model.Payment;
import com.loanmanagement.payment.domain.model.PaymentStatus;
import java.util.List;
import java.util.Optional;

public interface PaymentRepository {
    
    Payment save(Payment payment);
    
    Optional<Payment> findById(Long id);
    
    List<Payment> findByLoanId(Long loanId);
    
    List<Payment> findByStatus(PaymentStatus status);
    
    List<Payment> findOverduePayments();
    
    void deleteById(Long id);
}