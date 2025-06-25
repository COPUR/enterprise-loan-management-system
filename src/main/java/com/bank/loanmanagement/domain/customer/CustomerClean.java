package com.bank.loanmanagement.domain.customer;

import com.bank.loanmanagement.domain.shared.AggregateRoot;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

/**
 * Clean Domain Model for Customer - No Infrastructure Dependencies
 * This represents the proper hexagonal architecture implementation
 */
public class CustomerClean extends AggregateRoot<CustomerId> {
    
    private CustomerId id;
    private String firstName;
    private String lastName;
    private String email;
    private String phoneNumber;
    private String ssn;
    private LocalDate dateOfBirth;
    private CustomerStatus status;
    private CustomerType type;
    private Set<AddressClean> addresses;
    private CreditScore creditScore;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private Long version;
    
    // Constructor
    public CustomerClean(CustomerId id, String firstName, String lastName, 
                        String email, String phoneNumber, CustomerType type) {
        this.id = id;
        this.firstName = firstName;
        this.lastName = lastName;
        this.email = email;
        this.phoneNumber = phoneNumber;
        this.type = type;
        this.status = CustomerStatus.PENDING;
        this.addresses = new HashSet<>();
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
        this.version = 0L;
    }
    
    // Default constructor for frameworks
    protected CustomerClean() {
        this.addresses = new HashSet<>();
    }
    
    // Business Logic Methods
    public void addAddress(AddressClean address) {
        if (address == null) {
            throw new IllegalArgumentException("Address cannot be null");
        }
        addresses.add(address);
    }
    
    public void removeAddress(AddressClean address) {
        if (address == null) {
            throw new IllegalArgumentException("Address cannot be null");
        }
        addresses.remove(address);
    }
    
    public void updateCreditScore(int score, String reportingAgency) {
        if (score < 300 || score > 850) {
            throw new IllegalArgumentException("Credit score must be between 300 and 850");
        }
        if (reportingAgency == null || reportingAgency.trim().isEmpty()) {
            throw new IllegalArgumentException("Reporting agency cannot be null or empty");
        }
        
        this.creditScore = new CreditScore(score, reportingAgency, LocalDateTime.now());
        this.updatedAt = LocalDateTime.now();
        addDomainEvent(new CustomerCreditScoreUpdatedEvent(this.id.getValue(), score));
    }
    
    public void activate() {
        if (this.status != CustomerStatus.PENDING) {
            throw new IllegalStateException("Customer can only be activated from PENDING status");
        }
        this.status = CustomerStatus.ACTIVE;
        this.updatedAt = LocalDateTime.now();
        addDomainEvent(new CustomerActivatedEvent(this.id.getValue()));
    }
    
    public void suspend(String reason) {
        if (this.status != CustomerStatus.ACTIVE) {
            throw new IllegalStateException("Only active customers can be suspended");
        }
        if (reason == null || reason.trim().isEmpty()) {
            throw new IllegalArgumentException("Suspension reason cannot be null or empty");
        }
        
        this.status = CustomerStatus.SUSPENDED;
        this.updatedAt = LocalDateTime.now();
        addDomainEvent(new CustomerSuspendedEvent(this.id.getValue(), reason));
    }
    
    public void close(String reason) {
        if (reason == null || reason.trim().isEmpty()) {
            throw new IllegalArgumentException("Closure reason cannot be null or empty");
        }
        
        this.status = CustomerStatus.CLOSED;
        this.updatedAt = LocalDateTime.now();
        addDomainEvent(new CustomerClosedEvent(this.id.getValue(), reason));
    }
    
    public boolean isEligibleForLoan() {
        return status == CustomerStatus.ACTIVE && 
               creditScore != null && 
               creditScore.getScore() >= 600;
    }
    
    public boolean canReceiveCredit(int requestedAmount) {
        if (!isEligibleForLoan()) {
            return false;
        }
        
        // Business rule: Credit limit based on credit score
        int maxCredit = calculateMaxCreditLimit();
        return requestedAmount <= maxCredit;
    }
    
    private int calculateMaxCreditLimit() {
        if (creditScore == null) {
            return 0;
        }
        
        int score = creditScore.getScore();
        if (score >= 750) return 500000;      // Excellent credit
        if (score >= 700) return 300000;      // Good credit
        if (score >= 650) return 150000;      // Fair credit
        if (score >= 600) return 50000;       // Poor credit
        return 0;                             // Below minimum
    }
    
    public String getFullName() {
        return firstName + " " + lastName;
    }
    
    public int getCurrentCreditScore() {
        return creditScore != null ? creditScore.getScore() : 0;
    }
    
    public boolean hasValidContactInfo() {
        return email != null && !email.trim().isEmpty() &&
               phoneNumber != null && !phoneNumber.trim().isEmpty();
    }
    
    // Getters
    @Override
    public CustomerId getId() {
        return id;
    }
    
    public String getFirstName() {
        return firstName;
    }
    
    public String getLastName() {
        return lastName;
    }
    
    public String getEmail() {
        return email;
    }
    
    public String getPhoneNumber() {
        return phoneNumber;
    }
    
    public String getSsn() {
        return ssn;
    }
    
    public LocalDate getDateOfBirth() {
        return dateOfBirth;
    }
    
    public CustomerStatus getStatus() {
        return status;
    }
    
    public CustomerType getType() {
        return type;
    }
    
    public Set<AddressClean> getAddresses() {
        return new HashSet<>(addresses); // Defensive copy
    }
    
    public CreditScore getCreditScore() {
        return creditScore;
    }
    
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
    
    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
    
    public Long getVersion() {
        return version;
    }
    
    // Setters for reconstruction from persistence
    public void setSsn(String ssn) {
        this.ssn = ssn;
    }
    
    public void setDateOfBirth(LocalDate dateOfBirth) {
        this.dateOfBirth = dateOfBirth;
    }
    
    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
    
    public void setVersion(Long version) {
        this.version = version;
    }
}