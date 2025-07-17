package com.bank.customer.application.dto;

import com.bank.shared.kernel.domain.Money;

import java.math.BigDecimal;

/**
 * Request DTO for creating a customer with credit score and monthly income
 * Based on archive business logic requirements
 */
public record CreateCustomerRequestWithCreditScore(
    String firstName,
    String lastName,
    String email,
    String phoneNumber,
    Money monthlyIncome,
    Integer creditScore
) {
    
    public void validate() {
        if (firstName == null || firstName.trim().isEmpty()) {
            throw new IllegalArgumentException("First name cannot be null or empty");
        }
        if (lastName == null || lastName.trim().isEmpty()) {
            throw new IllegalArgumentException("Last name cannot be null or empty");
        }
        if (email == null || email.trim().isEmpty()) {
            throw new IllegalArgumentException("Email cannot be null or empty");
        }
        if (phoneNumber == null || phoneNumber.trim().isEmpty()) {
            throw new IllegalArgumentException("Phone number cannot be null or empty");
        }
        if (monthlyIncome == null || monthlyIncome.isNegative() || monthlyIncome.isZero()) {
            throw new IllegalArgumentException("Monthly income must be positive");
        }
        if (creditScore == null || creditScore < 300 || creditScore > 850) {
            throw new IllegalArgumentException("Credit score must be between 300 and 850");
        }
    }
}