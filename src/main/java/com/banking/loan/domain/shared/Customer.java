package com.banking.loan.domain.shared;

import com.banking.loan.domain.valueobjects.CreditScore;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Customer Entity in the Banking Domain
 * Follows DDD Entity pattern with identity and business logic
 */
public class Customer {
    
    private final String customerId;
    private String firstName;
    private String lastName;
    private String email;
    private String phoneNumber;
    private LocalDate dateOfBirth;
    private String nationalId;
    private BigDecimal monthlyIncome;
    private String kycStatus;
    private boolean islamicBankingPreference;
    private CreditScore creditScore;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    
    // Constructor
    public Customer(String customerId, String firstName, String lastName, String email) {
        this.customerId = customerId;
        this.firstName = firstName;
        this.lastName = lastName;
        this.email = email;
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
        this.kycStatus = "PENDING";
    }
    
    // Factory method following DDD patterns
    public static Customer create(String firstName, String lastName, String email) {
        String customerId = "CUST-" + System.currentTimeMillis();
        return new Customer(customerId, firstName, lastName, email);
    }
    
    // Business logic methods
    public boolean isEligibleForLoan(BigDecimal loanAmount) {
        if (!"VERIFIED".equals(kycStatus)) {
            return false;
        }
        
        if (monthlyIncome == null) {
            return false;
        }
        
        // Business rule: Maximum loan is 10x monthly income
        BigDecimal maxLoanAmount = monthlyIncome.multiply(BigDecimal.valueOf(10));
        return loanAmount.compareTo(maxLoanAmount) <= 0;
    }
    
    public void completeKYC(String verificationResult) {
        this.kycStatus = verificationResult;
        this.updatedAt = LocalDateTime.now();
    }
    
    public void updateContactInfo(String email, String phoneNumber) {
        this.email = email;
        this.phoneNumber = phoneNumber;
        this.updatedAt = LocalDateTime.now();
    }
    
    // Getters
    public String getCustomerId() { return customerId; }
    public String getFirstName() { return firstName; }
    public String getLastName() { return lastName; }
    public String getEmail() { return email; }
    public String getPhoneNumber() { return phoneNumber; }
    public LocalDate getDateOfBirth() { return dateOfBirth; }
    public String getNationalId() { return nationalId; }
    public BigDecimal getMonthlyIncome() { return monthlyIncome; }
    public String getKycStatus() { return kycStatus; }
    public boolean isIslamicBankingPreference() { return islamicBankingPreference; }
    public CreditScore getCreditScore() { return creditScore; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    
    // Setters for mutable properties
    public void setMonthlyIncome(BigDecimal monthlyIncome) {
        this.monthlyIncome = monthlyIncome;
        this.updatedAt = LocalDateTime.now();
    }
    
    public void setIslamicBankingPreference(boolean islamicBankingPreference) {
        this.islamicBankingPreference = islamicBankingPreference;
        this.updatedAt = LocalDateTime.now();
    }
    
    public void setDateOfBirth(LocalDate dateOfBirth) {
        this.dateOfBirth = dateOfBirth;
        this.updatedAt = LocalDateTime.now();
    }
    
    public void setNationalId(String nationalId) {
        this.nationalId = nationalId;
        this.updatedAt = LocalDateTime.now();
    }
    
    public void setCreditScore(CreditScore creditScore) {
        this.creditScore = creditScore;
        this.updatedAt = LocalDateTime.now();
    }
}