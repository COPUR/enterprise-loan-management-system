package com.bank.payment.application;

import com.bank.payment.application.dto.CreatePaymentRequest;
import com.bank.payment.application.dto.PaymentResponse;
import com.bank.payment.domain.AccountId;
import com.bank.payment.domain.LoanId;
import com.bank.payment.domain.Payment;
import com.bank.payment.domain.PaymentId;
import com.bank.payment.domain.PaymentRepository;
import com.bank.payment.domain.PaymentStatus;
import com.bank.payment.domain.PaymentType;
import com.bank.shared.kernel.domain.CustomerId;
import com.bank.shared.kernel.domain.Money;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PaymentProcessingServiceTest {

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private AccountValidationService accountValidationService;

    @Mock
    private FraudDetectionService fraudDetectionService;

    @Mock
    private ComplianceService complianceService;

    @InjectMocks
    private PaymentProcessingService service;

    @Test
    void processPaymentShouldPersistWhenAllChecksPass() {
        CreatePaymentRequest request = request("CUST-001", "ACC-1", "ACC-2", "500.00");
        when(accountValidationService.validateAccount(AccountId.of("ACC-1"))).thenReturn(true);
        when(accountValidationService.validateAccount(AccountId.of("ACC-2"))).thenReturn(true);
        when(accountValidationService.hasBalance(AccountId.of("ACC-1"), request.getAmountAsMoney())).thenReturn(true);
        when(fraudDetectionService.isValidPayment(any(Payment.class))).thenReturn(true);
        when(complianceService.validatePayment(any(Payment.class))).thenReturn(true);
        when(paymentRepository.save(any(Payment.class))).thenAnswer(invocation -> invocation.getArgument(0));

        PaymentResponse response = service.processPayment(request);

        assertThat(response.customerId()).isEqualTo("CUST-001");
        assertThat(response.status()).isEqualTo("PENDING");
        verify(paymentRepository).save(any(Payment.class));
    }

    @Test
    void processPaymentShouldRejectInvalidAccounts() {
        CreatePaymentRequest request = request("CUST-002", "ACC-X", "ACC-Y", "100.00");
        when(accountValidationService.validateAccount(AccountId.of("ACC-X"))).thenReturn(false);

        assertThatThrownBy(() -> service.processPayment(request))
            .isInstanceOf(InvalidAccountException.class)
            .hasMessageContaining("from account");

        when(accountValidationService.validateAccount(AccountId.of("ACC-X"))).thenReturn(true);
        when(accountValidationService.validateAccount(AccountId.of("ACC-Y"))).thenReturn(false);

        assertThatThrownBy(() -> service.processPayment(request))
            .isInstanceOf(InvalidAccountException.class)
            .hasMessageContaining("to account");
    }

    @Test
    void processPaymentShouldRejectInsufficientBalanceFraudAndCompliance() {
        CreatePaymentRequest request = request("CUST-003", "ACC-A", "ACC-B", "1000.00");
        when(accountValidationService.validateAccount(AccountId.of("ACC-A"))).thenReturn(true);
        when(accountValidationService.validateAccount(AccountId.of("ACC-B"))).thenReturn(true);
        when(accountValidationService.hasBalance(AccountId.of("ACC-A"), request.getAmountAsMoney())).thenReturn(false);
        when(accountValidationService.getAccountBalance(AccountId.of("ACC-A"))).thenReturn(Money.aed(new BigDecimal("10.00")));

        assertThatThrownBy(() -> service.processPayment(request))
            .isInstanceOf(InsufficientBalanceException.class);

        when(accountValidationService.hasBalance(AccountId.of("ACC-A"), request.getAmountAsMoney())).thenReturn(true);
        when(fraudDetectionService.isValidPayment(any(Payment.class))).thenReturn(false);
        assertThatThrownBy(() -> service.processPayment(request))
            .isInstanceOf(FraudDetectedException.class);

        when(fraudDetectionService.isValidPayment(any(Payment.class))).thenReturn(true);
        when(complianceService.validatePayment(any(Payment.class))).thenReturn(false);
        assertThatThrownBy(() -> service.processPayment(request))
            .isInstanceOf(ComplianceViolationException.class);
    }

    @Test
    void lifecycleOperationsShouldUpdatePaymentStatus() {
        Payment confirmable = regularPayment("PAY-SVC-001", "250.00");
        when(paymentRepository.findById(PaymentId.of("PAY-SVC-001"))).thenReturn(Optional.of(confirmable));
        when(paymentRepository.save(confirmable)).thenReturn(confirmable);
        PaymentResponse confirmed = service.confirmPayment("PAY-SVC-001");
        assertThat(confirmed.status()).isEqualTo("COMPLETED");

        Payment failable = regularPayment("PAY-SVC-002", "250.00");
        when(paymentRepository.findById(PaymentId.of("PAY-SVC-002"))).thenReturn(Optional.of(failable));
        when(paymentRepository.save(failable)).thenReturn(failable);
        PaymentResponse failed = service.failPayment("PAY-SVC-002", "timeout");
        assertThat(failed.status()).isEqualTo("FAILED");

        Payment cancellable = regularPayment("PAY-SVC-003", "250.00");
        when(paymentRepository.findById(PaymentId.of("PAY-SVC-003"))).thenReturn(Optional.of(cancellable));
        when(paymentRepository.save(cancellable)).thenReturn(cancellable);
        PaymentResponse cancelled = service.cancelPayment("PAY-SVC-003", "user");
        assertThat(cancelled.status()).isEqualTo("CANCELLED");

        Payment refundable = regularPayment("PAY-SVC-004", "250.00");
        refundable.confirm();
        when(paymentRepository.findById(PaymentId.of("PAY-SVC-004"))).thenReturn(Optional.of(refundable));
        when(paymentRepository.save(refundable)).thenReturn(refundable);
        PaymentResponse refunded = service.refundPayment("PAY-SVC-004");
        assertThat(refunded.status()).isEqualTo("REFUNDED");
    }

    @Test
    void lifecycleOperationsShouldThrowWhenPaymentMissing() {
        when(paymentRepository.findById(any(PaymentId.class))).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.confirmPayment("MISSING")).isInstanceOf(PaymentNotFoundException.class);
        assertThatThrownBy(() -> service.failPayment("MISSING", "x")).isInstanceOf(PaymentNotFoundException.class);
        assertThatThrownBy(() -> service.cancelPayment("MISSING", "x")).isInstanceOf(PaymentNotFoundException.class);
        assertThatThrownBy(() -> service.findPaymentById("MISSING")).isInstanceOf(PaymentNotFoundException.class);
        assertThatThrownBy(() -> service.refundPayment("MISSING")).isInstanceOf(PaymentNotFoundException.class);
        assertThatThrownBy(() -> service.getCustomerIdForPayment("MISSING")).isInstanceOf(PaymentNotFoundException.class);
    }

    @Test
    void findPaymentAndCustomerIdShouldReturnMappedValues() {
        Payment payment = regularPayment("PAY-SVC-005", "320.00");
        when(paymentRepository.findById(PaymentId.of("PAY-SVC-005"))).thenReturn(Optional.of(payment));

        PaymentResponse response = service.findPaymentById("PAY-SVC-005");
        String customerId = service.getCustomerIdForPayment("PAY-SVC-005");

        assertThat(response.paymentId()).isEqualTo("PAY-SVC-005");
        assertThat(customerId).isEqualTo(payment.getCustomerId().getValue());
    }

    @Test
    void retryPaymentShouldCreateNewPaymentForFailedAccountBasedPayments() {
        Payment failed = regularPayment("PAY-SVC-006", "700.00");
        failed.fail("temporary outage");
        when(paymentRepository.findById(PaymentId.of("PAY-SVC-006"))).thenReturn(Optional.of(failed));
        when(paymentRepository.save(any(Payment.class))).thenAnswer(invocation -> invocation.getArgument(0));

        PaymentResponse retried = service.retryPayment("PAY-SVC-006");

        assertThat(retried.paymentId()).startsWith("PAY-");
        assertThat(retried.paymentId()).isNotEqualTo("PAY-SVC-006");
        assertThat(retried.status()).isEqualTo("PENDING");

        ArgumentCaptor<Payment> captor = ArgumentCaptor.forClass(Payment.class);
        verify(paymentRepository).save(captor.capture());
        assertThat(captor.getValue().getFromAccountId()).isEqualTo(failed.getFromAccountId());
    }

    @Test
    void retryPaymentShouldRejectNonFailedAndNonAccountBasedPayments() {
        Payment nonFailed = regularPayment("PAY-SVC-007", "500.00");
        when(paymentRepository.findById(PaymentId.of("PAY-SVC-007"))).thenReturn(Optional.of(nonFailed));

        assertThatThrownBy(() -> service.retryPayment("PAY-SVC-007"))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("not in FAILED status");

        Payment failedLoanPayment = Payment.createLoanPayment(
            PaymentId.of("PAY-SVC-008"),
            CustomerId.of("CUST-SVC-008"),
            LoanId.of("LOAN-SVC-008"),
            Money.aed(new BigDecimal("1000.00")),
            LocalDate.now()
        );
        failedLoanPayment.markAsFailed("late");
        when(paymentRepository.findById(PaymentId.of("PAY-SVC-008"))).thenReturn(Optional.of(failedLoanPayment));

        assertThatThrownBy(() -> service.retryPayment("PAY-SVC-008"))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("account-based");
    }

    @Test
    void searchPaymentsShouldHandleInvalidStatusAndPaging() {
        Page<PaymentResponse> invalid = service.searchPayments(
            null,
            "not-a-status",
            null,
            null,
            null,
            null,
            null,
            null,
            PageRequest.of(0, 10)
        );

        assertThat(invalid.getTotalElements()).isZero();
        verify(paymentRepository, never()).findAll();
    }

    @Test
    void searchPaymentsShouldApplyFilteringAcrossBranches() {
        Payment p1 = regularPayment("PAY-SVC-009", "100.00");
        Payment p2 = regularPayment("PAY-SVC-010", "500.00");
        p2.markAsProcessing();
        p2.confirm();

        when(paymentRepository.findByCustomerId(CustomerId.of("CUST-SVC-009"))).thenReturn(List.of(p1, p2));
        when(paymentRepository.findByStatus(PaymentStatus.COMPLETED)).thenReturn(List.of(p2));
        when(paymentRepository.findByFromAccountId(AccountId.of("ACC-FROM-SVC-010"))).thenReturn(List.of(p2));
        when(paymentRepository.findByToAccountId(AccountId.of("ACC-TO-SVC-010"))).thenReturn(List.of(p2));
        when(paymentRepository.findByDateRange(any(LocalDate.class), any(LocalDate.class))).thenReturn(List.of(p1, p2));
        when(paymentRepository.findAll()).thenReturn(List.of(p1, p2));

        Page<PaymentResponse> byCustomer = service.searchPayments(
            "CUST-SVC-009", null, null, null, null, null, null, null, PageRequest.of(0, 10)
        );
        assertThat(byCustomer.getTotalElements()).isEqualTo(1);

        Page<PaymentResponse> byStatus = service.searchPayments(
            null, "completed", null, null, null, null, null, null, PageRequest.of(0, 10)
        );
        assertThat(byStatus.getTotalElements()).isEqualTo(1);
        assertThat(byStatus.getContent().get(0).status()).isEqualTo("COMPLETED");

        Page<PaymentResponse> byFrom = service.searchPayments(
            null, null, "ACC-FROM-SVC-010", null, null, null, null, null, PageRequest.of(0, 10)
        );
        assertThat(byFrom.getTotalElements()).isEqualTo(1);

        Page<PaymentResponse> byTo = service.searchPayments(
            null, null, null, "ACC-TO-SVC-010", null, null, null, null, PageRequest.of(0, 10)
        );
        assertThat(byTo.getTotalElements()).isEqualTo(1);

        Page<PaymentResponse> byDate = service.searchPayments(
            null,
            null,
            null,
            null,
            null,
            null,
            LocalDateTime.now().minusDays(1),
            LocalDateTime.now().plusDays(1),
            PageRequest.of(0, 10)
        );
        assertThat(byDate.getTotalElements()).isEqualTo(2);

        Page<PaymentResponse> filteredAndPaged = service.searchPayments(
            null,
            null,
            null,
            null,
            new BigDecimal("200.00"),
            new BigDecimal("600.00"),
            null,
            null,
            PageRequest.of(0, 1)
        );
        assertThat(filteredAndPaged.getTotalElements()).isEqualTo(1);
        assertThat(filteredAndPaged.getContent()).hasSize(1);
    }

    private static CreatePaymentRequest request(String customerId, String from, String to, String amount) {
        return new CreatePaymentRequest(
            customerId,
            from,
            to,
            new BigDecimal(amount),
            "AED",
            PaymentType.TRANSFER,
            "Test"
        );
    }

    private static Payment regularPayment(String paymentId, String amount) {
        String suffix = paymentId.substring(4);
        return Payment.create(
            PaymentId.of(paymentId),
            CustomerId.of("CUST-" + suffix),
            AccountId.of("ACC-FROM-" + suffix),
            AccountId.of("ACC-TO-" + suffix),
            Money.aed(new BigDecimal(amount)),
            PaymentType.TRANSFER,
            "Payment " + paymentId
        );
    }
}
