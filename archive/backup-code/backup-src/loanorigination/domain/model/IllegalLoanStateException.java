
package com.bank.loanmanagement.loanorigination.domain.model;

public class IllegalLoanStateException extends RuntimeException {
    public IllegalLoanStateException(String message) {
        super(message);
    }
}
