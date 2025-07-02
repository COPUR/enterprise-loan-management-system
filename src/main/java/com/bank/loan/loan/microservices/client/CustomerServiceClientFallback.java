package com.bank.loanmanagement.loan.microservices.client;

import com.bank.loan.loan.domain.customer.Customer;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

@Component
public class CustomerServiceClientFallback implements CustomerServiceClient {

    @Override
    public ResponseEntity<Customer> getCustomer(String customerId) {
        return ResponseEntity.status(503).build();
    }

    @Override
    public ResponseEntity<Double> getCreditLimit(String customerId) {
        return ResponseEntity.status(503).build();
    }

    @Override
    public ResponseEntity<Boolean> reserveCreditLimit(String customerId, Double amount) {
        return ResponseEntity.ok(false);
    }

    @Override
    public ResponseEntity<Boolean> releaseCreditLimit(String customerId, Double amount) {
        return ResponseEntity.ok(false);
    }

    @Override
    public ResponseEntity<Boolean> checkLoanEligibility(String customerId) {
        return ResponseEntity.ok(false);
    }
}