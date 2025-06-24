package com.bank.loanmanagement.application.service;

import com.bank.loanmanagement.domain.model.Customer;
import com.bank.loanmanagement.domain.model.CustomerStatus;
import com.bank.loanmanagement.domain.port.in.CreateCustomerCommand;
import com.bank.loanmanagement.domain.port.in.CustomerManagementUseCase;
import com.bank.loanmanagement.domain.port.in.UpdateCustomerCommand;
import com.bank.loanmanagement.domain.port.out.CustomerRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Application service implementing customer management use cases.
 * Orchestrates domain operations and coordinates with infrastructure.
 */
@Service
@Transactional
public class CustomerManagementService implements CustomerManagementUseCase {

    private final CustomerRepository customerRepository;

    public CustomerManagementService(CustomerRepository customerRepository) {
        this.customerRepository = customerRepository;
    }

    @Override
    public Customer createCustomer(CreateCustomerCommand command) {
        // Check if customer already exists
        if (customerRepository.existsByEmail(command.getEmail())) {
            throw new CustomerAlreadyExistsException(
                    "Customer with email " + command.getEmail() + " already exists");
        }

        // Create new customer using domain constructor
        Customer customer = new Customer(
                command.getFirstName(),
                command.getLastName(),
                command.getEmail(),
                command.getPhoneNumber(),
                command.getCreditScore(),
                command.getMonthlyIncome()
        );

        return customerRepository.save(customer);
    }

    @Override
    public Customer updateCustomer(UpdateCustomerCommand command) {
        Customer customer = customerRepository.findById(command.getCustomerId())
                .orElseThrow(() -> new CustomerNotFoundException(
                        "Customer not found with ID: " + command.getCustomerId()));

        // Update fields if provided
        if (command.getFirstName() != null) {
            // Note: In a real implementation, you'd need to add update methods to Customer
            // For now, this shows the pattern
        }

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

    @Override
    @Transactional(readOnly = true)
    public List<Customer> getAllCustomers(int page, int size) {
        return customerRepository.findAll(page, size);
    }

    @Override
    public void reserveCustomerCredit(Long customerId, BigDecimal amount) {
        Customer customer = customerRepository.findById(customerId)
                .orElseThrow(() -> new CustomerNotFoundException(
                        "Customer not found with ID: " + customerId));

        customer.reserveCredit(amount);
        customerRepository.save(customer);
    }

    @Override
    public void releaseCustomerCredit(Long customerId, BigDecimal amount) {
        Customer customer = customerRepository.findById(customerId)
                .orElseThrow(() -> new CustomerNotFoundException(
                        "Customer not found with ID: " + customerId));

        customer.releaseCredit(amount);
        customerRepository.save(customer);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isCustomerEligibleForLoan(Long customerId, BigDecimal loanAmount) {
        Customer customer = customerRepository.findById(customerId)
                .orElseThrow(() -> new CustomerNotFoundException(
                        "Customer not found with ID: " + customerId));

        return customer.isEligibleForLoan(loanAmount);
    }

    @Override
    public void updateCustomerCreditScore(Long customerId, Integer newCreditScore) {
        Customer customer = customerRepository.findById(customerId)
                .orElseThrow(() -> new CustomerNotFoundException(
                        "Customer not found with ID: " + customerId));

        customer.updateCreditScore(newCreditScore);
        customerRepository.save(customer);
    }

    @Override
    public void deactivateCustomer(Long customerId) {
        Customer customer = customerRepository.findById(customerId)
                .orElseThrow(() -> new CustomerNotFoundException(
                        "Customer not found with ID: " + customerId));

        // Note: You'd need to add a deactivate method to Customer domain model
        customerRepository.save(customer);
    }

    @Override
    public void reactivateCustomer(Long customerId) {
        Customer customer = customerRepository.findById(customerId)
                .orElseThrow(() -> new CustomerNotFoundException(
                        "Customer not found with ID: " + customerId));

        // Note: You'd need to add a reactivate method to Customer domain model
        customerRepository.save(customer);
    }
}