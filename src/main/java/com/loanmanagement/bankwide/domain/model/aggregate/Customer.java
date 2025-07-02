// bank-wide-service/domain/model/aggregate/Customer.java
package com.loanmanagement.bankwide.domain.model.aggregate;

import com.loanmanagement.bankwide.domain.model.value.CustomerId;
import com.loanmanagement.bankwide.domain.model.value.CustomerName;
import com.loanmanagement.bankwide.domain.event.*;
import com.loanmanagement.sharedkernel.domain.value.Money;
import com.loanmanagement.sharedkernel.domain.model.AggregateRoot;

import java.util.ArrayList;
import java.util.List;

public class Customer extends AggregateRoot<CustomerId> {
    
    private final CustomerId customerId;
    private final CustomerName name;
    private Money creditLimit;
    private Money usedCreditLimit;
    
    // Factory method for creating new customers
    public static Customer create(
        CustomerId customerId,
        CustomerName name,
        Money creditLimit
    ) {
        Customer customer = new Customer(customerId, name, creditLimit, Money.zero());
        
        customer.addDomainEvent(new CustomerCreated(
            customerId,
            name.getFullName(),
            creditLimit
        ));
        
        return customer;
    }
    
    // Constructor for reconstitution
    private Customer(
        CustomerId customerId,
        CustomerName name,
        Money creditLimit,
        Money usedCreditLimit
    ) {
        super(customerId);
        this.customerId = customerId;
        this.name = name;
        this.creditLimit = creditLimit;
        this.usedCreditLimit = usedCreditLimit;
    }
    
    public Money getAvailableCredit() {
        return creditLimit.subtract(usedCreditLimit);
    }
    
    public boolean hasAvailableCreditFor(Money requestedAmount) {
        return getAvailableCredit().isGreaterThanOrEqualTo(requestedAmount);
    }
    
    public void reserveCreditForLoan(Money loanPrincipalAmount) {
        if (!hasAvailableCreditFor(loanPrincipalAmount)) {
            throw new InsufficientCreditException(
                customerId,
                loanPrincipalAmount,
                getAvailableCredit()
            );
        }
        
        usedCreditLimit = usedCreditLimit.add(loanPrincipalAmount);
        
        addDomainEvent(new CreditReserved(
            customerId,
            loanPrincipalAmount,
            usedCreditLimit,
            getAvailableCredit()
        ));
    }
    
    public void releaseCreditFromLoan(Money loanPrincipalAmount) {
        usedCreditLimit = usedCreditLimit.subtract(loanPrincipalAmount);
        
        addDomainEvent(new CreditReleased(
            customerId,
            loanPrincipalAmount,
            usedCreditLimit,
            getAvailableCredit()
        ));
    }
    
    public void updateCreditLimit(Money newCreditLimit) {
        Money oldCreditLimit = this.creditLimit;
        this.creditLimit = newCreditLimit;
        
        addDomainEvent(new CreditLimitUpdated(
            customerId,
            oldCreditLimit,
            newCreditLimit,
            getAvailableCredit()
        ));
    }
    
    // Getters
    public CustomerId getCustomerId() { return customerId; }
    public CustomerName getName() { return name; }
    public Money getCreditLimit() { return creditLimit; }
    public Money getUsedCreditLimit() { return usedCreditLimit; }
}


