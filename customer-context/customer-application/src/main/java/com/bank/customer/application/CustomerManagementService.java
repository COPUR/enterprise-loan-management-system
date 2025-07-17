package com.bank.customer.application;

import com.bank.customer.application.dto.CreateCustomerRequest;
import com.bank.customer.application.dto.CreateCustomerRequestWithCreditScore;
import com.bank.customer.application.dto.CustomerResponse;
import com.bank.customer.domain.Customer;
import com.bank.customer.domain.CustomerRepository;
import com.bank.shared.kernel.domain.CustomerId;
import com.bank.shared.kernel.domain.Money;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Application Service for Customer Management
 * 
 * Implements functional requirements:
 * - FR-001: Customer Registration
 * - FR-002: Customer Profile Management
 * - FR-003: Credit Limit Management
 * - FR-004: Customer Lookup & Search
 */
@Service
@Transactional
public class CustomerManagementService {
    
    private final CustomerRepository customerRepository;
    
    public CustomerManagementService(CustomerRepository customerRepository) {
        this.customerRepository = customerRepository;
    }
    
    /**
     * FR-001: Create a new customer
     */
    public CustomerResponse createCustomer(CreateCustomerRequest request) {
        // Validate request
        request.validate();
        
        // Check for duplicate email
        if (customerRepository.existsByEmail(request.email())) {
            throw new IllegalArgumentException("Customer with email " + request.email() + " already exists");
        }
        
        // Create customer
        Customer customer = Customer.create(
            CustomerId.generate(),
            request.firstName(),
            request.lastName(),
            request.email(),
            request.phoneNumber(),
            request.getCreditLimitAsMoney()
        );
        
        // Save customer
        Customer savedCustomer = customerRepository.save(customer);
        
        return CustomerResponse.from(savedCustomer);
    }
    
    /**
     * FR-002: Find customer by ID
     */
    @Transactional(readOnly = true)
    public CustomerResponse findCustomerById(String customerId) {
        CustomerId id = CustomerId.of(customerId);
        Customer customer = customerRepository.findById(id)
            .orElseThrow(() -> CustomerNotFoundException.withId(customerId));
        
        return CustomerResponse.from(customer);
    }
    
    /**
     * FR-003: Update customer credit limit
     */
    public CustomerResponse updateCreditLimit(String customerId, Money newCreditLimit) {
        CustomerId id = CustomerId.of(customerId);
        Customer customer = customerRepository.findById(id)
            .orElseThrow(() -> CustomerNotFoundException.withId(customerId));
        
        customer.updateCreditLimit(newCreditLimit);
        Customer savedCustomer = customerRepository.save(customer);
        
        return CustomerResponse.from(savedCustomer);
    }
    
    /**
     * FR-003: Reserve credit for a customer
     */
    public CustomerResponse reserveCredit(String customerId, Money amount) {
        CustomerId id = CustomerId.of(customerId);
        Customer customer = customerRepository.findById(id)
            .orElseThrow(() -> CustomerNotFoundException.withId(customerId));
        
        customer.reserveCredit(amount);
        Customer savedCustomer = customerRepository.save(customer);
        
        return CustomerResponse.from(savedCustomer);
    }
    
    /**
     * FR-003: Release reserved credit for a customer
     */
    public CustomerResponse releaseCredit(String customerId, Money amount) {
        CustomerId id = CustomerId.of(customerId);
        Customer customer = customerRepository.findById(id)
            .orElseThrow(() -> CustomerNotFoundException.withId(customerId));
        
        customer.releaseCredit(amount);
        Customer savedCustomer = customerRepository.save(customer);
        
        return CustomerResponse.from(savedCustomer);
    }
    
    /**
     * Archive Business Logic: Create customer with credit score and monthly income
     */
    public CustomerResponse createCustomerWithCreditScore(CreateCustomerRequestWithCreditScore request) {
        // Validate request
        request.validate();
        
        // Check for duplicate email
        if (customerRepository.existsByEmail(request.email())) {
            throw new IllegalArgumentException("Customer with email " + request.email() + " already exists");
        }
        
        // Create customer with credit score
        Customer customer = Customer.createWithCreditScore(
            CustomerId.generate(),
            request.firstName(),
            request.lastName(),
            request.email(),
            request.phoneNumber(),
            request.monthlyIncome(),
            request.creditScore()
        );
        
        // Save customer
        Customer savedCustomer = customerRepository.save(customer);
        
        return CustomerResponse.from(savedCustomer);
    }
    
    /**
     * Archive Business Logic: Check loan eligibility based on credit score
     */
    @Transactional(readOnly = true)
    public boolean isEligibleForLoan(String customerId, Money loanAmount) {
        CustomerId id = CustomerId.of(customerId);
        Customer customer = customerRepository.findById(id)
            .orElseThrow(() -> CustomerNotFoundException.withId(customerId));
        
        return customer.isEligibleForLoan(loanAmount);
    }
    
    /**
     * Archive Business Logic: Update customer credit score
     */
    public CustomerResponse updateCreditScore(String customerId, Integer newCreditScore) {
        CustomerId id = CustomerId.of(customerId);
        Customer customer = customerRepository.findById(id)
            .orElseThrow(() -> CustomerNotFoundException.withId(customerId));
        
        customer.updateCreditScore(newCreditScore);
        Customer savedCustomer = customerRepository.save(customer);
        
        return CustomerResponse.from(savedCustomer);
    }
    
    /**
     * Archive Business Logic: Find customer by email
     */
    @Transactional(readOnly = true)
    public CustomerResponse findCustomerByEmail(String email) {
        Customer customer = customerRepository.findByEmail(email)
            .orElseThrow(() -> new CustomerNotFoundException("Customer not found with email: " + email));
        
        return CustomerResponse.from(customer);
    }
    
    /**
     * Archive Business Logic: Update customer contact information
     */
    public CustomerResponse updateContactInformation(String customerId, String newEmail, String newPhoneNumber) {
        CustomerId id = CustomerId.of(customerId);
        Customer customer = customerRepository.findById(id)
            .orElseThrow(() -> CustomerNotFoundException.withId(customerId));
        
        customer.updateContactInformation(newEmail, newPhoneNumber);
        Customer savedCustomer = customerRepository.save(customer);
        
        return CustomerResponse.from(savedCustomer);
    }
}