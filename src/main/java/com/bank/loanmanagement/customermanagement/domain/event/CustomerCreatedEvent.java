package com.bank.loanmanagement.customermanagement.domain.event;

import com.bank.loanmanagement.sharedkernel.domain.DomainEvent;
import com.bank.loanmanagement.sharedkernel.domain.Money;
import com.bank.loanmanagement.customermanagement.domain.model.CustomerId;
import com.bank.loanmanagement.customermanagement.domain.model.PersonalName;
import com.bank.loanmanagement.customermanagement.domain.model.EmailAddress;

import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Domain event published when a new customer is created.
 */
public class CustomerCreatedEvent extends DomainEvent {
    
    private final CustomerId customerId;
    private final PersonalName name;
    private final EmailAddress email;
    private final Money initialCreditLimit;
    
    public CustomerCreatedEvent(
        CustomerId customerId,
        PersonalName name,
        EmailAddress email,
        Money initialCreditLimit
    ) {
        super();
        this.customerId = Objects.requireNonNull(customerId);
        this.name = Objects.requireNonNull(name);
        this.email = Objects.requireNonNull(email);
        this.initialCreditLimit = Objects.requireNonNull(initialCreditLimit);
    }
    
    public CustomerId getCustomerId() {
        return customerId;
    }
    
    public PersonalName getName() {
        return name;
    }
    
    public EmailAddress getEmail() {
        return email;
    }
    
    public Money getInitialCreditLimit() {
        return initialCreditLimit;
    }
    
    @Override
    public String getEventType() {
        return "CustomerCreated";
    }
    
    @Override
    public String toString() {
        return "CustomerCreatedEvent{" +
                "customerId=" + customerId +
                ", name=" + name +
                ", email=" + email +
                ", initialCreditLimit=" + initialCreditLimit +
                ", occurredOn=" + getOccurredOn() +
                '}';
    }
}