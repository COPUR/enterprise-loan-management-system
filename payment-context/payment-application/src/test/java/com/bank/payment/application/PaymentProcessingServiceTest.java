package com.bank.payment.application;

import com.bank.payment.application.dto.CreatePaymentRequest;
import com.bank.payment.application.dto.PaymentResponse;
import com.bank.payment.domain.*;
import com.bank.shared.kernel.domain.CustomerId;
import com.bank.shared.kernel.domain.Money;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Currency;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * TDD Test Suite for Payment Processing Service
 * 
 * Tests Functional Requirements:
 * - FR-009: Payment Processing & Settlement
 * - FR-010: Payment Validation & Compliance
 * - FR-011: Payment Status Management
 * - FR-012: Payment Reconciliation
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("Payment Processing Service Tests")
class PaymentProcessingServiceTest {
    
    @Mock
    private PaymentRepository paymentRepository;
    
    @Mock
    private AccountValidationService accountValidationService;
    
    @Mock
    private FraudDetectionService fraudDetectionService;
    
    @Mock
    private ComplianceService complianceService;
    
    private PaymentProcessingService paymentService;
    
    @BeforeEach
    void setUp() {
        paymentService = new PaymentProcessingService(
            paymentRepository, 
            accountValidationService, 
            fraudDetectionService,
            complianceService
        );
    }
    
    @Test
    @DisplayName("FR-009: Should process valid payment successfully")
    void shouldProcessValidPaymentSuccessfully() {
        // Given
        CreatePaymentRequest request = new CreatePaymentRequest(
            "CUST-12345678",
            "ACC-11111111",
            "ACC-22222222", 
            BigDecimal.valueOf(1000),
            "USD",
            PaymentType.TRANSFER,
            "Monthly loan payment"
        );
        
        PaymentId expectedPaymentId = PaymentId.generate();
        Payment expectedPayment = Payment.create(
            expectedPaymentId,
            CustomerId.of(request.customerId()),
            AccountId.of(request.fromAccountId()),
            AccountId.of(request.toAccountId()),
            request.getAmountAsMoney(),
            request.paymentType(),
            request.description()
        );
        
        when(accountValidationService.validateAccount(AccountId.of(request.fromAccountId()))).thenReturn(true);
        when(accountValidationService.validateAccount(AccountId.of(request.toAccountId()))).thenReturn(true);
        when(accountValidationService.hasBalance(AccountId.of(request.fromAccountId()), request.getAmountAsMoney())).thenReturn(true);
        when(fraudDetectionService.isValidPayment(any(Payment.class))).thenReturn(true);
        when(complianceService.validatePayment(any(Payment.class))).thenReturn(true);
        when(paymentRepository.save(any(Payment.class))).thenReturn(expectedPayment);
        
        // When
        PaymentResponse response = paymentService.processPayment(request);
        
        // Then
        assertThat(response).isNotNull();
        assertThat(response.customerId()).isEqualTo("CUST-12345678");
        assertThat(response.fromAccountId()).isEqualTo("ACC-11111111");
        assertThat(response.toAccountId()).isEqualTo("ACC-22222222");
        assertThat(response.amount()).isEqualByComparingTo(BigDecimal.valueOf(1000));
        assertThat(response.status()).isEqualTo("PENDING");
        assertThat(response.paymentType()).isEqualTo("TRANSFER");
        
        verify(accountValidationService).validateAccount(AccountId.of(request.fromAccountId()));
        verify(accountValidationService).validateAccount(AccountId.of(request.toAccountId()));
        verify(accountValidationService).hasBalance(AccountId.of(request.fromAccountId()), request.getAmountAsMoney());
        verify(fraudDetectionService).isValidPayment(any(Payment.class));
        verify(complianceService).validatePayment(any(Payment.class));
        verify(paymentRepository).save(any(Payment.class));
    }
    
