package com.bank.loanmanagement.loan.domain.shared;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Objects;

/**
 * Customer Profile Value Object
 * 
 * Represents customer information needed within the Loan Management domain.
 * This is a domain concept independent of external Customer Management system.
 * 
 * Architecture Compliance:
 * ✅ DDD: Value object with immutable design
 * ✅ Clean Code: Intention-revealing names and business methods
 * ✅ Type Safety: Strong typing with business validation
 * ✅ Hexagonal Architecture: Domain concept independent of external systems
 */
@Getter
@Builder
public class CustomerProfile {
    
    private final Long customerId;
    private final String firstName;
    private final String lastName;
    private final String email;
    private final String phone;
    private final BigDecimal monthlyIncome;
    private final EmploymentStatus employmentStatus;
    private final Integer creditScore;
    private final LocalDate dateOfBirth;
    private final String address;
    private final boolean isActive;
    
    /**
     * Private constructor to enforce builder usage
     */
    private CustomerProfile(Long customerId, String firstName, String lastName, String email,
                           String phone, BigDecimal monthlyIncome, EmploymentStatus employmentStatus,
                           Integer creditScore, LocalDate dateOfBirth, String address, boolean isActive) {
        
        this.customerId = Objects.requireNonNull(customerId, "Customer ID is required");
        this.firstName = Objects.requireNonNull(firstName, "First name is required");
        this.lastName = Objects.requireNonNull(lastName, "Last name is required");
        this.email = email;
        this.phone = phone;
        this.monthlyIncome = monthlyIncome;
        this.employmentStatus = employmentStatus;
        this.creditScore = creditScore;
        this.dateOfBirth = dateOfBirth;
        this.address = address;
        this.isActive = isActive;
    }
    
    /**
     * Business method to get full name
     */
    public String getFullName() {
        return firstName + " " + lastName;
    }
    
    /**
     * Business method to check if customer has good credit
     */
    public boolean hasGoodCredit() {
        return creditScore != null && creditScore >= 700;
    }
    
    /**
     * Business method to check if customer has excellent credit
     */
    public boolean hasExcellentCredit() {
        return creditScore != null && creditScore >= 800;
    }
    
    /**
     * Business method to calculate age
     */
    public Integer getAge() {
        if (dateOfBirth == null) {
            return null;
        }
        return LocalDate.now().getYear() - dateOfBirth.getYear();
    }
    
    /**
     * Business method to check if customer is employed
     */
    public boolean isEmployed() {
        return employmentStatus == EmploymentStatus.EMPLOYED || 
               employmentStatus == EmploymentStatus.SELF_EMPLOYED;
    }
    
    /**
     * Business method to check if customer has stable income
     */
    public boolean hasStableIncome() {
        return monthlyIncome != null && 
               monthlyIncome.compareTo(BigDecimal.ZERO) > 0 &&
               isEmployed();
    }
    
    /**
     * Business method to check if customer qualifies for loan consideration
     */
    public boolean qualifiesForLoanConsideration() {
        return isActive && 
               hasStableIncome() &&
               creditScore != null &&
               creditScore >= 500; // Minimum credit score for consideration
    }
    
    /**
     * Business method to get credit risk level
     */
    public CreditRiskLevel getCreditRiskLevel() {
        if (creditScore == null) {
            return CreditRiskLevel.UNKNOWN;
        }
        
        if (creditScore >= 800) {
            return CreditRiskLevel.EXCELLENT;
        } else if (creditScore >= 740) {
            return CreditRiskLevel.VERY_GOOD;
        } else if (creditScore >= 670) {
            return CreditRiskLevel.GOOD;
        } else if (creditScore >= 580) {
            return CreditRiskLevel.FAIR;
        } else {
            return CreditRiskLevel.POOR;
        }
    }
    
    /**
     * Business method to calculate debt-to-income ratio if loan is approved
     */
    public BigDecimal calculateProjectedDebtToIncomeRatio(BigDecimal monthlyLoanPayment) {
        if (monthlyIncome == null || monthlyIncome.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        }
        
        return monthlyLoanPayment.divide(monthlyIncome, 4, BigDecimal.ROUND_HALF_UP);
    }
    
    /**
     * Business method to check if customer can afford monthly payment
     */
    public boolean canAffordMonthlyPayment(BigDecimal monthlyPayment) {
        if (!hasStableIncome()) {
            return false;
        }
        
        // Business rule: DTI should not exceed 43%
        BigDecimal dtiRatio = calculateProjectedDebtToIncomeRatio(monthlyPayment);
        return dtiRatio.compareTo(new BigDecimal("0.43")) <= 0;
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof CustomerProfile)) return false;
        CustomerProfile that = (CustomerProfile) o;
        return Objects.equals(customerId, that.customerId);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(customerId);
    }
    
    @Override
    public String toString() {
        return String.format("CustomerProfile{customerId=%d, fullName='%s', creditScore=%d, " +
                           "employmentStatus=%s, isActive=%s}",
                           customerId, getFullName(), creditScore, employmentStatus, isActive);
    }
    
    /**
     * Employment status enumeration
     */
    public enum EmploymentStatus {
        EMPLOYED("Employed"),
        SELF_EMPLOYED("Self-Employed"),
        UNEMPLOYED("Unemployed"),
        RETIRED("Retired"),
        STUDENT("Student"),
        OTHER("Other");
        
        private final String displayName;
        
        EmploymentStatus(String displayName) {
            this.displayName = displayName;
        }
        
        public String getDisplayName() {
            return displayName;
        }
    }
    
    /**
     * Credit risk level enumeration
     */
    public enum CreditRiskLevel {
        EXCELLENT("Excellent (800+)"),
        VERY_GOOD("Very Good (740-799)"),
        GOOD("Good (670-739)"),
        FAIR("Fair (580-669)"),
        POOR("Poor (300-579)"),
        UNKNOWN("Unknown");
        
        private final String displayName;
        
        CreditRiskLevel(String displayName) {
            this.displayName = displayName;
        }
        
        public String getDisplayName() {
            return displayName;
        }
    }
}