package com.bank.payment.infrastructure.persistence;

import com.bank.payment.domain.AccountId;
import com.bank.payment.domain.Payment;
import com.bank.payment.domain.PaymentId;
import com.bank.payment.domain.PaymentStatus;
import com.bank.payment.domain.PaymentType;
import com.bank.shared.kernel.domain.CustomerId;
import com.bank.shared.kernel.domain.Money;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("In-memory payment repository adapter tests")
class InMemoryPaymentRepositoryAdapterTest {

    private final InMemoryPaymentRepositoryAdapter repository = new InMemoryPaymentRepositoryAdapter();

    @Test
    @DisplayName("Should save and retrieve by ID")
    void shouldSaveAndRetrieveById() {
        Payment payment = newPayment("PAY-00000001", "CUST-10000001", "ACC-11111111", "ACC-22222222", 100);

        repository.save(payment);

        assertThat(repository.findById(PaymentId.of("PAY-00000001"))).contains(payment);
        assertThat(repository.existsById(PaymentId.of("PAY-00000001"))).isTrue();
    }

    @Test
    @DisplayName("Should filter by customer and account identifiers")
    void shouldFilterByCustomerAndAccounts() {
        Payment p1 = newPayment("PAY-00000002", "CUST-20000001", "ACC-11111112", "ACC-22222223", 200);
        Payment p2 = newPayment("PAY-00000003", "CUST-20000001", "ACC-11111113", "ACC-22222224", 300);
        Payment p3 = newPayment("PAY-00000004", "CUST-20000002", "ACC-11111112", "ACC-22222225", 400);

        repository.save(p1);
        repository.save(p2);
        repository.save(p3);

        assertThat(repository.findByCustomerId(CustomerId.of("CUST-20000001"))).containsExactlyInAnyOrder(p1, p2);
        assertThat(repository.findByFromAccountId(AccountId.of("ACC-11111112"))).containsExactlyInAnyOrder(p1, p3);
        assertThat(repository.findByToAccountId(AccountId.of("ACC-22222224"))).containsExactly(p2);
    }

    @Test
    @DisplayName("Should support status, date range, and age queries")
    void shouldSupportStatusDateAndAgeQueries() {
        Payment pending = newPayment("PAY-00000005", "CUST-30000001", "ACC-11111114", "ACC-22222226", 500);
        Payment failed = newPayment("PAY-00000006", "CUST-30000002", "ACC-11111115", "ACC-22222227", 600);
        failed.fail("simulated failure");

        repository.save(pending);
        repository.save(failed);

        LocalDate today = LocalDate.now();
        List<Payment> todayPayments = repository.findByDateRange(today, today);

        assertThat(repository.findByStatus(PaymentStatus.PENDING)).contains(pending);
        assertThat(repository.findFailedPayments()).containsExactly(failed);
        assertThat(repository.findPendingPaymentsOlderThan(0)).contains(pending);
        assertThat(todayPayments).contains(pending, failed);
    }

    @Test
    @DisplayName("Should delete persisted payment")
    void shouldDeletePayment() {
        Payment payment = newPayment("PAY-00000007", "CUST-40000001", "ACC-11111116", "ACC-22222228", 700);
        repository.save(payment);

        repository.delete(payment);

        assertThat(repository.findById(PaymentId.of("PAY-00000007"))).isEmpty();
        assertThat(repository.existsById(PaymentId.of("PAY-00000007"))).isFalse();
    }

    private static Payment newPayment(String paymentId, String customerId, String fromAccountId, String toAccountId, int amount) {
        return Payment.create(
            PaymentId.of(paymentId),
            CustomerId.of(customerId),
            AccountId.of(fromAccountId),
            AccountId.of(toAccountId),
            Money.usd(BigDecimal.valueOf(amount)),
            PaymentType.TRANSFER,
            "test payment"
        );
    }
}
