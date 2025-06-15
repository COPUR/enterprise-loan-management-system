
package com.bank.loanmanagement.domain.port.in;

import java.math.BigDecimal;

/**
 * Command object for creating a new customer.
 * Encapsulates all data required for customer creation.
 */
public class CreateCustomerCommand {
    
    private final String firstName;
    private final String lastName;
    private final String email;
    private final String phoneNumber;
    private final Integer creditScore;
    private final BigDecimal monthlyIncome;
    
    public CreateCustomerCommand(String firstName, String lastName, String email, 
                               String phoneNumber, Integer creditScore, BigDecimal monthlyIncome) {
        this.firstName = firstName;
        this.lastName = lastName;
        this.email = email;
        this.phoneNumber = phoneNumber;
        this.creditScore = creditScore;
        this.monthlyIncome = monthlyIncome;
    }
    
    // Getters
    public String getFirstName() { return firstName; }
    public String getLastName() { return lastName; }
    public String getEmail() { return email; }
    public String getPhoneNumber() { return phoneNumber; }
    public Integer getCreditScore() { return creditScore; }
    public BigDecimal getMonthlyIncome() { return monthlyIncome; }
    
    @Override
    public String toString() {
        return String.format("CreateCustomerCommand{firstName='%s', lastName='%s', email='%s'}", 
                           firstName, lastName, email);
    }
}
