package com.loanmanagement.loan.domain.model;

import com.loanmanagement.shared.domain.Money;
import lombok.Builder;
import lombok.Value;

import java.util.Objects;

/**
 * Customer Profile Value Object
 * Contains customer information for loan eligibility assessment
 */
@Value
@Builder(toBuilder = true)
public class CustomerProfile {
    
    CustomerId customerId;
    int creditScore;
    Money monthlyIncome;
    Money monthlyDebtObligations;
    EmploymentType employmentType;
    int employmentDuration; // in months
    ResidencyStatus residencyStatus;
    int age;
    int bankingHistory; // in months
    
    // Additional profile information
    String firstName;
    String lastName;
    String ssn;
    String emailAddress;
    String phoneNumber;
    boolean isFirstTimeBorrower;
    
    public CustomerProfile(CustomerId customerId, int creditScore, Money monthlyIncome,
                          Money monthlyDebtObligations, EmploymentType employmentType,
                          int employmentDuration, ResidencyStatus residencyStatus, int age,
                          int bankingHistory, String firstName, String lastName, String ssn,
                          String emailAddress, String phoneNumber, boolean isFirstTimeBorrower) {
        
        // Validation
        Objects.requireNonNull(customerId, "Customer ID cannot be null");
        Objects.requireNonNull(monthlyIncome, "Monthly income cannot be null");
        Objects.requireNonNull(monthlyDebtObligations, "Monthly debt obligations cannot be null");
        Objects.requireNonNull(employmentType, "Employment type cannot be null");
        Objects.requireNonNull(residencyStatus, "Residency status cannot be null");
        
        if (creditScore < 300 || creditScore > 850) {
            throw new IllegalArgumentException("Credit score must be between 300 and 850");
        }
        
        if (age < 0 || age > 150) {
            throw new IllegalArgumentException("Age must be realistic");
        }
        
        if (employmentDuration < 0) {
            throw new IllegalArgumentException("Employment duration cannot be negative");
        }
        
        if (bankingHistory < 0) {
            throw new IllegalArgumentException("Banking history cannot be negative");
        }
        
        this.customerId = customerId;
        this.creditScore = creditScore;
        this.monthlyIncome = monthlyIncome;
        this.monthlyDebtObligations = monthlyDebtObligations;
        this.employmentType = employmentType;
        this.employmentDuration = employmentDuration;
        this.residencyStatus = residencyStatus;
        this.age = age;
        this.bankingHistory = bankingHistory;
        this.firstName = firstName;
        this.lastName = lastName;
        this.ssn = ssn;
        this.emailAddress = emailAddress;
        this.phoneNumber = phoneNumber;
        this.isFirstTimeBorrower = isFirstTimeBorrower;
    }
    
    /**
     * Calculate current debt-to-income ratio
     */
    public java.math.BigDecimal getCurrentDTIRatio() {
        if (monthlyIncome.getAmount().compareTo(java.math.BigDecimal.ZERO) == 0) {
            return java.math.BigDecimal.ZERO;
        }
        return monthlyDebtObligations.getAmount()
                .divide(monthlyIncome.getAmount(), 4, java.math.RoundingMode.HALF_UP);
    }
    
    /**
     * Get full name
     */
    public String getFullName() {
        if (firstName != null && lastName != null) {
            return firstName + " " + lastName;
        }
        return "Customer " + customerId.getValue();
    }
    
    /**
     * Check if customer has excellent credit
     */
    public boolean hasExcellentCredit() {
        return creditScore >= 750;
    }
    
    /**
     * Check if customer has good credit
     */
    public boolean hasGoodCredit() {
        return creditScore >= 670;
    }
    
    /**
     * Check if customer has stable employment
     */
    public boolean hasStableEmployment() {
        return employmentType == EmploymentType.FULL_TIME && employmentDuration >= 24;
    }
    
    /**
     * Check if customer is experienced with banking
     */
    public boolean isExperiencedBankingCustomer() {
        return bankingHistory >= 36; // 3+ years
    }
    
    /**
     * Get credit score category
     */
    public String getCreditScoreCategory() {
        if (creditScore >= 800) return "Excellent";
        if (creditScore >= 740) return "Very Good";
        if (creditScore >= 670) return "Good";
        if (creditScore >= 580) return "Fair";
        return "Poor";
    }
    
    /**
     * Get employment stability rating
     */
    public String getEmploymentStabilityRating() {
        return switch (employmentType) {
            case FULL_TIME -> {
                if (employmentDuration >= 60) yield "Very Stable";
                if (employmentDuration >= 24) yield "Stable";
                if (employmentDuration >= 12) yield "Moderately Stable";
                yield "Less Stable";
            }
            case PART_TIME, CONTRACT -> "Moderate";
            case SELF_EMPLOYED -> employmentDuration >= 24 ? "Stable" : "Variable";
            case GOVERNMENT -> "Very Stable";
            case UNEMPLOYED -> "Unstable";
        };
    }
}