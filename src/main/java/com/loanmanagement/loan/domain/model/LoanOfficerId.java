package com.loanmanagement.loan.domain.model;

import com.loanmanagement.shared.domain.DomainId;
import lombok.EqualsAndHashCode;
import lombok.Value;

import java.util.UUID;

/**
 * Loan Officer ID Value Object
 * Unique identifier for loan officer entities
 */
@Value
@EqualsAndHashCode(callSuper = true)
public class LoanOfficerId extends DomainId {

    public LoanOfficerId(String value) {
        super(value);
    }

    public static LoanOfficerId of(String value) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException("Loan Officer ID cannot be null or empty");
        }
        return new LoanOfficerId(value);
    }

    public static LoanOfficerId generate() {
        return new LoanOfficerId("OFFICER-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());
    }

    @Override
    public String toString() {
        return getValue();
    }
}