    @Test
    @DisplayName("FR-009: Should reject payment with invalid from account")
    void shouldRejectPaymentWithInvalidFromAccount() {
        // Given
        CreatePaymentRequest request = new CreatePaymentRequest(
            "CUST-12345678",
            "ACC-INVALID",
            "ACC-22222222", 
            BigDecimal.valueOf(500),
            "USD",
            PaymentType.TRANSFER,
            "Invalid account payment"
        );
        
        when(accountValidationService.validateAccount(AccountId.of(request.fromAccountId()))).thenReturn(false);
        
        // When & Then
        assertThatThrownBy(() -> paymentService.processPayment(request))
            .isInstanceOf(InvalidAccountException.class)
            .hasMessageContaining("Invalid from account");
        
        verify(accountValidationService).validateAccount(AccountId.of(request.fromAccountId()));
        verify(paymentRepository, never()).save(any(Payment.class));
    }
    
    @Test
    @DisplayName("FR-009: Should reject payment with insufficient balance")
    void shouldRejectPaymentWithInsufficientBalance() {
        // Given
        CreatePaymentRequest request = new CreatePaymentRequest(
            "CUST-12345678",
            "ACC-11111111",
            "ACC-22222222", 
            BigDecimal.valueOf(10000), // Large amount
            "USD",
            PaymentType.TRANSFER,
            "Insufficient balance payment"
        );
        
        when(accountValidationService.validateAccount(AccountId.of(request.fromAccountId()))).thenReturn(true);
        when(accountValidationService.validateAccount(AccountId.of(request.toAccountId()))).thenReturn(true);
        when(accountValidationService.hasBalance(AccountId.of(request.fromAccountId()), request.getAmountAsMoney())).thenReturn(false);
        when(accountValidationService.getAccountBalance(AccountId.of(request.fromAccountId()))).thenReturn(Money.usd(BigDecimal.valueOf(5000)));
        
        // When & Then
        assertThatThrownBy(() -> paymentService.processPayment(request))
            .isInstanceOf(InsufficientBalanceException.class)
            .hasMessageContaining("insufficient balance");
        
        verify(accountValidationService).validateAccount(AccountId.of(request.fromAccountId()));
        verify(accountValidationService).validateAccount(AccountId.of(request.toAccountId()));
        verify(accountValidationService).hasBalance(AccountId.of(request.fromAccountId()), request.getAmountAsMoney());
        verify(paymentRepository, never()).save(any(Payment.class));
    }
    
    @Test
    @DisplayName("FR-010: Should reject payment flagged by fraud detection")
    void shouldRejectPaymentFlaggedByFraudDetection() {
        // Given
        CreatePaymentRequest request = new CreatePaymentRequest(
            "CUST-12345678",
            "ACC-11111111",
            "ACC-22222222", 
            BigDecimal.valueOf(2000),
            "USD",
            PaymentType.TRANSFER,
            "Suspicious payment"
        );
        
        when(accountValidationService.validateAccount(AccountId.of(request.fromAccountId()))).thenReturn(true);
        when(accountValidationService.validateAccount(AccountId.of(request.toAccountId()))).thenReturn(true);
        when(accountValidationService.hasBalance(AccountId.of(request.fromAccountId()), request.getAmountAsMoney())).thenReturn(true);
        when(fraudDetectionService.isValidPayment(any(Payment.class))).thenReturn(false);
        
        // When & Then
        assertThatThrownBy(() -> paymentService.processPayment(request))
            .isInstanceOf(FraudDetectedException.class)
            .hasMessageContaining("fraud");
        
        verify(fraudDetectionService).isValidPayment(any(Payment.class));
        verify(paymentRepository, never()).save(any(Payment.class));
    }
    
