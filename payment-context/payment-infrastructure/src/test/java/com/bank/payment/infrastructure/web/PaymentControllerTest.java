package com.bank.payment.infrastructure.web;

import com.bank.payment.application.PaymentProcessingService;
import com.bank.payment.application.dto.CreatePaymentRequest;
import com.bank.payment.application.dto.PaymentResponse;
import com.bank.payment.domain.PaymentType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PaymentControllerTest {

    @Mock
    private PaymentProcessingService paymentService;

    @InjectMocks
    private PaymentController controller;

    @Test
    void processPaymentShouldReturnCreated() {
        CreatePaymentRequest request = new CreatePaymentRequest(
            "CUST-CTRL-001",
            "ACC-FROM-001",
            "ACC-TO-001",
            new BigDecimal("250.00"),
            "AED",
            PaymentType.TRANSFER,
            "Payment"
        );
        PaymentResponse response = sampleResponse("PAY-CTRL-001", "PENDING");
        when(paymentService.processPayment(request)).thenReturn(response);

        ResponseEntity<PaymentResponse> entity = controller.processPayment(request);

        assertThat(entity.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(entity.getBody()).isEqualTo(response);
    }

    @Test
    void getConfirmAndRefundShouldReturnOk() {
        when(paymentService.findPaymentById("PAY-CTRL-002")).thenReturn(sampleResponse("PAY-CTRL-002", "PENDING"));
        when(paymentService.confirmPayment("PAY-CTRL-002")).thenReturn(sampleResponse("PAY-CTRL-002", "COMPLETED"));
        when(paymentService.refundPayment("PAY-CTRL-002")).thenReturn(sampleResponse("PAY-CTRL-002", "REFUNDED"));

        assertThat(controller.getPayment("PAY-CTRL-002").getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(controller.confirmPayment("PAY-CTRL-002").getBody().status()).isEqualTo("COMPLETED");
        assertThat(controller.refundPayment("PAY-CTRL-002").getBody().status()).isEqualTo("REFUNDED");
    }

    @Test
    void failAndCancelShouldDelegateReason() {
        when(paymentService.failPayment("PAY-CTRL-003", "timeout"))
            .thenReturn(sampleResponse("PAY-CTRL-003", "FAILED"));
        when(paymentService.cancelPayment("PAY-CTRL-003", "user"))
            .thenReturn(sampleResponse("PAY-CTRL-003", "CANCELLED"));

        ResponseEntity<PaymentResponse> failed = controller.failPayment(
            "PAY-CTRL-003",
            new PaymentController.FailPaymentRequest("timeout")
        );
        ResponseEntity<PaymentResponse> cancelled = controller.cancelPayment(
            "PAY-CTRL-003",
            new PaymentController.CancelPaymentRequest("user")
        );

        assertThat(failed.getBody().status()).isEqualTo("FAILED");
        assertThat(cancelled.getBody().status()).isEqualTo("CANCELLED");
    }

    private static PaymentResponse sampleResponse(String paymentId, String status) {
        return new PaymentResponse(
            paymentId,
            "CUST-CTRL-001",
            "ACC-FROM-001",
            "ACC-TO-001",
            new BigDecimal("250.00"),
            new BigDecimal("0.00"),
            new BigDecimal("250.00"),
            "AED",
            "TRANSFER",
            status,
            "Payment",
            status.equals("FAILED") ? "timeout" : null,
            LocalDateTime.parse("2026-01-01T00:00:00"),
            status.equals("PENDING") ? null : LocalDateTime.parse("2026-01-01T00:05:00"),
            Instant.parse("2026-01-01T00:05:00Z")
        );
    }
}
