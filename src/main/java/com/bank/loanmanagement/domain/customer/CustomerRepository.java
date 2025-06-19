package com.bank.loanmanagement.domain.customer;

import java.util.List;
import java.util.Optional;

public interface CustomerRepository {
    
    Customer save(Customer customer);
    
    Optional<Customer> findById(CustomerId id);
    
    Optional<Customer> findByEmail(String email);
    
    Optional<Customer> findBySsn(String ssn);
    
    List<Customer> findByStatus(CustomerStatus status);
    
    List<Customer> findAll();
    
    void delete(Customer customer);
    
    boolean existsByEmail(String email);
    
    boolean existsBySsn(String ssn);
}