// domain/exception/InsufficientCreditException.java
package com.loanmanagement.loan.exception;

public class InsufficientCreditException extends RuntimeException {
    public InsufficientCreditException(String message) {
        super(message);
    }
}

// domain/exception/NoPayableInstallmentsException.java
