package com.loanmanagement.domain.model.entity;

import com.loanmanagement.sharedkernel.domain.value.Money;
import com.loanmanagement.domain.event.*;
import com.loanmanagement.domain.exception.InsufficientCreditException;
import java.util.ArrayList;
import java.util.List;

/**
 * Customer domain entity following DDD principles.
 * Encapsulates customer business logic and maintains invariants.
 */
public class Customer {
    private final Long id;
    private final String name;
    private final String surname;
    private final Money creditLimit;
    private Money usedCreditLimit;
    private final List<DomainEvent> domainEvents = new ArrayList<>();

    public Customer(Long id, String name, String surname, Money creditLimit) {
        if (id == null || id <= 0) {
            throw new IllegalArgumentException("Customer ID must be positive");
        }
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("Customer name cannot be null or empty");
        }
        if (surname == null || surname.trim().isEmpty()) {
            throw new IllegalArgumentException("Customer surname cannot be null or empty");
        }
        if (creditLimit == null || !creditLimit.isPositive()) {
            throw new IllegalArgumentException("Credit limit must be positive");
        }

        this.id = id;
        this.name = name;
        this.surname = surname;
        this.creditLimit = creditLimit;
        this.usedCreditLimit = Money.ZERO;
    }
    
    public Money getAvailableCredit() {
        return creditLimit.subtract(usedCreditLimit);
    }
    
    public boolean hasAvailableCreditFor(Money amount) {
        return getAvailableCredit().isGreaterThanOrEqual(amount);
    }
    
    /**
     * Reserves credit for a loan, ensuring business invariants are maintained.
     */
    public void reserveCredit(Money amount) {
        if (amount == null || !amount.isPositive()) {
            throw new IllegalArgumentException("Amount must be positive");
        }

        if (!hasAvailableCreditFor(amount)) {
            throw new InsufficientCreditException(
                id,
                amount.toString(),
                getAvailableCredit().toString()
            );
        }

        this.usedCreditLimit = usedCreditLimit.add(amount);
        // Domain event would be added here in a complete implementation
    }

    /**
     * Releases credit from a completed or cancelled loan.
     */
    public void releaseCredit(Money amount) {
        if (amount == null || !amount.isPositive()) {
            throw new IllegalArgumentException("Amount must be positive");
        }

        if (amount.isGreaterThan(usedCreditLimit)) {
            throw new IllegalArgumentException("Cannot release more credit than currently used");
        }

        this.usedCreditLimit = usedCreditLimit.subtract(amount);
        // Domain event would be added here in a complete implementation
    }

    // Getters following clean code principles
    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getSurname() {
        return surname;
    }

    public Money getCreditLimit() {
        return creditLimit;
    }

    public Money getUsedCreditLimit() {
        return usedCreditLimit;
    }

    public List<DomainEvent> getDomainEvents() {
        return new ArrayList<>(domainEvents);
    }

    public void clearDomainEvents() {
        domainEvents.clear();
    }
}