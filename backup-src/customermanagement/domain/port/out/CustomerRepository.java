
package com.bank.loanmanagement.customermanagement.domain.port.out;

import com.bank.loanmanagement.customermanagement.domain.model.Customer;

import java.util.List;
import java.util.Optional;

public interface CustomerRepository {
    Customer save(Customer customer);
    Optional<Customer> findById(Long customerId);
    Optional<Customer> findByEmail(String email);
    List<Customer> findAll();
    void deleteById(Long customerId);
    boolean existsByEmail(String email);
}
