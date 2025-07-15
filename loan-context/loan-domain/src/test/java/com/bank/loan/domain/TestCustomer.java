package com.bank.loan.domain;

import com.bank.shared.kernel.domain.CustomerId;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Test implementation of Customer interface for unit testing
 * 
 * This test double provides a simple implementation of the Customer
 * interface for testing loan eligibility logic without external dependencies.
 * 
 * Builder Pattern: Provides fluent API for test setup
 */
public class TestCustomer implements Customer {
    
    private final CustomerId customerId;
    private final boolean active;
    private final Integer creditScore;
    private final LocalDate dateOfBirth;
    private final BigDecimal monthlyIncome;
    private final BigDecimal existingMonthlyObligations;
    private final String firstName;
    private final String lastName;
    private final String email;
    private final String phoneNumber;
    
    private TestCustomer(Builder builder) {
        this.customerId = builder.customerId;
        this.active = builder.active;
        this.creditScore = builder.creditScore;
        this.dateOfBirth = builder.dateOfBirth;
        this.monthlyIncome = builder.monthlyIncome;
        this.existingMonthlyObligations = builder.existingMonthlyObligations;
        this.firstName = builder.firstName;
        this.lastName = builder.lastName;
        this.email = builder.email;
        this.phoneNumber = builder.phoneNumber;
    }
    
    public static Builder builder() {
        return new Builder();
    }
    
    @Override
    public CustomerId getId() {
        return customerId;
    }
    
    @Override
    public boolean isActive() {
        return active;
    }
    
    @Override
    public Integer getCreditScore() {
        return creditScore;
    }
    
    @Override
    public LocalDate getDateOfBirth() {
        return dateOfBirth;
    }
    
    @Override
    public BigDecimal getMonthlyIncome() {
        return monthlyIncome;
    }
    
    @Override
    public BigDecimal getExistingMonthlyObligations() {
        return existingMonthlyObligations;
    }
    
    @Override
    public String getFirstName() {
        return firstName;
    }
    
    @Override
    public String getLastName() {
        return lastName;
    }
    
    @Override
    public String getEmail() {
        return email;
    }
    
    @Override
    public String getPhoneNumber() {
        return phoneNumber;
    }
    
    public static class Builder {
        private CustomerId customerId = CustomerId.generate();
        private boolean active = true;
        private Integer creditScore = 750;
        private LocalDate dateOfBirth = LocalDate.now().minusYears(35);
        private BigDecimal monthlyIncome = new BigDecimal("12000");
        private BigDecimal existingMonthlyObligations = new BigDecimal("2000");
        private String firstName = "Test";
        private String lastName = "Customer";
        private String email = "test@example.com";
        private String phoneNumber = "+971501234567";
        
        public Builder customerId(CustomerId customerId) {
            this.customerId = customerId;
            return this;
        }
        
        public Builder active(boolean active) {
            this.active = active;
            return this;
        }
        
        public Builder creditScore(Integer creditScore) {
            this.creditScore = creditScore;
            return this;
        }
        
        public Builder dateOfBirth(LocalDate dateOfBirth) {
            this.dateOfBirth = dateOfBirth;
            return this;
        }
        
        public Builder monthlyIncome(BigDecimal monthlyIncome) {
            this.monthlyIncome = monthlyIncome;
            return this;
        }
        
        public Builder existingMonthlyObligations(BigDecimal existingMonthlyObligations) {
            this.existingMonthlyObligations = existingMonthlyObligations;
            return this;
        }
        
        public Builder firstName(String firstName) {
            this.firstName = firstName;
            return this;
        }
        
        public Builder lastName(String lastName) {
            this.lastName = lastName;
            return this;
        }
        
        public Builder email(String email) {
            this.email = email;
            return this;
        }
        
        public Builder phoneNumber(String phoneNumber) {
            this.phoneNumber = phoneNumber;
            return this;
        }
        
        public TestCustomer build() {
            return new TestCustomer(this);
        }
    }
}