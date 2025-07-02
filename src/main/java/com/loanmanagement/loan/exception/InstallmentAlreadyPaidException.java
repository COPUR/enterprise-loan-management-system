package com.loanmanagement.loan.exception;

import com.loanmanagement.loan.domain.model.value.InstallmentId;

/**
 * Domain exception thrown when attempting to pay an already paid installment.
 * Follows DDD principles by representing a business rule violation.
 */
public class InstallmentAlreadyPaidException extends RuntimeException {

    private final InstallmentId installmentId;

    public InstallmentAlreadyPaidException(InstallmentId installmentId) {
        super("Installment " + installmentId + " has already been paid");
        this.installmentId = installmentId;
    }

    public InstallmentId getInstallmentId() {
        return installmentId;
    }
}
