package com.bank.loanmanagement.customermanagement.application.service;

import com.bank.loanmanagement.customermanagement.domain.model.*;
import com.bank.loanmanagement.customermanagement.domain.port.in.*;
import com.bank.loanmanagement.customermanagement.domain.port.out.CustomerRepository;
import com.bank.loanmanagement.customermanagement.domain.port.out.DomainEventPublisher;
import com.bank.loanmanagement.sharedkernel.domain.Money;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Currency;
import java.util.List;
import java.util.Optional;

/**
 * Application service implementing customer management use cases.
 * Orchestrates domain logic and coordinates with infrastructure adapters.
 */
@Service
@Transactional
public class CustomerManagementService implements CustomerManagementUseCase {
    
    private static final Logger logger = LoggerFactory.getLogger(CustomerManagementService.class);
    
    private final CustomerRepository customerRepository;
    private final DomainEventPublisher eventPublisher;
    
    public CustomerManagementService(
        CustomerRepository customerRepository,
        DomainEventPublisher eventPublisher
    ) {
        this.customerRepository = customerRepository;
        this.eventPublisher = eventPublisher;
    }
    
    @Override
    public Customer createCustomer(CreateCustomerCommand command) {
        logger.info("Creating customer with email: {}", command.email());
        
        // 1. Validate business rules
        EmailAddress email = EmailAddress.of(command.email());
        if (customerRepository.existsByEmail(email)) {
            throw new CustomerAlreadyExistsException("Customer with email already exists: " + command.email());
        }
        
        // 2. Create domain objects
        CustomerId customerId = CustomerId.generate();
        PersonalName name = PersonalName.of(command.firstName(), command.lastName());
        PhoneNumber phoneNumber = PhoneNumber.of(command.phoneNumber());
        CreditLimit creditLimit = CreditLimit.ofUsd(command.initialCreditLimit());
        
        // 3. Create customer aggregate
        Customer customer = Customer.create(customerId, name, email, phoneNumber, creditLimit);
        
        // 4. Persist customer
        Customer savedCustomer = customerRepository.save(customer);
        
        // 5. Publish domain events
        publishDomainEvents(savedCustomer);
        
        logger.info("Successfully created customer with ID: {}", savedCustomer.getId());
        return savedCustomer;
    }
    
    @Override
    public Customer updateCustomer(UpdateCustomerCommand command) {
        logger.info("Updating customer with ID: {}", command.customerId());
        
        // 1. Find existing customer
        Customer customer = findCustomerById(command.customerId());
        
        // 2. Update fields if provided
        if (command.firstName().isPresent() || command.lastName().isPresent()) {
            String firstName = command.firstName().orElse(customer.getName().getFirstName());
            String lastName = command.lastName().orElse(customer.getName().getLastName());
            PersonalName newName = PersonalName.of(firstName, lastName);
            customer.setName(newName);
        }
        
        if (command.email().isPresent()) {
            EmailAddress newEmail = EmailAddress.of(command.email().get());
            // Check for duplicate email
            if (!customer.getEmail().equals(newEmail) && customerRepository.existsByEmail(newEmail)) {
                throw new CustomerAlreadyExistsException("Customer with email already exists: " + newEmail.getValue());
            }
            customer.setEmail(newEmail);
        }
        
        if (command.phoneNumber().isPresent()) {
            PhoneNumber newPhoneNumber = PhoneNumber.of(command.phoneNumber().get());
            customer.setPhoneNumber(newPhoneNumber);
        }
        
        // 3. Save updated customer
        Customer updatedCustomer = customerRepository.save(customer);
        
        // 4. Publish domain events
        publishDomainEvents(updatedCustomer);
        
        logger.info("Successfully updated customer with ID: {}", updatedCustomer.getId());
        return updatedCustomer;
    }
    
    @Override
    @Transactional(readOnly = true)
    public Optional<Customer> findCustomer(FindCustomerQuery query) {
        logger.debug("Finding customer with ID: {}", query.customerId());
        return customerRepository.findById(query.customerId());
    }
    
    @Override
    @Transactional(readOnly = true)
    public List<Customer> getAllCustomers(GetAllCustomersQuery query) {
        logger.debug("Getting all customers - page: {}, size: {}", query.page(), query.size());
        return customerRepository.findAll(query.page(), query.size(), query.sortBy(), query.sortDirection());
    }
    
