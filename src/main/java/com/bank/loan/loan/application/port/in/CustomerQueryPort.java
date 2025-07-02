package com.bank.loanmanagement.loan.application.port.in;

import com.bank.loan.loan.domain.customer.Customer;
import com.bank.loanmanagement.loan.domain.customer.CustomerId;

import java.util.List;
import java.util.Optional;

/**
 * Port for querying customer data
 * Following hexagonal architecture principles
 */
public interface CustomerQueryPort {
    
    Optional<Customer> findById(CustomerId customerId);
    
    List<Customer> findAll();
    
    Optional<Customer> findByEmail(String email);
    
    List<Customer> findByRiskLevel(String riskLevel);
}