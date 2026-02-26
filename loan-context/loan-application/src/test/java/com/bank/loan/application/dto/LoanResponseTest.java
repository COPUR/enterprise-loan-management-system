package com.bank.loan.application.dto;

import com.bank.loan.domain.InterestRate;
import com.bank.loan.domain.Loan;
import com.bank.loan.domain.LoanId;
import com.bank.loan.domain.LoanTerm;
import com.bank.shared.kernel.domain.CustomerId;
import com.bank.shared.kernel.domain.Money;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

class LoanResponseTest {

    @Test
    void fromShouldMapLoanAggregateFields() {
        Loan loan = Loan.create(
            LoanId.of("LOAN-RESP-001"),
            CustomerId.of("CUST-RESP-001"),
            Money.aed(new BigDecimal("12000.00")),
            InterestRate.of(new BigDecimal("6.0")),
            LoanTerm.ofMonths(12)
        );

        LoanResponse response = LoanResponse.from(loan);

        assertThat(response.loanId()).isEqualTo("LOAN-RESP-001");
        assertThat(response.customerId()).isEqualTo("CUST-RESP-001");
        assertThat(response.principalAmount()).isEqualByComparingTo("12000.00");
        assertThat(response.annualInterestRate()).isEqualByComparingTo("6.0000");
        assertThat(response.termInMonths()).isEqualTo(12);
        assertThat(response.status()).isEqualTo("CREATED");
        assertThat(response.monthlyPayment()).isPositive();
        assertThat(response.createdAt()).isNotNull();
        assertThat(response.lastModifiedAt()).isNotNull();
    }
}
