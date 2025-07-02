package com.loanmanagement.loan.domain.model.value;

import com.loanmanagement.sharedkernel.domain.model.ValueObject;
import java.util.UUID;

/**
 * Value object representing a unique installment identifier.
 * Follows DDD principles by encapsulating installment identity within the loan bounded context.
 */
public final class InstallmentId extends ValueObject {

    private final String value;

    private InstallmentId(String value) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException("InstallmentId cannot be null or empty");
        }
        this.value = value;
    }

    public static InstallmentId generate() {
        return new InstallmentId(UUID.randomUUID().toString());
    }

    public static InstallmentId of(String value) {
        return new InstallmentId(value);
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
