
package com.bank.loanmanagement.domain.model;

import com.bank.loanmanagement.sharedkernel.domain.AggregateRoot;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Customer aggregate root representing a bank customer entity.
 * Encapsulates customer business rules and invariants.
 */
public class Customer extends AggregateRoot {
    
    private static final int MIN_CREDIT_SCORE = 300;
    private static final int MAX_CREDIT_SCORE = 850;
    private static final BigDecimal MIN_MONTHLY_INCOME = new BigDecimal("1000.00");
    
    private Long customerId;
    private String firstName;
    private String lastName;
    private String email;
    private String phoneNumber;
    private Integer creditScore;
    private BigDecimal monthlyIncome;
    private BigDecimal creditLimit;
    private BigDecimal usedCreditLimit;
    private CustomerStatus status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    
    // Default constructor for JPA
    protected Customer() {
        this.status = CustomerStatus.ACTIVE;
        this.createdAt = LocalDateTime.now();
        this.usedCreditLimit = BigDecimal.ZERO;
    }
    
    // Business constructor
    public Customer(String firstName, String lastName, String email, 
                   String phoneNumber, Integer creditScore, BigDecimal monthlyIncome) {
        this();
        this.firstName = validateAndSetFirstName(firstName);
        this.lastName = validateAndSetLastName(lastName);
        this.email = validateAndSetEmail(email);
        this.phoneNumber = validateAndSetPhoneNumber(phoneNumber);
        this.creditScore = validateAndSetCreditScore(creditScore);
        this.monthlyIncome = validateAndSetMonthlyIncome(monthlyIncome);
        this.creditLimit = calculateInitialCreditLimit();
    }
    
    // Business methods
    public void reserveCredit(BigDecimal amount) {
        if (amount.compareTo(getAvailableCredit()) > 0) {
            throw new IllegalStateException("Insufficient credit available");
        }
        this.usedCreditLimit = this.usedCreditLimit.add(amount);
        this.updatedAt = LocalDateTime.now();
    }
    
    public void releaseCredit(BigDecimal amount) {
        if (amount.compareTo(this.usedCreditLimit) > 0) {
            throw new IllegalStateException("Cannot release more credit than used");
        }
        this.usedCreditLimit = this.usedCreditLimit.subtract(amount);
        this.updatedAt = LocalDateTime.now();
    }
    
    public BigDecimal getAvailableCredit() {
        return this.creditLimit.subtract(this.usedCreditLimit);
    }
    
    public void updateCreditScore(Integer newCreditScore) {
        this.creditScore = validateAndSetCreditScore(newCreditScore);
        this.creditLimit = calculateInitialCreditLimit();
        this.updatedAt = LocalDateTime.now();
    }
    
    public boolean isEligibleForLoan(BigDecimal loanAmount) {
        return this.status == CustomerStatus.ACTIVE && 
               loanAmount.compareTo(getAvailableCredit()) <= 0 &&
               this.creditScore >= 600;
    }
    
    // Validation methods
    private String validateAndSetFirstName(String firstName) {
        if (firstName == null || firstName.trim().isEmpty()) {
            throw new IllegalArgumentException("First name cannot be null or empty");
        }
        if (firstName.length() > 50) {
            throw new IllegalArgumentException("First name cannot exceed 50 characters");
        }
        return firstName.trim();
    }
    
    private String validateAndSetLastName(String lastName) {
        if (lastName == null || lastName.trim().isEmpty()) {
            throw new IllegalArgumentException("Last name cannot be null or empty");
        }
        if (lastName.length() > 50) {
            throw new IllegalArgumentException("Last name cannot exceed 50 characters");
        }
        return lastName.trim();
    }
    
    private String validateAndSetEmail(String email) {
        if (email == null || email.trim().isEmpty()) {
            throw new IllegalArgumentException("Email cannot be null or empty");
        }
        if (!email.matches("^[A-Za-z0-9+_.-]+@([A-Za-z0-9.-]+\\.[A-Za-z]{2,})$")) {
            throw new IllegalArgumentException("Invalid email format");
        }
        return email.trim().toLowerCase();
    }
    
    private String validateAndSetPhoneNumber(String phoneNumber) {
        if (phoneNumber == null || phoneNumber.trim().isEmpty()) {
            throw new IllegalArgumentException("Phone number cannot be null or empty");
        }
        String cleanPhone = phoneNumber.replaceAll("[^0-9]", "");
        if (cleanPhone.length() < 10 || cleanPhone.length() > 15) {
            throw new IllegalArgumentException("Phone number must be between 10 and 15 digits");
        }
        return cleanPhone;
    }
    
    private Integer validateAndSetCreditScore(Integer creditScore) {
        if (creditScore == null) {
            throw new IllegalArgumentException("Credit score cannot be null");
        }
        if (creditScore < MIN_CREDIT_SCORE || creditScore > MAX_CREDIT_SCORE) {
            throw new IllegalArgumentException(
                String.format("Credit score must be between %d and %d", MIN_CREDIT_SCORE, MAX_CREDIT_SCORE));
        }
        return creditScore;
    }
    
    private BigDecimal validateAndSetMonthlyIncome(BigDecimal monthlyIncome) {
        if (monthlyIncome == null) {
            throw new IllegalArgumentException("Monthly income cannot be null");
        }
        if (monthlyIncome.compareTo(MIN_MONTHLY_INCOME) < 0) {
            throw new IllegalArgumentException(
                String.format("Monthly income must be at least %s", MIN_MONTHLY_INCOME));
        }
        return monthlyIncome;
    }
    
    private BigDecimal calculateInitialCreditLimit() {
        // Business rule: Credit limit based on credit score and monthly income
        BigDecimal baseMultiplier = BigDecimal.valueOf(3);
        if (this.creditScore >= 750) {
            baseMultiplier = BigDecimal.valueOf(5);
        } else if (this.creditScore >= 650) {
            baseMultiplier = BigDecimal.valueOf(4);
        }
        return this.monthlyIncome.multiply(baseMultiplier);
    }
    
    // Getters
    public Long getCustomerId() { return customerId; }
    public String getFirstName() { return firstName; }
    public String getLastName() { return lastName; }
    public String getEmail() { return email; }
    public String getPhoneNumber() { return phoneNumber; }
    public Integer getCreditScore() { return creditScore; }
    public BigDecimal getMonthlyIncome() { return monthlyIncome; }
    public BigDecimal getCreditLimit() { return creditLimit; }
    public BigDecimal getUsedCreditLimit() { return usedCreditLimit; }
    public CustomerStatus getStatus() { return status; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    
    public String getFullName() {
        return firstName + " " + lastName;
    }
    
    // Package-private setters for JPA
    void setCustomerId(Long customerId) { this.customerId = customerId; }
    void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
    void setStatus(CustomerStatus status) { this.status = status; }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Customer)) return false;
        Customer customer = (Customer) o;
        return Objects.equals(customerId, customer.customerId);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(customerId);
    }
    
    @Override
    public String toString() {
        return String.format("Customer{id=%d, name='%s', email='%s', status=%s}", 
                           customerId, getFullName(), email, status);
    }
}
