package com.bank.loanmanagement.domain.loan;

import com.bank.loanmanagement.domain.shared.EntityId;

import java.util.Objects;
import java.util.UUID;

/**
 * Domain value object for loan offer identification
 */
public class LoanOfferId extends EntityId {

    private LoanOfferId(String value) {
        super(value);
    }

    public static LoanOfferId of(String value) {
        Objects.requireNonNull(value, "Loan offer ID cannot be null");
        if (value.trim().isEmpty()) {
            throw new IllegalArgumentException("Loan offer ID cannot be empty");
        }
        return new LoanOfferId(value);
    }

    public static LoanOfferId generate() {
        return new LoanOfferId("OFFER-" + UUID.randomUUID().toString());
    }
}