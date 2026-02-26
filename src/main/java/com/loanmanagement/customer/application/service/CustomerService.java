package com.loanmanagement.customer.application.service;

import com.loanmanagement.customer.application.port.in.CreateCustomerUseCase;
import com.loanmanagement.customer.application.port.in.GetCustomerUseCase;
import com.loanmanagement.customer.application.port.out.CustomerRepository;
import com.loanmanagement.customer.domain.model.Customer;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@Transactional
public class CustomerService implements CreateCustomerUseCase, GetCustomerUseCase {
    
    private final CustomerRepository customerRepository;
    
    public CustomerService(CustomerRepository customerRepository) {
        this.customerRepository = customerRepository;
    }
    
    @Override
    public Customer createCustomer(CreateCustomerCommand command) {
        if (customerRepository.existsByEmail(command.email())) {
            throw new IllegalArgumentException("Customer with email " + command.email() + " already exists");
        }
        
        Customer customer = new Customer(
                command.firstName(),
                command.lastName(),
                command.email(),
                command.phone(),
                command.dateOfBirth(),
                command.monthlyIncome()
        );
        
        return customerRepository.save(customer);
    }
    
    @Override
    @Transactional(readOnly = true)
    public Optional<Customer> getCustomerById(Long customerId) {
        return customerRepository.findById(customerId);
    }
    
    @Override
    @Transactional(readOnly = true)
    public Optional<Customer> getCustomerByEmail(String email) {
        return customerRepository.findByEmail(email);
    }
}