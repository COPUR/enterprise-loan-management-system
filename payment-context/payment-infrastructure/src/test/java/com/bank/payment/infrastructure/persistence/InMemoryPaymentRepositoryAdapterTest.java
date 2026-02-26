package com.bank.payment.infrastructure.persistence;

import com.bank.payment.domain.AccountId;
import com.bank.payment.domain.Payment;
import com.bank.payment.domain.PaymentId;
import com.bank.payment.domain.PaymentStatus;
import com.bank.payment.domain.PaymentType;
import com.bank.shared.kernel.domain.CustomerId;
import com.bank.shared.kernel.domain.Money;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class InMemoryPaymentRepositoryAdapterTest {

    private final InMemoryPaymentRepositoryAdapter repository = new InMemoryPaymentRepositoryAdapter();

    @Test
    void saveFindExistsDeleteShouldWork() {
        Payment payment = payment("PAY-REP-001", "CUST-REP-001", "ACC-F-001", "ACC-T-001", "100.00");

        repository.save(payment);

        assertThat(repository.existsById(PaymentId.of("PAY-REP-001"))).isTrue();
        assertThat(repository.findById(PaymentId.of("PAY-REP-001"))).contains(payment);
        assertThat(repository.findAll()).contains(payment);

        repository.delete(payment);
        assertThat(repository.existsById(PaymentId.of("PAY-REP-001"))).isFalse();
        assertThat(repository.findById(PaymentId.of("PAY-REP-001"))).isEmpty();
        repository.delete(null);
    }

    @Test
    void shouldFilterByCustomerStatusAccountsAndDateRange() {
        Payment p1 = payment("PAY-REP-002", "CUST-REP-002", "ACC-F-002", "ACC-T-002", "100.00");
        Payment p2 = payment("PAY-REP-003", "CUST-REP-002", "ACC-F-003", "ACC-T-003", "200.00");
        Payment p3 = payment("PAY-REP-004", "CUST-REP-003", "ACC-F-003", "ACC-T-004", "300.00");
        p2.markAsProcessing();
        p2.confirm();
        p3.fail("network");

        repository.save(p1);
        repository.save(p2);
        repository.save(p3);

        assertThat(repository.findByCustomerId(CustomerId.of("CUST-REP-002"))).hasSize(2);
        assertThat(repository.findByStatus(PaymentStatus.COMPLETED)).containsExactly(p2);
        assertThat(repository.findFailedPayments()).containsExactly(p3);
        assertThat(repository.findByFromAccountId(AccountId.of("ACC-F-003"))).containsExactly(p3, p2);
        assertThat(repository.findByToAccountId(AccountId.of("ACC-T-003"))).containsExactly(p2);

        LocalDate today = LocalDate.now();
        assertThat(repository.findByDateRange(today, today)).hasSize(3);
        assertThat(repository.findByDateRange(today.plusDays(1), today.minusDays(1))).hasSize(3);
        assertThat(repository.findByDateRange(null, today)).hasSize(3);
    }

    @Test
    void findPendingPaymentsOlderThanShouldHonorThreshold() throws Exception {
        Payment oldPending = payment("PAY-REP-005", "CUST-REP-005", "ACC-F-005", "ACC-T-005", "400.00");
        Payment freshPending = payment("PAY-REP-006", "CUST-REP-006", "ACC-F-006", "ACC-T-006", "500.00");

        setCreatedAt(oldPending, LocalDateTime.now().minusHours(6));
        setCreatedAt(freshPending, LocalDateTime.now().minusMinutes(30));

        repository.save(oldPending);
        repository.save(freshPending);

        List<Payment> olderThanThreeHours = repository.findPendingPaymentsOlderThan(3);
        assertThat(olderThanThreeHours).contains(oldPending).doesNotContain(freshPending);

        assertThatThrownBy(() -> repository.findPendingPaymentsOlderThan(-1))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining(">= 0");
    }

    private static Payment payment(
        String paymentId,
        String customerId,
        String from,
        String to,
        String amount
    ) {
        return Payment.create(
            PaymentId.of(paymentId),
            CustomerId.of(customerId),
            AccountId.of(from),
            AccountId.of(to),
            Money.aed(new BigDecimal(amount)),
            PaymentType.TRANSFER,
            "repo-test"
        );
    }

    private static void setCreatedAt(Payment payment, LocalDateTime value) throws Exception {
        Field field = Payment.class.getDeclaredField("createdAt");
        field.setAccessible(true);
        field.set(payment, value);
    }
}
