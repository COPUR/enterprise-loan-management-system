package com.bank.loan.domain;

import com.bank.shared.kernel.domain.CustomerId;
import com.bank.shared.kernel.domain.Money;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

class LoanEventsTest {

    @Test
    void loanEventsShouldExposePayloadFields() {
        LoanId loanId = LoanId.of("LOAN-EVT-001");
        CustomerId customerId = CustomerId.of("CUST-EVT-001");
        Money amount = Money.aed(new BigDecimal("5000.00"));
        LocalDate disbursementDate = LocalDate.now();

        LoanCreatedEvent created = new LoanCreatedEvent(loanId, customerId, amount);
        LoanApprovedEvent approved = new LoanApprovedEvent(loanId, customerId, amount);
        LoanRejectedEvent rejected = new LoanRejectedEvent(loanId, customerId, "policy");
        LoanDisbursedEvent disbursed = new LoanDisbursedEvent(loanId, customerId, amount, disbursementDate);
        LoanCancelledEvent cancelled = new LoanCancelledEvent(loanId, customerId, "withdrawn");
        LoanPaymentMadeEvent paymentMade = new LoanPaymentMadeEvent(
            loanId,
            customerId,
            Money.aed(new BigDecimal("1000.00")),
            Money.aed(new BigDecimal("5000.00")),
            Money.aed(new BigDecimal("4000.00"))
        );
        LoanFullyPaidEvent fullyPaid = new LoanFullyPaidEvent(loanId, customerId);

        assertThat(created.getLoanId()).isEqualTo(loanId);
        assertThat(created.getCustomerId()).isEqualTo(customerId);
        assertThat(created.getPrincipalAmount()).isEqualTo(amount);
        assertThat(created.getEventId()).isNotBlank();
        assertThat(created.getOccurredOn()).isNotNull();

        assertThat(approved.getPrincipalAmount()).isEqualTo(amount);
        assertThat(rejected.getReason()).isEqualTo("policy");
        assertThat(disbursed.getDisbursementDate()).isEqualTo(disbursementDate);
        assertThat(cancelled.getReason()).isEqualTo("withdrawn");
        assertThat(paymentMade.getPaymentAmount().getAmount()).isEqualByComparingTo("1000.00");
        assertThat(paymentMade.getPreviousBalance().getAmount()).isEqualByComparingTo("5000.00");
        assertThat(paymentMade.getNewBalance().getAmount()).isEqualByComparingTo("4000.00");
        assertThat(fullyPaid.getLoanId()).isEqualTo(loanId);
        assertThat(fullyPaid.getCustomerId()).isEqualTo(customerId);
    }
}
