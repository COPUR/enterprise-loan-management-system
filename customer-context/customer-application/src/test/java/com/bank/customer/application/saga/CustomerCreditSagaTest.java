package com.bank.customer.application.saga;

import com.bank.customer.application.CustomerManagementService;
import com.bank.shared.kernel.domain.CustomerId;
import com.bank.shared.kernel.domain.Money;
import com.bank.shared.kernel.event.LoanDisbursedEvent;
import com.bank.shared.kernel.event.LoanFullyPaidEvent;
import com.bank.shared.kernel.event.PaymentFailedEvent;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class CustomerCreditSagaTest {

    @Mock
    private CustomerManagementService customerService;

    @InjectMocks
    private CustomerCreditSaga saga;

    @Test
    void handleLoanDisbursedShouldReserveCredit() {
        LoanDisbursedEvent event = new LoanDisbursedEvent(
            "LOAN-001",
            CustomerId.of("CUST-SAGA-001"),
            Money.aed(new BigDecimal("1500.00")),
            LocalDate.now()
        );

        saga.handleLoanDisbursed(event);

        verify(customerService).reserveCredit("CUST-SAGA-001", Money.aed(new BigDecimal("1500.00")));
    }

    @Test
    void handleLoanDisbursedShouldSwallowExceptions() {
        LoanDisbursedEvent event = new LoanDisbursedEvent(
            "LOAN-001",
            CustomerId.of("CUST-SAGA-002"),
            Money.aed(new BigDecimal("1500.00")),
            LocalDate.now()
        );
        doThrow(new RuntimeException("failure")).when(customerService).reserveCredit(any(), any());

        assertThatCode(() -> saga.handleLoanDisbursed(event)).doesNotThrowAnyException();
    }

    @Test
    void compensateFailedCreditReservationShouldReleaseCredit() {
        saga.compensateFailedCreditReservation(
            "CUST-SAGA-003",
            Money.aed(new BigDecimal("200.00")),
            "downstream-failure"
        );

        verify(customerService).releaseCredit("CUST-SAGA-003", Money.aed(new BigDecimal("200.00")));
    }

    @Test
    void compensateFailedCreditReservationShouldSwallowExceptions() {
        doThrow(new RuntimeException("failure")).when(customerService).releaseCredit(any(), any());

        assertThatCode(() -> saga.compensateFailedCreditReservation(
            "CUST-SAGA-004",
            Money.aed(new BigDecimal("50.00")),
            "reason"
        )).doesNotThrowAnyException();
    }

    @Test
    void loanFullyPaidAndPaymentFailedHandlersShouldNotThrow() {
        LoanFullyPaidEvent loanFullyPaidEvent = new LoanFullyPaidEvent("LOAN-100", CustomerId.of("CUST-SAGA-005"));
        PaymentFailedEvent paymentFailedEvent = new PaymentFailedEvent("PAY-100", CustomerId.of("CUST-SAGA-005"), "failure");

        assertThatCode(() -> saga.handleLoanFullyPaid(loanFullyPaidEvent)).doesNotThrowAnyException();
        assertThatCode(() -> saga.handlePaymentFailed(paymentFailedEvent)).doesNotThrowAnyException();
    }
}
