package com.loanmanagement.loan.domain.model;

import com.loanmanagement.shared.domain.DomainId;
import lombok.EqualsAndHashCode;
import lombok.Value;

import java.util.UUID;

/**
 * Loan ID Value Object
 * Unique identifier for loan entities
 */
@Value
@EqualsAndHashCode(callSuper = true)
public class LoanId extends DomainId {

    public LoanId(String value) {
        super(value);
    }

    public static LoanId of(String value) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException("Loan ID cannot be null or empty");
        }
        return new LoanId(value);
    }

    public static LoanId generate() {
        return new LoanId("LOAN-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());
    }

    @Override
    public String toString() {
        return getValue();
    }
}