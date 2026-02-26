package com.loanmanagement.customer.application.port.in;

import com.loanmanagement.customer.domain.model.Customer;
import java.util.Optional;

public interface GetCustomerUseCase {
    
    Optional<Customer> getCustomerById(Long customerId);
    
    Optional<Customer> getCustomerByEmail(String email);
}