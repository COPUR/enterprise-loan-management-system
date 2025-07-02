package com.loanmanagement.loan.exception;

public class NoPayableInstallmentsException extends RuntimeException {
    public NoPayableInstallmentsException(String message) {
        super(message);
    }
}