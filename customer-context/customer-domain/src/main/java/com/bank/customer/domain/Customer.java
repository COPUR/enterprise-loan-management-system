package com.bank.customer.domain;

import com.bank.shared.kernel.domain.AggregateRoot;
import com.bank.shared.kernel.domain.CustomerId;
import com.bank.shared.kernel.domain.Money;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Customer Aggregate Root
 * 
 * Represents a banking customer with their personal information,
 * credit profile, and business rules for customer management.
 */
public class Customer extends AggregateRoot<CustomerId> {
    
    private static final int MIN_CREDIT_SCORE = 300;
    private static final int MAX_CREDIT_SCORE = 850;
    private static final Money MIN_MONTHLY_INCOME = Money.aed(new BigDecimal("1000.00"));
    private static final int MIN_CREDIT_SCORE_FOR_LOAN = 600;
    
    private CustomerId customerId;
    private String firstName;
    private String lastName;
    private String email;
    private String phoneNumber;
    private CreditProfile creditProfile;
    private Integer creditScore;
    private Money monthlyIncome;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    
    // Private constructor for JPA
    protected Customer() {}
    
    private Customer(CustomerId customerId, String firstName, String lastName, 
                    String email, String phoneNumber, CreditProfile creditProfile) {
        this.customerId = Objects.requireNonNull(customerId, "Customer ID cannot be null");
        this.firstName = Objects.requireNonNull(firstName, "First name cannot be null");
        this.lastName = Objects.requireNonNull(lastName, "Last name cannot be null");
        this.email = Objects.requireNonNull(email, "Email cannot be null");
        this.phoneNumber = phoneNumber;
        this.creditProfile = Objects.requireNonNull(creditProfile, "Credit profile cannot be null");
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
        
        // Domain event
        addDomainEvent(new CustomerCreatedEvent(customerId, firstName + " " + lastName));
    }
    
    private Customer(CustomerId customerId, String firstName, String lastName, 
                    String email, String phoneNumber, Money monthlyIncome, Integer creditScore) {
        this.customerId = Objects.requireNonNull(customerId, "Customer ID cannot be null");
        this.firstName = Objects.requireNonNull(firstName, "First name cannot be null");
        this.lastName = Objects.requireNonNull(lastName, "Last name cannot be null");
        this.email = Objects.requireNonNull(email, "Email cannot be null");
        this.phoneNumber = phoneNumber;
        this.monthlyIncome = validateMonthlyIncome(monthlyIncome);
        this.creditScore = validateCreditScore(creditScore);
        this.creditProfile = CreditProfile.create(calculateCreditLimit());
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
        
        // Domain event
        addDomainEvent(new CustomerCreatedEvent(customerId, firstName + " " + lastName));
    }
    
    public static Customer create(CustomerId customerId, String firstName, String lastName,
                                String email, String phoneNumber, Money creditLimit) {
        validateCustomerData(firstName, lastName, email);
        CreditProfile creditProfile = CreditProfile.create(creditLimit);
        return new Customer(customerId, firstName, lastName, email, phoneNumber, creditProfile);
    }
    
    public static Customer createWithCreditScore(CustomerId customerId, String firstName, String lastName,
                                               String email, String phoneNumber, Money monthlyIncome, Integer creditScore) {
        validateCustomerData(firstName, lastName, email);
        return new Customer(customerId, firstName, lastName, email, phoneNumber, monthlyIncome, creditScore);
    }
    
    private static void validateCustomerData(String firstName, String lastName, String email) {
        if (firstName == null || firstName.trim().isEmpty()) {
            throw new IllegalArgumentException("First name cannot be null or empty");
        }
        if (lastName == null || lastName.trim().isEmpty()) {
            throw new IllegalArgumentException("Last name cannot be null or empty");
        }
        if (email == null || !isValidEmail(email)) {
            throw new IllegalArgumentException("Email must be valid");
        }
    }
    
    private static boolean isValidEmail(String email) {
        if (email == null || email.trim().isEmpty()) {
            return false;
        }
        // Basic email validation: contains @ and has text before and after @
        int atIndex = email.indexOf('@');
        return atIndex > 0 && atIndex < email.length() - 1 && email.indexOf('@', atIndex + 1) == -1;
    }
    
