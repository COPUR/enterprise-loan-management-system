package com.bank.payment.domain;

import com.bank.shared.kernel.domain.CustomerId;
import com.bank.shared.kernel.domain.Money;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

class PaymentEventsTest {

    @Test
    void allPaymentEventsShouldExposeTheirPayload() {
        PaymentId paymentId = PaymentId.of("PAY-EVT-001");
        CustomerId customerId = CustomerId.of("CUST-EVT-001");
        LoanId loanId = LoanId.of("LOAN-EVT-001");
        Money amount = Money.aed(new BigDecimal("500.00"));

        PaymentCreatedEvent created = new PaymentCreatedEvent(paymentId, customerId, amount, PaymentType.TRANSFER);
        PaymentProcessingEvent processing = new PaymentProcessingEvent(paymentId, customerId);
        PaymentCompletedEvent completed = new PaymentCompletedEvent(paymentId, customerId, amount);
        PaymentFailedEvent failed = new PaymentFailedEvent(paymentId, customerId, "reason");
        PaymentCancelledEvent cancelled = new PaymentCancelledEvent(paymentId, customerId, "cancelled");
        PaymentRefundedEvent refunded = new PaymentRefundedEvent(paymentId, customerId, amount);
        LoanPaymentCreatedEvent loanCreated = new LoanPaymentCreatedEvent(paymentId, customerId, loanId, amount);
        LoanPaymentCompletedEvent loanCompleted = new LoanPaymentCompletedEvent(paymentId, customerId, loanId, amount);
        LoanPaymentFailedEvent loanFailed = new LoanPaymentFailedEvent(paymentId, customerId, loanId, "timeout");

        assertThat(created.getPaymentType()).isEqualTo(PaymentType.TRANSFER);
        assertThat(created.getAmount()).isEqualTo(amount);
        assertThat(created.getPaymentId()).isEqualTo(paymentId);
        assertThat(created.getEventId()).isNotBlank();
        assertThat(created.getOccurredOn()).isNotNull();

        assertThat(processing.getPaymentId()).isEqualTo(paymentId);
        assertThat(processing.getCustomerId()).isEqualTo(customerId);
        assertThat(processing.getEventId()).isNotBlank();
        assertThat(processing.getOccurredOn()).isNotNull();

        assertThat(completed.getPaymentId()).isEqualTo(paymentId);
        assertThat(completed.getAmount()).isEqualTo(amount);
        assertThat(completed.getEventId()).isNotBlank();
        assertThat(completed.getOccurredOn()).isNotNull();

        assertThat(failed.getPaymentId()).isEqualTo(paymentId);
        assertThat(failed.getCustomerId()).isEqualTo(customerId);
        assertThat(failed.getFailureReason()).isEqualTo("reason");
        assertThat(failed.getEventId()).isNotBlank();
        assertThat(failed.getOccurredOn()).isNotNull();

        assertThat(cancelled.getPaymentId()).isEqualTo(paymentId);
        assertThat(cancelled.getCustomerId()).isEqualTo(customerId);
        assertThat(cancelled.getCancellationReason()).isEqualTo("cancelled");
        assertThat(cancelled.getEventId()).isNotBlank();
        assertThat(cancelled.getOccurredOn()).isNotNull();

        assertThat(refunded.getPaymentId()).isEqualTo(paymentId);
        assertThat(refunded.getCustomerId()).isEqualTo(customerId);
        assertThat(refunded.getRefundAmount()).isEqualTo(amount);
        assertThat(refunded.getEventId()).isNotBlank();
        assertThat(refunded.getOccurredOn()).isNotNull();

        assertThat(loanCreated.getPaymentId()).isEqualTo(paymentId);
        assertThat(loanCreated.getCustomerId()).isEqualTo(customerId);
        assertThat(loanCreated.getLoanId()).isEqualTo(loanId);
        assertThat(loanCreated.getScheduledAmount()).isEqualTo(amount);
        assertThat(loanCreated.getEventId()).isNotBlank();
        assertThat(loanCreated.getOccurredOn()).isNotNull();

        assertThat(loanCompleted.getPaymentId()).isEqualTo(paymentId);
        assertThat(loanCompleted.getCustomerId()).isEqualTo(customerId);
        assertThat(loanCompleted.getActualAmount()).isEqualTo(amount);
        assertThat(loanCompleted.getLoanId()).isEqualTo(loanId);
        assertThat(loanCompleted.getEventId()).isNotBlank();
        assertThat(loanCompleted.getOccurredOn()).isNotNull();

        assertThat(loanFailed.getPaymentId()).isEqualTo(paymentId);
        assertThat(loanFailed.getCustomerId()).isEqualTo(customerId);
        assertThat(loanFailed.getLoanId()).isEqualTo(loanId);
        assertThat(loanFailed.getFailureReason()).isEqualTo("timeout");
        assertThat(loanFailed.getEventId()).isNotBlank();
        assertThat(loanFailed.getOccurredOn()).isNotNull();
        assertThat(loanCreated.toString()).contains("LoanPaymentCreatedEvent");
        assertThat(loanCompleted.toString()).contains("LoanPaymentCompletedEvent");
        assertThat(loanFailed.toString()).contains("LoanPaymentFailedEvent");
    }
}
