package com.bank.payment.application;

import com.bank.payment.application.dto.CreatePaymentRequest;
import com.bank.payment.application.dto.PaymentResponse;
import com.bank.payment.domain.*;
import com.bank.shared.kernel.domain.CustomerId;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.stream.Stream;

/**
 * Application Service for Payment Processing
 * 
 * Implements functional requirements:
 * - FR-009: Payment Processing & Settlement
 * - FR-010: Payment Validation & Compliance
 * - FR-011: Payment Status Management
 * - FR-012: Payment Reconciliation
 */
@Service("paymentService")
@Transactional
public class PaymentProcessingService {
    
    private final PaymentRepository paymentRepository;
    private final AccountValidationService accountValidationService;
    private final FraudDetectionService fraudDetectionService;
    private final ComplianceService complianceService;
    
    public PaymentProcessingService(PaymentRepository paymentRepository,
                                  AccountValidationService accountValidationService,
                                  FraudDetectionService fraudDetectionService,
                                  ComplianceService complianceService) {
        this.paymentRepository = paymentRepository;
        this.accountValidationService = accountValidationService;
        this.fraudDetectionService = fraudDetectionService;
        this.complianceService = complianceService;
    }
    
    /**
     * FR-009: Process a new payment
     */
    public PaymentResponse processPayment(CreatePaymentRequest request) {
        // Validate request
        request.validate();
        
        // Validate accounts
        AccountId fromAccountId = AccountId.of(request.fromAccountId());
        AccountId toAccountId = AccountId.of(request.toAccountId());
        
        if (!accountValidationService.validateAccount(fromAccountId)) {
            throw InvalidAccountException.fromAccount(request.fromAccountId());
        }
        
        if (!accountValidationService.validateAccount(toAccountId)) {
            throw InvalidAccountException.toAccount(request.toAccountId());
        }
        
        // Check balance
        if (!accountValidationService.hasBalance(fromAccountId, request.getAmountAsMoney())) {
            throw InsufficientBalanceException.forAccount(
                request.fromAccountId(), 
                request.getAmountAsMoney().toString(),
                accountValidationService.getAccountBalance(fromAccountId).toString());
        }
        
        // Create payment
        Payment payment = Payment.create(
            PaymentId.generate(),
            CustomerId.of(request.customerId()),
            fromAccountId,
            toAccountId,
            request.getAmountAsMoney(),
            request.paymentType(),
            request.description()
        );
        
        // Fraud detection
        if (!fraudDetectionService.isValidPayment(payment)) {
            throw FraudDetectedException.generic();
        }
        
        // Compliance validation
        if (!complianceService.validatePayment(payment)) {
            throw ComplianceViolationException.generic();
        }
        
        // Save payment
        Payment savedPayment = paymentRepository.save(payment);
        
        return PaymentResponse.from(savedPayment);
    }
    
    /**
     * FR-011: Confirm a pending payment
     */
    public PaymentResponse confirmPayment(String paymentId) {
        PaymentId id = PaymentId.of(paymentId);
        Payment payment = paymentRepository.findById(id)
            .orElseThrow(() -> PaymentNotFoundException.withId(paymentId));
        
        payment.confirm();
        Payment savedPayment = paymentRepository.save(payment);
        
        return PaymentResponse.from(savedPayment);
    }
    
    /**
     * FR-011: Fail a pending payment
     */
    public PaymentResponse failPayment(String paymentId, String reason) {
        PaymentId id = PaymentId.of(paymentId);
        Payment payment = paymentRepository.findById(id)
            .orElseThrow(() -> PaymentNotFoundException.withId(paymentId));
        
        payment.fail(reason);
        Payment savedPayment = paymentRepository.save(payment);
        
        return PaymentResponse.from(savedPayment);
    }
    
    /**
     * FR-011: Cancel a pending payment
     */
    public PaymentResponse cancelPayment(String paymentId, String reason) {
        PaymentId id = PaymentId.of(paymentId);
        Payment payment = paymentRepository.findById(id)
            .orElseThrow(() -> PaymentNotFoundException.withId(paymentId));
        
        payment.cancel(reason);
        Payment savedPayment = paymentRepository.save(payment);
        
        return PaymentResponse.from(savedPayment);
    }
    
    /**
     * FR-012: Find payment by ID
     */
    @Transactional(readOnly = true)
    public PaymentResponse findPaymentById(String paymentId) {
        PaymentId id = PaymentId.of(paymentId);
        Payment payment = paymentRepository.findById(id)
            .orElseThrow(() -> PaymentNotFoundException.withId(paymentId));
        
        return PaymentResponse.from(payment);
    }
    
    /**
     * FR-011: Refund a completed payment
     */
    public PaymentResponse refundPayment(String paymentId) {
        PaymentId id = PaymentId.of(paymentId);
        Payment payment = paymentRepository.findById(id)
            .orElseThrow(() -> PaymentNotFoundException.withId(paymentId));
        
        payment.refund();
        Payment savedPayment = paymentRepository.save(payment);
        
        return PaymentResponse.from(savedPayment);
    }

