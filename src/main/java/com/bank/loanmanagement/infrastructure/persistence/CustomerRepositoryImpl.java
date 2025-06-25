package com.bank.loanmanagement.infrastructure.persistence;

import com.bank.loanmanagement.domain.customer.Customer;
import com.bank.loanmanagement.domain.customer.CustomerId;
import com.bank.loanmanagement.domain.customer.CustomerRepository;
import com.bank.loanmanagement.domain.customer.CustomerStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public class CustomerRepositoryImpl implements CustomerRepository {
    
    private final JpaCustomerRepository jpaCustomerRepository;
    
    @Autowired
    public CustomerRepositoryImpl(JpaCustomerRepository jpaCustomerRepository) {
        this.jpaCustomerRepository = jpaCustomerRepository;
    }
    
    @Override
    public Customer save(Customer customer) {
        return jpaCustomerRepository.save(customer);
    }
    
    @Override
    public Optional<Customer> findById(CustomerId id) {
        return jpaCustomerRepository.findById(id);
    }
    
    @Override
    public Optional<Customer> findByEmail(String email) {
        return jpaCustomerRepository.findByEmail(email);
    }
    
    @Override
    public Optional<Customer> findBySsn(String ssn) {
        return jpaCustomerRepository.findBySsn(ssn);
    }
    
    @Override
    public List<Customer> findByStatus(CustomerStatus status) {
        return jpaCustomerRepository.findByStatus(status);
    }
    
    @Override
    public List<Customer> findAll() {
        return jpaCustomerRepository.findAll();
    }
    
    @Override
    public void delete(Customer customer) {
        jpaCustomerRepository.delete(customer);
    }
    
    @Override
    public boolean existsByEmail(String email) {
        return jpaCustomerRepository.existsByEmail(email);
    }
    
    @Override
    public boolean existsBySsn(String ssn) {
        return jpaCustomerRepository.existsBySsn(ssn);
    }
}