
package com.bank.loanmanagement.customermanagement.domain.model;

public class InsufficientCreditException extends RuntimeException {
    public InsufficientCreditException(String message) {
        super(message);
    }
}
