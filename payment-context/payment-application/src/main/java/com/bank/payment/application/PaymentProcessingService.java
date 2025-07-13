package com.bank.payment.application;

import com.bank.payment.application.dto.CreatePaymentRequest;
import com.bank.payment.application.dto.PaymentResponse;
import com.bank.payment.domain.*;
import com.bank.shared.kernel.domain.CustomerId;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Application Service for Payment Processing
 * 
 * Implements functional requirements:
 * - FR-009: Payment Processing & Settlement
 * - FR-010: Payment Validation & Compliance
 * - FR-011: Payment Status Management
 * - FR-012: Payment Reconciliation
 */
@Service
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
}