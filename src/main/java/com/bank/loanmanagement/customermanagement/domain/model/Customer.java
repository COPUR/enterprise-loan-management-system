package com.bank.loanmanagement.customermanagement.domain.model;

import com.bank.loanmanagement.sharedkernel.domain.AggregateRoot;
import com.bank.loanmanagement.sharedkernel.domain.Money;
import com.bank.loanmanagement.customermanagement.domain.event.CustomerCreatedEvent;
import com.bank.loanmanagement.customermanagement.domain.event.CreditReservedEvent;
import com.bank.loanmanagement.customermanagement.domain.event.CreditReleasedEvent;
import com.bank.loanmanagement.customermanagement.domain.event.CustomerActivatedEvent;
import com.bank.loanmanagement.customermanagement.domain.event.CustomerSuspendedEvent;

import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Customer aggregate root - pure domain model without infrastructure dependencies.
 * Encapsulates all business rules for customer management and credit operations.
 */
public class Customer extends AggregateRoot<CustomerId> {
    
    private CustomerId customerId;
    private PersonalName name;
    private EmailAddress email;
    private PhoneNumber phoneNumber;
    private CreditLimit creditLimit;
    private Money usedCredit;
    private CustomerStatus status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    
    // Private constructor for domain creation
    private Customer(
        CustomerId customerId,
        PersonalName name,
        EmailAddress email,
        PhoneNumber phoneNumber,
        CreditLimit creditLimit
    ) {
        this.customerId = Objects.requireNonNull(customerId, "Customer ID cannot be null");
        this.name = Objects.requireNonNull(name, "Name cannot be null");
        this.email = Objects.requireNonNull(email, "Email cannot be null");
        this.phoneNumber = Objects.requireNonNull(phoneNumber, "Phone number cannot be null");
        this.creditLimit = Objects.requireNonNull(creditLimit, "Credit limit cannot be null");
        this.usedCredit = Money.zero(creditLimit.getAmount().getCurrency());
        this.status = CustomerStatus.PENDING;
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }
    
    // Factory method for creating new customers
    public static Customer create(
        CustomerId customerId,
        PersonalName name,
        EmailAddress email,
        PhoneNumber phoneNumber,
        CreditLimit creditLimit
    ) {
        Customer customer = new Customer(customerId, name, email, phoneNumber, creditLimit);
        customer.addDomainEvent(new CustomerCreatedEvent(
            customerId,
            name,
            email,
            creditLimit.getAmount()
        ));
        return customer;
    }
    
    // Business logic: Reserve credit for loan or transaction
    public void reserveCredit(Money amount) {
        Objects.requireNonNull(amount, "Amount cannot be null");
        
        if (status != CustomerStatus.ACTIVE) {
            throw new IllegalStateException("Cannot reserve credit for inactive customer: " + customerId);
        }
        
        if (!hasAvailableCredit(amount)) {
            throw new InsufficientCreditException(
                customerId, 
                amount, 
                getAvailableCredit()
            );
        }
        
        this.usedCredit = this.usedCredit.add(amount);
        this.updatedAt = LocalDateTime.now();
        
        addDomainEvent(new CreditReservedEvent(
            customerId,
            amount,
            getAvailableCredit()
        ));
    }
    
    // Business logic: Release previously reserved credit
    public void releaseCredit(Money amount) {
        Objects.requireNonNull(amount, "Amount cannot be null");
        
        if (amount.isGreaterThan(usedCredit)) {
            throw new IllegalArgumentException(
                String.format("Cannot release more credit (%s) than currently used (%s)", 
                    amount, usedCredit)
            );
        }
        
        this.usedCredit = this.usedCredit.subtract(amount);
        this.updatedAt = LocalDateTime.now();
        
        addDomainEvent(new CreditReleasedEvent(
            customerId,
            amount,
            getAvailableCredit()
        ));
    }
    