    @Test
    @DisplayName("FR-010: Should reject payment that fails compliance check")
    void shouldRejectPaymentThatFailsComplianceCheck() {
        // Given
        CreatePaymentRequest request = new CreatePaymentRequest(
            "CUST-12345678",
            "ACC-11111111",
            "ACC-22222222", 
            BigDecimal.valueOf(15000), // Large amount triggering compliance
            "USD",
            PaymentType.TRANSFER,
            "Large compliance payment"
        );
        
        when(accountValidationService.validateAccount(AccountId.of(request.fromAccountId()))).thenReturn(true);
        when(accountValidationService.validateAccount(AccountId.of(request.toAccountId()))).thenReturn(true);
        when(accountValidationService.hasBalance(AccountId.of(request.fromAccountId()), request.getAmountAsMoney())).thenReturn(true);
        when(fraudDetectionService.isValidPayment(any(Payment.class))).thenReturn(true);
        when(complianceService.validatePayment(any(Payment.class))).thenReturn(false);
        
        // When & Then
        assertThatThrownBy(() -> paymentService.processPayment(request))
            .isInstanceOf(ComplianceViolationException.class)
            .hasMessageContaining("compliance");
        
        verify(complianceService).validatePayment(any(Payment.class));
        verify(paymentRepository, never()).save(any(Payment.class));
    }
    
    @Test
    @DisplayName("FR-011: Should confirm pending payment")
    void shouldConfirmPendingPayment() {
        // Given
        PaymentId paymentId = PaymentId.of("PAY-12345678");
        Payment payment = Payment.create(
            paymentId,
            CustomerId.of("CUST-12345678"),
            AccountId.of("ACC-11111111"),
            AccountId.of("ACC-22222222"),
            Money.usd(BigDecimal.valueOf(750)),
            PaymentType.TRANSFER,
            "Payment confirmation test"
        );
        
        when(paymentRepository.findById(paymentId)).thenReturn(Optional.of(payment));
        when(paymentRepository.save(any(Payment.class))).thenAnswer(invocation -> invocation.getArgument(0));
        
        // When
        PaymentResponse response = paymentService.confirmPayment(paymentId.getValue());
        
        // Then
        assertThat(response).isNotNull();
        assertThat(response.status()).isEqualTo("COMPLETED");
        assertThat(response.completedAt()).isNotNull();
        
        verify(paymentRepository).findById(paymentId);
        verify(paymentRepository).save(any(Payment.class));
    }
    
    @Test
    @DisplayName("FR-011: Should fail pending payment")
    void shouldFailPendingPayment() {
        // Given
        PaymentId paymentId = PaymentId.of("PAY-12345678");
        Payment payment = Payment.create(
            paymentId,
            CustomerId.of("CUST-12345678"),
            AccountId.of("ACC-11111111"),
            AccountId.of("ACC-22222222"),
            Money.usd(BigDecimal.valueOf(750)),
            PaymentType.TRANSFER,
            "Payment failure test"
        );
        
        String failureReason = "Network timeout during processing";
        
        when(paymentRepository.findById(paymentId)).thenReturn(Optional.of(payment));
        when(paymentRepository.save(any(Payment.class))).thenAnswer(invocation -> invocation.getArgument(0));
        
        // When
        PaymentResponse response = paymentService.failPayment(paymentId.getValue(), failureReason);
        
        // Then
        assertThat(response).isNotNull();
        assertThat(response.status()).isEqualTo("FAILED");
        assertThat(response.failureReason()).isEqualTo(failureReason);
        
        verify(paymentRepository).findById(paymentId);
        verify(paymentRepository).save(any(Payment.class));
    }
    
    @Test
    @DisplayName("FR-011: Should throw exception when payment not found")
    void shouldThrowExceptionWhenPaymentNotFound() {
        // Given
        PaymentId nonExistentId = PaymentId.of("PAY-99999999");
        when(paymentRepository.findById(nonExistentId)).thenReturn(Optional.empty());
        
        // When & Then
        assertThatThrownBy(() -> paymentService.confirmPayment(nonExistentId.getValue()))
            .isInstanceOf(PaymentNotFoundException.class)
            .hasMessage("Payment not found with ID: PAY-99999999");
        
        verify(paymentRepository).findById(nonExistentId);
    }
    
