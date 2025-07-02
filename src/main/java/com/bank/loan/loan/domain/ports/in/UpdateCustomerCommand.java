
package com.bank.loanmanagement.loan.domain.port.in;

import java.math.BigDecimal;

/**
 * Command object for updating an existing customer.
 * Encapsulates all data required for customer updates.
 */
public class UpdateCustomerCommand {
    
    private final Long customerId;
    private final String firstName;
    private final String lastName;
    private final String email;
    private final String phoneNumber;
    private final BigDecimal monthlyIncome;
    
    public UpdateCustomerCommand(Long customerId, String firstName, String lastName, 
                               String email, String phoneNumber, BigDecimal monthlyIncome) {
        this.customerId = customerId;
        this.firstName = firstName;
        this.lastName = lastName;
        this.email = email;
        this.phoneNumber = phoneNumber;
        this.monthlyIncome = monthlyIncome;
    }
    
    // Getters
    public Long getCustomerId() { return customerId; }
    public String getFirstName() { return firstName; }
    public String getLastName() { return lastName; }
    public String getEmail() { return email; }
    public String getPhoneNumber() { return phoneNumber; }
    public BigDecimal getMonthlyIncome() { return monthlyIncome; }
    
    @Override
    public String toString() {
        return String.format("UpdateCustomerCommand{customerId=%d, firstName='%s', lastName='%s'}", 
                           customerId, firstName, lastName);
    }
}
