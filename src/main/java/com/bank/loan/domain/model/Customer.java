package com.bank.loan.domain.model;

import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Customer Domain Model (Clean Domain - No Infrastructure Dependencies)
 * 
 * Represents a banking customer with business logic for loan eligibility,
 * credit assessment, and customer lifecycle management.
 */
@Getter @Setter @NoArgsConstructor @AllArgsConstructor
@Builder
public class Customer {
    
    private CustomerId customerId;
    private String firstName;
    private String lastName;
    private String email;
    private String phone;
    private String address;
    private String city;
    private String postalCode;
    private String country;
    private String nationality;
    private String occupation;
    private String employerName;
    private LocalDate dateOfBirth;
    private Integer creditScore;
    private BigDecimal creditLimit;
    private BigDecimal monthlyIncome;
    private BigDecimal existingMonthlyObligations;
    private CustomerStatus status;
    private RiskCategory riskCategory;
    private CustomerType customerType;
    private EmploymentType employmentType;
    private String drivingLicense;
    private String businessLicense;
    private String businessName;
    private Integer yearsInBusiness;
    private String religionPreference;
    private Boolean islamicBankingPreference = false;
    private LocalDateTime registrationDate;
    private LocalDateTime lastUpdated;
    private String useCaseReference;
    
    /**
     * Critical Business Rule: Determine loan eligibility
     * This method contains core banking business logic that must be preserved
     */
    public boolean isEligibleForLoan(Money requestedAmount) {
        if (status != CustomerStatus.ACTIVE) {
            return false;
        }
        
        if (creditScore == null || creditScore < 600) {
            return false;
        }
        
        if (monthlyIncome == null || monthlyIncome.compareTo(BigDecimal.ZERO) <= 0) {
            return false;
        }
        
        // Debt-to-income ratio check (should not exceed 40%)
        BigDecimal totalObligations = existingMonthlyObligations != null ? 
            existingMonthlyObligations : BigDecimal.ZERO;
        
        // Calculate monthly payment for requested loan (assuming 5-year term at 5% APR)
        BigDecimal estimatedMonthlyPayment = calculateEstimatedMonthlyPayment(requestedAmount.getAmount());
        BigDecimal projectedTotalObligations = totalObligations.add(estimatedMonthlyPayment);
        
        BigDecimal debtToIncomeRatio = projectedTotalObligations.divide(monthlyIncome, 4, BigDecimal.ROUND_HALF_UP);
        
        return debtToIncomeRatio.compareTo(new BigDecimal("0.40")) <= 0;
    }
    
    /**
     * Update customer credit score - Financial Logic
     */
    public void updateCreditScore(Integer newCreditScore) {
        if (newCreditScore == null || newCreditScore < 300 || newCreditScore > 850) {
            throw new IllegalArgumentException("Credit score must be between 300 and 850");
        }
        
        Integer oldScore = this.creditScore;
        this.creditScore = newCreditScore;
        this.lastUpdated = LocalDateTime.now();
        
        // Update risk category based on new credit score
        updateRiskCategoryBasedOnCreditScore(newCreditScore);
        
        // Domain event could be published here
        // publishDomainEvent(new CustomerCreditScoreUpdatedEvent(customerId, oldScore, newCreditScore));
    }
    
    /**
     * Activate customer account - Business Logic
     */
    public void activate() {
        if (this.status == CustomerStatus.SUSPENDED || this.status == CustomerStatus.PENDING_VERIFICATION) {
            this.status = CustomerStatus.ACTIVE;
            this.lastUpdated = LocalDateTime.now();
        } else {
            throw new IllegalStateException("Cannot activate customer in current status: " + this.status);
        }
    }
    
    /**
     * Suspend customer account - Business Logic
     */
    public void suspend() {
        if (this.status == CustomerStatus.ACTIVE) {
            this.status = CustomerStatus.SUSPENDED;
            this.lastUpdated = LocalDateTime.now();
        } else {
            throw new IllegalStateException("Cannot suspend customer in current status: " + this.status);
        }
    }
    
    /**
     * Close customer account - Business Logic
     */
    public void close() {
        if (this.status == CustomerStatus.ACTIVE || this.status == CustomerStatus.SUSPENDED) {
            this.status = CustomerStatus.CLOSED;
            this.lastUpdated = LocalDateTime.now();
        } else {
            throw new IllegalStateException("Cannot close customer in current status: " + this.status);
        }
    }
    
    /**
     * Calculate age from date of birth
     */
    public Integer getAge() {
        if (dateOfBirth == null) return null;
        return LocalDate.now().getYear() - dateOfBirth.getYear();
    }
    
    /**
     * Check if customer prefers Islamic banking
     */
    public boolean isIslamicBankingCustomer() {
        return islamicBankingPreference != null && islamicBankingPreference;
    }
    
    /**
     * Get customer's full name
     */
    public String getFullName() {
        return firstName + " " + lastName;
    }
    
    /**
     * Check if customer is a business customer
     */
    public boolean isBusinessCustomer() {
        return customerType == CustomerType.BUSINESS;
    }
    
    /**
     * Calculate available credit limit
     */
    public BigDecimal getAvailableCreditLimit() {
        if (creditLimit == null) return BigDecimal.ZERO;
        
        // This would typically subtract current outstanding balances
        // For now, return the full credit limit
        return creditLimit;
    }
    
    // Private helper methods
    
    private BigDecimal calculateEstimatedMonthlyPayment(BigDecimal loanAmount) {
        if (loanAmount == null || loanAmount.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO;
        }
        
        // Simple calculation: 5-year term at 5% APR
        BigDecimal monthlyRate = new BigDecimal("0.05").divide(new BigDecimal("12"), 6, BigDecimal.ROUND_HALF_UP);
        int numberOfPayments = 60; // 5 years * 12 months
        
        // PMT formula: L[c(1 + c)^n]/[(1 + c)^n - 1]
        BigDecimal onePlusRate = BigDecimal.ONE.add(monthlyRate);
        BigDecimal onePlusRatePowerN = onePlusRate.pow(numberOfPayments);
        
        BigDecimal numerator = loanAmount.multiply(monthlyRate).multiply(onePlusRatePowerN);
        BigDecimal denominator = onePlusRatePowerN.subtract(BigDecimal.ONE);
        
        return numerator.divide(denominator, 2, BigDecimal.ROUND_HALF_UP);
    }
    
    private void updateRiskCategoryBasedOnCreditScore(Integer creditScore) {
        if (creditScore >= 750) {
            this.riskCategory = RiskCategory.LOW;
        } else if (creditScore >= 650) {
            this.riskCategory = RiskCategory.MEDIUM;
        } else {
            this.riskCategory = RiskCategory.HIGH;
        }
    }
    
    /**
     * Initialize timestamps for new customer
     */
    public void initializeTimestamps() {
        this.registrationDate = LocalDateTime.now();
        this.lastUpdated = LocalDateTime.now();
    }
    
    /**
     * Update last modified timestamp
     */
    public void markAsUpdated() {
        this.lastUpdated = LocalDateTime.now();
    }
}