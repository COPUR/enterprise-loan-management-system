package com.bank.payment.domain;

import com.bank.shared.kernel.domain.ValueObject;

import java.util.Objects;

/**
 * Value Object representing an Account ID
 */
public final class AccountId implements ValueObject {
    
    private final String value;
    
    private AccountId(String value) {
        this.value = Objects.requireNonNull(value, "Account ID cannot be null");
        if (value.trim().isEmpty()) {
            throw new IllegalArgumentException("Account ID cannot be empty");
        }
    }
    
    public static AccountId of(String value) {
        return new AccountId(value);
    }
    
    public String getValue() {
        return value;
    }
    
    @Override
    public boolean isEmpty() {
        return value == null || value.trim().isEmpty();
    }
    
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        AccountId accountId = (AccountId) obj;
        return Objects.equals(value, accountId.value);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(value);
    }
    
    @Override
    public String toString() {
        return "AccountId{" + value + "}";
    }
}