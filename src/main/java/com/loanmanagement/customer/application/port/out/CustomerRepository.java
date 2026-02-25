package com.loanmanagement.customer.application.port.out;

import com.loanmanagement.customer.domain.model.Customer;
import java.util.Optional;

public interface CustomerRepository {
    
    Customer save(Customer customer);
    
    Optional<Customer> findById(Long id);
    
    Optional<Customer> findByEmail(String email);
    
    void deleteById(Long id);
    
    boolean existsByEmail(String email);
}