    private Money validateMonthlyIncome(Money monthlyIncome) {
        if (monthlyIncome == null) {
            throw new IllegalArgumentException("Monthly income cannot be null");
        }
        if (monthlyIncome.compareTo(MIN_MONTHLY_INCOME) < 0) {
            throw new IllegalArgumentException(
                String.format("Monthly income must be at least %s", MIN_MONTHLY_INCOME));
        }
        return monthlyIncome;
    }
    
    private Integer validateCreditScore(Integer creditScore) {
        if (creditScore == null) {
            throw new IllegalArgumentException("Credit score cannot be null");
        }
        if (creditScore < MIN_CREDIT_SCORE || creditScore > MAX_CREDIT_SCORE) {
            throw new IllegalArgumentException(
                String.format("Credit score must be between %d and %d", MIN_CREDIT_SCORE, MAX_CREDIT_SCORE));
        }
        return creditScore;
    }
    
    private Money calculateCreditLimit() {
        if (monthlyIncome == null || creditScore == null) {
            return Money.zero(MIN_MONTHLY_INCOME.getCurrency());
        }
        
        // Business rule: Credit limit based on credit score and monthly income
        BigDecimal baseMultiplier = new BigDecimal("3");
        if (creditScore >= 750) {
            baseMultiplier = new BigDecimal("5");
        } else if (creditScore >= 650) {
            baseMultiplier = new BigDecimal("4");
        }
        
        return monthlyIncome.multiply(baseMultiplier);
    }
    
    @Override
    public CustomerId getId() {
        return customerId;
    }
    
    public String getFirstName() {
        return firstName;
    }
    
    public String getLastName() {
        return lastName;
    }
    
    public String getFullName() {
        return firstName + " " + lastName;
    }
    
    public String getEmail() {
        return email;
    }
    
    public String getPhoneNumber() {
        return phoneNumber;
    }
    
    public CreditProfile getCreditProfile() {
        return creditProfile;
    }
    
    public Integer getCreditScore() {
        return creditScore;
    }
    
    public Money getMonthlyIncome() {
        return monthlyIncome;
    }
    
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
    
    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
    
    public void updateContactInformation(String email, String phoneNumber) {
        if (email != null && isValidEmail(email)) {
            this.email = email;
        }
        if (phoneNumber != null && !phoneNumber.trim().isEmpty()) {
            this.phoneNumber = phoneNumber;
        }
        this.updatedAt = LocalDateTime.now();
        
        addDomainEvent(new CustomerContactUpdatedEvent(customerId, email, phoneNumber));
    }
    
    public void updateCreditLimit(Money newCreditLimit) {
        Money oldLimit = this.creditProfile.getCreditLimit();
        this.creditProfile = this.creditProfile.updateCreditLimit(newCreditLimit);
        this.updatedAt = LocalDateTime.now();
        
        addDomainEvent(new CustomerCreditLimitUpdatedEvent(customerId, oldLimit, newCreditLimit));
    }
    
    public boolean canBorrowAmount(Money amount) {
        return creditProfile.canBorrow(amount);
    }
    
    public void reserveCredit(Money amount) {
        if (!canBorrowAmount(amount)) {
            throw new InsufficientCreditException(
                String.format("Customer %s has insufficient credit. Requested: %s, Available: %s",
                    customerId, amount, creditProfile.getAvailableCredit()));
        }
        this.creditProfile = this.creditProfile.reserveCredit(amount);
        this.updatedAt = LocalDateTime.now();
        
        addDomainEvent(new CustomerCreditReservedEvent(customerId, amount));
    }
    
    public void releaseCredit(Money amount) {
        this.creditProfile = this.creditProfile.releaseCredit(amount);
        this.updatedAt = LocalDateTime.now();
        
        addDomainEvent(new CustomerCreditReleasedEvent(customerId, amount));
    }
    
    public void updateCreditScore(Integer newCreditScore) {
        this.creditScore = validateCreditScore(newCreditScore);
        this.creditProfile = CreditProfile.create(calculateCreditLimit(), this.creditProfile.getUsedCredit());
        this.updatedAt = LocalDateTime.now();
        
        addDomainEvent(new CustomerCreditScoreUpdatedEvent(customerId, newCreditScore));
    }
    
    public boolean isEligibleForLoan(Money loanAmount) {
        return creditScore != null && creditScore >= MIN_CREDIT_SCORE_FOR_LOAN && 
               canBorrowAmount(loanAmount);
    }
}