    @Override
    public void activateCustomer(ActivateCustomerCommand command) {
        logger.info("Activating customer with ID: {}", command.customerId());
        
        // 1. Find customer
        Customer customer = findCustomerById(command.customerId());
        
        // 2. Activate customer (domain logic)
        customer.activate();
        
        // 3. Save customer
        Customer activatedCustomer = customerRepository.save(customer);
        
        // 4. Publish domain events
        publishDomainEvents(activatedCustomer);
        
        logger.info("Successfully activated customer with ID: {}", activatedCustomer.getId());
    }
    
    @Override
    public void suspendCustomer(SuspendCustomerCommand command) {
        logger.info("Suspending customer with ID: {} for reason: {}", 
            command.customerId(), command.reason());
        
        // 1. Find customer
        Customer customer = findCustomerById(command.customerId());
        
        // 2. Suspend customer (domain logic)
        customer.suspend(command.reason());
        
        // 3. Save customer
        Customer suspendedCustomer = customerRepository.save(customer);
        
        // 4. Publish domain events
        publishDomainEvents(suspendedCustomer);
        
        logger.info("Successfully suspended customer with ID: {}", suspendedCustomer.getId());
    }
    
    @Override
    public void reserveCredit(ReserveCreditCommand command) {
        logger.info("Reserving credit of {} for customer: {}", 
            command.amount(), command.customerId());
        
        // 1. Find customer
        Customer customer = findCustomerById(command.customerId());
        
        // 2. Reserve credit (domain logic with business rules)
        customer.reserveCredit(command.amount());
        
        // 3. Save customer
        Customer updatedCustomer = customerRepository.save(customer);
        
        // 4. Publish domain events
        publishDomainEvents(updatedCustomer);
        
        logger.info("Successfully reserved credit of {} for customer: {}", 
            command.amount(), command.customerId());
    }
    
    @Override
    public void releaseCredit(ReleaseCreditCommand command) {
        logger.info("Releasing credit of {} for customer: {}", 
            command.amount(), command.customerId());
        
        // 1. Find customer
        Customer customer = findCustomerById(command.customerId());
        
        // 2. Release credit (domain logic)
        customer.releaseCredit(command.amount());
        
        // 3. Save customer
        Customer updatedCustomer = customerRepository.save(customer);
        
        // 4. Publish domain events
        publishDomainEvents(updatedCustomer);
        
        logger.info("Successfully released credit of {} for customer: {}", 
            command.amount(), command.customerId());
    }
    
    @Override
    public void updateCreditLimit(UpdateCreditLimitCommand command) {
        logger.info("Updating credit limit to {} for customer: {}", 
            command.newCreditLimit(), command.customerId());
        
        // 1. Find customer
        Customer customer = findCustomerById(command.customerId());
        
        // 2. Update credit limit (domain logic)
        CreditLimit newCreditLimit = CreditLimit.of(command.newCreditLimit());
        customer.updateCreditLimit(newCreditLimit);
        
        // 3. Save customer
        Customer updatedCustomer = customerRepository.save(customer);
        
        // 4. Publish domain events
        publishDomainEvents(updatedCustomer);
        
        logger.info("Successfully updated credit limit for customer: {}", command.customerId());
    }
    
    @Override
    @Transactional(readOnly = true)
    public boolean checkLoanEligibility(CheckLoanEligibilityQuery query) {
        logger.debug("Checking loan eligibility for customer: {} with amount: {}", 
            query.customerId(), query.loanAmount());
        
        // 1. Find customer
        Customer customer = findCustomerById(query.customerId());
        
        // 2. Check eligibility (domain logic)
        boolean eligible = customer.isEligibleForLoan(query.loanAmount());
        
        logger.debug("Loan eligibility result for customer {}: {}", query.customerId(), eligible);
        return eligible;
    }
    
    // Helper methods
    private Customer findCustomerById(CustomerId customerId) {
        return customerRepository.findById(customerId)
            .orElseThrow(() -> new CustomerNotFoundException("Customer not found: " + customerId));
    }
    
    private void publishDomainEvents(Customer customer) {
        customer.getDomainEvents().forEach(eventPublisher::publish);
        customer.clearDomainEvents();
    }
}