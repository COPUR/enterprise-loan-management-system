
package com.bank.loanmanagement.customermanagement.infrastructure.adapter.out.persistence;

import com.bank.loanmanagement.customermanagement.domain.model.Customer;
import com.bank.loanmanagement.customermanagement.domain.port.out.CustomerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class CustomerRepositoryAdapter implements CustomerRepository {
    
    private final CustomerJpaRepository jpaRepository;
    
    @Override
    public Customer save(Customer customer) {
        return jpaRepository.save(customer);
    }
    
    @Override
    public Optional<Customer> findById(Long customerId) {
        return jpaRepository.findById(customerId);
    }
    
    @Override
    public Optional<Customer> findByEmail(String email) {
        return jpaRepository.findByEmail(email);
    }
    
    @Override
    public List<Customer> findAll() {
        return jpaRepository.findAll();
    }
    
    @Override
    public void deleteById(Long customerId) {
        jpaRepository.deleteById(customerId);
    }
    
    @Override
    public boolean existsByEmail(String email) {
        return jpaRepository.existsByEmail(email);
    }
}