    @Test
    @DisplayName("FR-012: Should find payment by ID")
    void shouldFindPaymentById() {
        // Given
        PaymentId paymentId = PaymentId.of("PAY-12345678");
        Payment payment = Payment.create(
            paymentId,
            CustomerId.of("CUST-12345678"),
            AccountId.of("ACC-11111111"),
            AccountId.of("ACC-22222222"),
            Money.usd(BigDecimal.valueOf(1250)),
            PaymentType.WIRE_TRANSFER,
            "Wire transfer payment"
        );
        
        when(paymentRepository.findById(paymentId)).thenReturn(Optional.of(payment));
        
        // When
        PaymentResponse response = paymentService.findPaymentById(paymentId.getValue());
        
        // Then
        assertThat(response).isNotNull();
        assertThat(response.paymentId()).isEqualTo(paymentId.getValue());
        assertThat(response.customerId()).isEqualTo("CUST-12345678");
        assertThat(response.amount()).isEqualByComparingTo(BigDecimal.valueOf(1250));
        assertThat(response.paymentType()).isEqualTo("WIRE_TRANSFER");
        
        verify(paymentRepository).findById(paymentId);
    }
    
    @Test
    @DisplayName("FR-009: Should reject payment with invalid amount")
    void shouldRejectPaymentWithInvalidAmount() {
        // Given
        CreatePaymentRequest invalidRequest = new CreatePaymentRequest(
            "CUST-12345678",
            "ACC-11111111",
            "ACC-22222222",
            BigDecimal.valueOf(-100), // Negative amount
            "USD",
            PaymentType.TRANSFER,
            "Invalid amount payment"
        );
        
        // When & Then
        assertThatThrownBy(() -> paymentService.processPayment(invalidRequest))
            .isInstanceOf(IllegalArgumentException.class);
        
        verify(paymentRepository, never()).save(any(Payment.class));
    }
    
    @Test
    @DisplayName("FR-012: Should calculate payment fees correctly")
    void shouldCalculatePaymentFeesCorrectly() {
        // Given
        CreatePaymentRequest request = new CreatePaymentRequest(
            "CUST-12345678",
            "ACC-11111111",
            "ACC-22222222", 
            BigDecimal.valueOf(5000),
            "USD",
            PaymentType.WIRE_TRANSFER, // Higher fee type
            "Wire transfer with fees"
        );
        
        PaymentId expectedPaymentId = PaymentId.generate();
        Payment expectedPayment = Payment.create(
            expectedPaymentId,
            CustomerId.of(request.customerId()),
            AccountId.of(request.fromAccountId()),
            AccountId.of(request.toAccountId()),
            request.getAmountAsMoney(),
            request.paymentType(),
            request.description()
        );
        
        when(accountValidationService.validateAccount(AccountId.of(request.fromAccountId()))).thenReturn(true);
        when(accountValidationService.validateAccount(AccountId.of(request.toAccountId()))).thenReturn(true);
        when(accountValidationService.hasBalance(AccountId.of(request.fromAccountId()), request.getAmountAsMoney())).thenReturn(true);
        when(fraudDetectionService.isValidPayment(any(Payment.class))).thenReturn(true);
        when(complianceService.validatePayment(any(Payment.class))).thenReturn(true);
        when(paymentRepository.save(any(Payment.class))).thenReturn(expectedPayment);
        
        // When
        PaymentResponse response = paymentService.processPayment(request);
        
        // Then
        assertThat(response).isNotNull();
        assertThat(response.fee()).isGreaterThan(BigDecimal.ZERO); // Wire transfers have fees
        assertThat(response.totalAmount()).isEqualByComparingTo(response.amount().add(response.fee()));
        
        verify(paymentRepository).save(any(Payment.class));
    }
}