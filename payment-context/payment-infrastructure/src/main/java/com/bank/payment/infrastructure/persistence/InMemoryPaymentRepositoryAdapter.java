package com.bank.payment.infrastructure.persistence;

import com.bank.payment.domain.AccountId;
import com.bank.payment.domain.Payment;
import com.bank.payment.domain.PaymentId;
import com.bank.payment.domain.PaymentRepository;
import com.bank.payment.domain.PaymentStatus;
import com.bank.shared.kernel.domain.CustomerId;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * In-memory adapter for the payment domain repository port.
 */
@Repository
public class InMemoryPaymentRepositoryAdapter implements PaymentRepository {

    private final ConcurrentMap<String, Payment> payments = new ConcurrentHashMap<>();

    @Override
    public Payment save(Payment payment) {
        Objects.requireNonNull(payment, "Payment cannot be null");
        PaymentId id = Objects.requireNonNull(payment.getId(), "Payment ID cannot be null");
        payments.put(id.getValue(), payment);
        return payment;
    }

    @Override
    public Optional<Payment> findById(PaymentId paymentId) {
        Objects.requireNonNull(paymentId, "Payment ID cannot be null");
        return Optional.ofNullable(payments.get(paymentId.getValue()));
    }

    @Override
    public List<Payment> findAll() {
        return snapshot();
    }

    @Override
    public List<Payment> findByCustomerId(CustomerId customerId) {
        Objects.requireNonNull(customerId, "Customer ID cannot be null");
        return payments.values().stream()
            .filter(payment -> customerId.equals(payment.getCustomerId()))
            .sorted(byNewestFirst())
            .toList();
    }

    @Override
    public List<Payment> findByStatus(PaymentStatus status) {
        Objects.requireNonNull(status, "Payment status cannot be null");
        return payments.values().stream()
            .filter(payment -> status == payment.getStatus())
            .sorted(byNewestFirst())
            .toList();
    }

    @Override
    public List<Payment> findByDateRange(LocalDate startDate, LocalDate endDate) {
        if (startDate == null || endDate == null) {
            return snapshot();
        }

        LocalDate start = startDate.isBefore(endDate) ? startDate : endDate;
        LocalDate end = endDate.isAfter(startDate) ? endDate : startDate;

        return payments.values().stream()
            .filter(payment -> payment.getCreatedAt() != null)
            .filter(payment -> {
                LocalDate paymentDate = payment.getCreatedAt().toLocalDate();
                return !paymentDate.isBefore(start) && !paymentDate.isAfter(end);
            })
            .sorted(byNewestFirst())
            .toList();
    }

    @Override
    public List<Payment> findByFromAccountId(AccountId accountId) {
        Objects.requireNonNull(accountId, "Account ID cannot be null");
        return payments.values().stream()
            .filter(payment -> accountId.equals(payment.getFromAccountId()))
            .sorted(byNewestFirst())
            .toList();
    }

    @Override
    public List<Payment> findByToAccountId(AccountId accountId) {
        Objects.requireNonNull(accountId, "Account ID cannot be null");
        return payments.values().stream()
            .filter(payment -> accountId.equals(payment.getToAccountId()))
            .sorted(byNewestFirst())
            .toList();
    }

    @Override
    public boolean existsById(PaymentId paymentId) {
        Objects.requireNonNull(paymentId, "Payment ID cannot be null");
        return payments.containsKey(paymentId.getValue());
    }

    @Override
    public void delete(Payment payment) {
        if (payment == null || payment.getId() == null) {
            return;
        }
        payments.remove(payment.getId().getValue());
    }

    @Override
    public List<Payment> findFailedPayments() {
        return findByStatus(PaymentStatus.FAILED);
    }

    @Override
    public List<Payment> findPendingPaymentsOlderThan(int hours) {
        if (hours < 0) {
            throw new IllegalArgumentException("Hours must be >= 0");
        }
        LocalDateTime threshold = LocalDateTime.now().minusHours(hours);
        return payments.values().stream()
            .filter(payment -> payment.getStatus() == PaymentStatus.PENDING)
            .filter(payment -> payment.getCreatedAt() != null && payment.getCreatedAt().isBefore(threshold))
            .sorted(byNewestFirst())
            .toList();
    }

    private List<Payment> snapshot() {
        return payments.values().stream()
            .sorted(byNewestFirst())
            .toList();
    }

    private static Comparator<Payment> byNewestFirst() {
        return Comparator
            .comparing(Payment::getCreatedAt, Comparator.nullsLast(Comparator.reverseOrder()))
            .thenComparing(payment -> payment.getId() == null ? "" : payment.getId().getValue());
    }
}
