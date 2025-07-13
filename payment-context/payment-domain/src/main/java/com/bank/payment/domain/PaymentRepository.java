package com.bank.payment.domain;

import com.bank.shared.kernel.domain.CustomerId;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * Repository interface for Payment aggregate
 * 
 * This is a domain service interface that will be implemented
 * by the infrastructure layer following DDD patterns
 */
public interface PaymentRepository {
    
    /**
     * Save a payment aggregate
     */
    Payment save(Payment payment);
    
    /**
     * Find payment by ID
     */
    Optional<Payment> findById(PaymentId paymentId);
    
    /**
     * Find all payments for a customer
     */
    List<Payment> findByCustomerId(CustomerId customerId);
    
    /**
     * Find payments by status
     */
    List<Payment> findByStatus(PaymentStatus status);
    
    /**
     * Find payments by date range
     */
    List<Payment> findByDateRange(LocalDate startDate, LocalDate endDate);
    
    /**
     * Find payments by account
     */
    List<Payment> findByFromAccountId(AccountId accountId);
    
    /**
     * Find payments by account
     */
    List<Payment> findByToAccountId(AccountId accountId);
    
    /**
     * Check if payment exists
     */
    boolean existsById(PaymentId paymentId);
    
    /**
     * Delete a payment
     */
    void delete(Payment payment);
    
    /**
     * Find failed payments for retry
     */
    List<Payment> findFailedPayments();
    
    /**
     * Find pending payments older than specified hours
     */
    List<Payment> findPendingPaymentsOlderThan(int hours);
}