    // Business logic: Activate customer account
    public void activate() {
        if (status != CustomerStatus.PENDING) {
            throw new IllegalStateException(
                "Customer can only be activated from PENDING status. Current status: " + status
            );
        }
        
        this.status = CustomerStatus.ACTIVE;
        this.updatedAt = LocalDateTime.now();
        
        addDomainEvent(new CustomerActivatedEvent(customerId));
    }
    
    // Business logic: Suspend customer account
    public void suspend(String reason) {
        Objects.requireNonNull(reason, "Suspension reason cannot be null");
        
        if (status != CustomerStatus.ACTIVE) {
            throw new IllegalStateException("Only active customers can be suspended");
        }
        
        this.status = CustomerStatus.SUSPENDED;
        this.updatedAt = LocalDateTime.now();
        
        addDomainEvent(new CustomerSuspendedEvent(customerId, reason));
    }
    
    // Business logic: Check if customer is eligible for loan
    public boolean isEligibleForLoan(Money loanAmount) {
        return status == CustomerStatus.ACTIVE && 
               hasAvailableCredit(loanAmount) &&
               getAvailableCredit().isGreaterThanOrEqualTo(loanAmount);
    }
    
    // Business logic: Update credit limit
    public void updateCreditLimit(CreditLimit newCreditLimit) {
        Objects.requireNonNull(newCreditLimit, "Credit limit cannot be null");
        
        // Ensure new limit accommodates used credit
        if (newCreditLimit.getAmount().isLessThan(usedCredit)) {
            throw new IllegalArgumentException(
                String.format("New credit limit (%s) cannot be less than used credit (%s)",
                    newCreditLimit.getAmount(), usedCredit)
            );
        }
        
        this.creditLimit = newCreditLimit;
        this.updatedAt = LocalDateTime.now();
    }
    
    // Business logic: Check available credit
    private boolean hasAvailableCredit(Money amount) {
        return getAvailableCredit().isGreaterThanOrEqualTo(amount);
    }
    
    // Business logic: Calculate available credit
    public Money getAvailableCredit() {
        return creditLimit.getAmount().subtract(usedCredit);
    }
    
    // Getters
    @Override
    public CustomerId getId() {
        return customerId;
    }
    
    public PersonalName getName() {
        return name;
    }
    
    public EmailAddress getEmail() {
        return email;
    }
    
    public PhoneNumber getPhoneNumber() {
        return phoneNumber;
    }
    
    public CreditLimit getCreditLimit() {
        return creditLimit;
    }
    
    public Money getUsedCredit() {
        return usedCredit;
    }
    
    public CustomerStatus getStatus() {
        return status;
    }
    
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
    
    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
    
    // Package-private setters for reconstruction from persistence
    public void setCustomerId(CustomerId customerId) {
        this.customerId = customerId;
    }
    
    public void setName(PersonalName name) {
        this.name = name;
        this.updatedAt = LocalDateTime.now();
    }
    
    public void setEmail(EmailAddress email) {
        this.email = email;
        this.updatedAt = LocalDateTime.now();
    }
    
    public void setPhoneNumber(PhoneNumber phoneNumber) {
        this.phoneNumber = phoneNumber;
        this.updatedAt = LocalDateTime.now();
    }
    
    public void setCreditLimit(CreditLimit creditLimit) {
        this.creditLimit = creditLimit;
        this.updatedAt = LocalDateTime.now();
    }
    
    public void setUsedCredit(Money usedCredit) {
        this.usedCredit = usedCredit;
    }
    
    public void setStatus(CustomerStatus status) {
        this.status = status;
    }
    
    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
    
    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Customer customer = (Customer) o;
        return Objects.equals(customerId, customer.customerId);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(customerId);
    }
    
    @Override
    public String toString() {
        return "Customer{" +
                "customerId=" + customerId +
                ", name=" + name +
                ", email=" + email +
                ", status=" + status +
                '}';
    }
}