    /**
     * FR-012: Search payments with filters and pagination
     */
    @Transactional(readOnly = true)
    public Page<PaymentResponse> searchPayments(
            String customerId,
            String status,
            String fromAccount,
            String toAccount,
            BigDecimal minAmount,
            BigDecimal maxAmount,
            LocalDateTime startDate,
            LocalDateTime endDate,
            Pageable pageable) {

        PaymentStatus parsedStatus = null;
        if (status != null && !status.isBlank()) {
            try {
                parsedStatus = PaymentStatus.valueOf(status.trim().toUpperCase());
            } catch (IllegalArgumentException ex) {
                return new PageImpl<>(Collections.emptyList(), pageable, 0);
            }
        }

        final PaymentStatus statusFilter = parsedStatus;
        List<Payment> base = resolveInitialSearchSet(customerId, statusFilter, fromAccount, toAccount, startDate, endDate);

        Stream<Payment> stream = base.stream();

        if (customerId != null && !customerId.isBlank()) {
            String normalized = customerId.trim();
            stream = stream.filter(payment -> payment.getCustomerId() != null
                && normalized.equals(payment.getCustomerId().getValue()));
        }

        if (statusFilter != null) {
            stream = stream.filter(payment -> payment.getStatus() == statusFilter);
        }

        if (fromAccount != null && !fromAccount.isBlank()) {
            String normalized = fromAccount.trim();
            stream = stream.filter(payment -> payment.getFromAccountId() != null
                && normalized.equals(payment.getFromAccountId().getValue()));
        }

        if (toAccount != null && !toAccount.isBlank()) {
            String normalized = toAccount.trim();
            stream = stream.filter(payment -> payment.getToAccountId() != null
                && normalized.equals(payment.getToAccountId().getValue()));
        }

        if (minAmount != null) {
            stream = stream.filter(payment -> payment.getAmount() != null
                && payment.getAmount().getAmount().compareTo(minAmount) >= 0);
        }

        if (maxAmount != null) {
            stream = stream.filter(payment -> payment.getAmount() != null
                && payment.getAmount().getAmount().compareTo(maxAmount) <= 0);
        }

        if (startDate != null) {
            stream = stream.filter(payment -> payment.getCreatedAt() != null
                && !payment.getCreatedAt().isBefore(startDate));
        }

        if (endDate != null) {
            stream = stream.filter(payment -> payment.getCreatedAt() != null
                && !payment.getCreatedAt().isAfter(endDate));
        }

        List<PaymentResponse> responses = stream
            .map(PaymentResponse::from)
            .toList();

        int total = responses.size();
        int start = (int) pageable.getOffset();
        int end = Math.min(start + pageable.getPageSize(), total);

        List<PaymentResponse> pageContent = start >= total
            ? Collections.emptyList()
            : responses.subList(start, end);

        return new PageImpl<>(pageContent, pageable, total);
    }

    /**
     * FR-011: Retry a failed payment
     */
    public PaymentResponse retryPayment(String paymentId) {
        PaymentId id = PaymentId.of(paymentId);
        Payment failedPayment = paymentRepository.findById(id)
            .orElseThrow(() -> PaymentNotFoundException.withId(paymentId));

        if (failedPayment.getStatus() != PaymentStatus.FAILED) {
            throw new IllegalStateException("Payment is not in FAILED status");
        }

        if (failedPayment.getFromAccountId() == null || failedPayment.getToAccountId() == null) {
            throw new IllegalStateException("Retry is only supported for account-based payments");
        }

        Payment retry = Payment.create(
            PaymentId.generate(),
            failedPayment.getCustomerId(),
            failedPayment.getFromAccountId(),
            failedPayment.getToAccountId(),
            failedPayment.getAmount(),
            failedPayment.getPaymentType(),
            failedPayment.getDescription()
        );

        Payment savedPayment = paymentRepository.save(retry);
        return PaymentResponse.from(savedPayment);
    }

    /**
     * Resolve customer ID for security checks
     */
    @Transactional(readOnly = true)
    public String getCustomerIdForPayment(String paymentId) {
        PaymentId id = PaymentId.of(paymentId);
        Payment payment = paymentRepository.findById(id)
            .orElseThrow(() -> PaymentNotFoundException.withId(paymentId));
        return payment.getCustomerId().getValue();
    }

    private List<Payment> resolveInitialSearchSet(
            String customerId,
            PaymentStatus status,
            String fromAccount,
            String toAccount,
            LocalDateTime startDate,
            LocalDateTime endDate) {

        if (customerId != null && !customerId.isBlank()) {
            return paymentRepository.findByCustomerId(CustomerId.of(customerId.trim()));
        }

        if (status != null) {
            return paymentRepository.findByStatus(status);
        }

        if (fromAccount != null && !fromAccount.isBlank()) {
            return paymentRepository.findByFromAccountId(AccountId.of(fromAccount.trim()));
        }

        if (toAccount != null && !toAccount.isBlank()) {
            return paymentRepository.findByToAccountId(AccountId.of(toAccount.trim()));
        }

        if (startDate != null || endDate != null) {
            LocalDate start = startDate != null ? startDate.toLocalDate() : endDate.toLocalDate();
            LocalDate end = endDate != null ? endDate.toLocalDate() : startDate.toLocalDate();
            return paymentRepository.findByDateRange(start, end);
        }

        return paymentRepository.findAll();
    }
}
