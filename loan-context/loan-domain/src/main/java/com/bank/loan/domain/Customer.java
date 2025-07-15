package com.bank.loan.domain;

import com.bank.shared.kernel.domain.CustomerId;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Customer interface for Loan Domain
 * 
 * This interface follows the Dependency Inversion Principle (DIP)
 * by defining what the loan domain needs from a customer without
 * depending on the concrete customer implementation.
 * 
 * GRASP Principles Applied:
 * - Low Coupling: Interface segregation for loan-specific needs
 * - Information Expert: Customer knows its own eligibility data
 * - Protected Variations: Shields loan domain from customer changes
 */
public interface Customer {
    
    /**
     * Get unique customer identifier
     */
    CustomerId getId();
    
    /**
     * Check if customer account is active
     */
    boolean isActive();
    
    /**
     * Get customer's credit score
     */
    Integer getCreditScore();
    
    /**
     * Get customer's date of birth for age calculation
     */
    LocalDate getDateOfBirth();
    
    /**
     * Get customer's monthly income
     */
    BigDecimal getMonthlyIncome();
    
    /**
     * Get customer's existing monthly debt obligations
     */
    BigDecimal getExistingMonthlyObligations();
    
    /**
     * Get customer's first name
     */
    String getFirstName();
    
    /**
     * Get customer's last name  
     */
    String getLastName();
    
    /**
     * Get customer's email address
     */
    String getEmail();
    
    /**
     * Get customer's phone number
     */
    String getPhoneNumber();
}