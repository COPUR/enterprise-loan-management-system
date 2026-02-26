package com.bank.loan.application;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class LoanApplicationExceptionsTest {

    @Test
    void insufficientCreditExceptionFactoryShouldBuildDetailedMessage() {
        InsufficientCreditException ex = InsufficientCreditException.forCustomer(
            "CUST-EX-001",
            "AED 10000.00",
            "AED 2500.00"
        );

        assertThat(ex.getMessage()).contains("CUST-EX-001");
        assertThat(ex.getMessage()).contains("Requested");
        assertThat(ex.getMessage()).contains("Available");
    }

    @Test
    void loanNotFoundExceptionFactoryShouldBuildDetailedMessage() {
        LoanNotFoundException ex = LoanNotFoundException.withId("LOAN-EX-001");

        assertThat(ex.getMessage()).isEqualTo("Loan not found with ID: LOAN-EX-001");
    }

    @Test
    void loanNotFoundExceptionWithCauseShouldPreserveCause() {
        RuntimeException cause = new RuntimeException("db unavailable");
        LoanNotFoundException ex = new LoanNotFoundException("LOAN-EX-002", cause);

        assertThat(ex.getMessage()).contains("LOAN-EX-002");
        assertThat(ex.getCause()).isSameAs(cause);
    }
}
