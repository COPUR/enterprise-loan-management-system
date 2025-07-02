package com.loanmanagement.loan.domain.model.value;

import com.loanmanagement.sharedkernel.domain.model.ValueObject;
import java.util.UUID;

/**
 * Value object representing a unique loan identifier.
 * Follows DDD principles by encapsulating loan identity within the loan bounded context.
 */
public final class LoanId extends ValueObject {

    private final String value;

    private LoanId(String value) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException("LoanId cannot be null or empty");
        }
        this.value = value;
    }

    public static LoanId generate() {
        return new LoanId(UUID.randomUUID().toString());
    }

    public static LoanId of(String value) {
        return new LoanId(value);
    }

    public String getValue() {
        return value;
    }

    @Override
    protected Object[] getEqualityComponents() {
        return new Object[]{value};
    }

    @Override
    public String toString() {
        return value;
    }
}
