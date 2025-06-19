
package com.bank.loanmanagement.customermanagement.application.service;

import com.bank.loanmanagement.customermanagement.domain.model.Customer;
import com.bank.loanmanagement.customermanagement.domain.model.CustomerStatus;
import com.bank.loanmanagement.customermanagement.domain.port.in.CreateCustomerCommand;
import com.bank.loanmanagement.customermanagement.domain.port.in.CustomerManagementUseCase;
import com.bank.loanmanagement.customermanagement.domain.port.in.UpdateCustomerCommand;
import com.bank.loanmanagement.customermanagement.domain.port.out.CustomerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class CustomerManagementService implements CustomerManagementUseCase {
    
    private final CustomerRepository customerRepository;
    
    @Override
    public Customer createCustomer(CreateCustomerCommand command) {
        validateCustomerCreation(command);
        
        Customer customer = Customer.builder()
                .firstName(command.getFirstName())
                .lastName(command.getLastName())
                .email(command.getEmail())
                .phoneNumber(command.getPhoneNumber())
                .creditScore(command.getCreditScore())
                .monthlyIncome(command.getMonthlyIncome())
                .creditLimit(command.getCreditLimit())
                .availableCredit(command.getCreditLimit())
                .status(CustomerStatus.ACTIVE)
                .build();
        
        return customerRepository.save(customer);
    }
    
    @Override
    public Customer updateCustomer(UpdateCustomerCommand command) {
        Customer customer = customerRepository.findById(command.getCustomerId())
                .orElseThrow(() -> new CustomerNotFoundException("Customer not found"));
        
        updateCustomerFields(customer, command);
        return customerRepository.save(customer);
    }
    
    @Override
    @Transactional(readOnly = true)
    public Customer getCustomerById(Long customerId) {
        return customerRepository.findById(customerId)
                .orElseThrow(() -> new CustomerNotFoundException("Customer not found"));
    }
    
    @Override
    @Transactional(readOnly = true)
    public List<Customer> getAllCustomers() {
        return customerRepository.findAll();
    }
    
    @Override
    public void deleteCustomer(Long customerId) {
        if (!customerRepository.findById(customerId).isPresent()) {
            throw new CustomerNotFoundException("Customer not found");
        }
        customerRepository.deleteById(customerId);
    }
    
    @Override
    public void reserveCredit(Long customerId, BigDecimal amount) {
        Customer customer = getCustomerById(customerId);
        customer.reserveCredit(amount);
        customerRepository.save(customer);
    }
    
    @Override
    public void releaseCredit(Long customerId, BigDecimal amount) {
        Customer customer = getCustomerById(customerId);
        customer.releaseCredit(amount);
        customerRepository.save(customer);
    }
    
    private void validateCustomerCreation(CreateCustomerCommand command) {
        if (customerRepository.existsByEmail(command.getEmail())) {
            throw new CustomerAlreadyExistsException("Customer with email already exists");
        }
    }
    
    private void updateCustomerFields(Customer customer, UpdateCustomerCommand command) {
        if (command.getFirstName() != null) {
            customer.setFirstName(command.getFirstName());
        }
        if (command.getLastName() != null) {
            customer.setLastName(command.getLastName());
        }
        if (command.getEmail() != null) {
            customer.setEmail(command.getEmail());
        }
        if (command.getPhoneNumber() != null) {
            customer.setPhoneNumber(command.getPhoneNumber());
        }
        if (command.getCreditScore() != null) {
            customer.setCreditScore(command.getCreditScore());
        }
        if (command.getMonthlyIncome() != null) {
            customer.setMonthlyIncome(command.getMonthlyIncome());
        }
        if (command.getCreditLimit() != null) {
            customer.setCreditLimit(command.getCreditLimit());
        }
        if (command.getStatus() != null) {
            customer.setStatus(command.getStatus());
        }
    }
}
