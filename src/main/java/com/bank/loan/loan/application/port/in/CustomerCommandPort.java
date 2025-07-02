package com.bank.loanmanagement.loan.application.port.in;

import com.bank.loan.loan.domain.customer.Customer;
import com.bank.loanmanagement.loan.domain.customer.CustomerId;

/**
 * Port for customer command operations
 * Following hexagonal architecture principles
 */
public interface CustomerCommandPort {
    
    Customer createCustomer(Customer customer);
    
    Customer updateCustomer(Customer customer);
    
    void deleteCustomer(CustomerId customerId);
    
    Customer reserveCredit(CustomerId customerId, java.math.BigDecimal amount);
    
    Customer releaseCredit(CustomerId customerId, java.math.BigDecimal amount);
}