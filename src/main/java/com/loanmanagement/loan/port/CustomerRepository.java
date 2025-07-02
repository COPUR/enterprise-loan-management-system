// domain/port/CustomerRepository.java
package com.loanmanagement.loan.port;

import com.loanmanagement.domain.model.entity.Customer;
import java.util.Optional;

public interface CustomerRepository {
    Optional<Customer> findById(Long id);
    Customer save(Customer customer);
}

// domain/port/LoanRepository.java
