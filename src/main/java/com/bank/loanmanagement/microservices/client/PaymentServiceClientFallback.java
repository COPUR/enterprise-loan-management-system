package com.bank.loanmanagement.microservices.client;

import com.bank.loanmanagement.domain.payment.Payment;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

@Component
public class PaymentServiceClientFallback implements PaymentServiceClient {

    @Override
    public ResponseEntity<Payment> processPayment(Payment payment) {
        return ResponseEntity.status(503).build();
    }

    @Override
    public ResponseEntity<Boolean> reversePayment(String paymentId) {
        return ResponseEntity.ok(false);
    }

    @Override
    public ResponseEntity<Payment> getPayment(String paymentId) {
        return ResponseEntity.status(503).build();
    }

    @Override
    public ResponseEntity<Payment[]> getPaymentsByLoanId(String loanId) {
        return ResponseEntity.status(503).body(new Payment[0]);
    }
}