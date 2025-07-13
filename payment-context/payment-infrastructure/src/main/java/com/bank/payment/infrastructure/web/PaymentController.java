package com.bank.payment.infrastructure.web;

import com.bank.payment.application.PaymentProcessingService;
import com.bank.payment.application.dto.CreatePaymentRequest;
import com.bank.payment.application.dto.PaymentResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * REST Controller for Payment Processing
 * 
 * Implements Hexagonal Architecture - Adapter for HTTP requests
 * Functional Requirements: FR-009 through FR-012
 */
@RestController
@RequestMapping("/api/v1/payments")
public class PaymentController {
    
    private final PaymentProcessingService paymentService;
    
    public PaymentController(PaymentProcessingService paymentService) {
        this.paymentService = paymentService;
    }
    
    /**
     * FR-009: Process payment
     */
    @PostMapping
    @PreAuthorize("hasAnyRole('CUSTOMER', 'BANKER', 'ADMIN')")
    public ResponseEntity<PaymentResponse> processPayment(@Valid @RequestBody CreatePaymentRequest request) {
        PaymentResponse response = paymentService.processPayment(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
    
    /**
     * FR-012: Get payment by ID
     */
    @GetMapping("/{paymentId}")
    @PreAuthorize("hasAnyRole('CUSTOMER', 'BANKER', 'ADMIN')")
    public ResponseEntity<PaymentResponse> getPayment(@PathVariable String paymentId) {
        PaymentResponse response = paymentService.findPaymentById(paymentId);
        return ResponseEntity.ok(response);
    }
    
    /**
     * FR-011: Confirm payment
     */
    @PostMapping("/{paymentId}/confirm")
    @PreAuthorize("hasAnyRole('BANKER', 'ADMIN')")
    public ResponseEntity<PaymentResponse> confirmPayment(@PathVariable String paymentId) {
        PaymentResponse response = paymentService.confirmPayment(paymentId);
        return ResponseEntity.ok(response);
    }
    
    /**
     * FR-011: Fail payment
     */
    @PostMapping("/{paymentId}/fail")
    @PreAuthorize("hasAnyRole('BANKER', 'ADMIN')")
    public ResponseEntity<PaymentResponse> failPayment(
            @PathVariable String paymentId,
            @RequestBody FailPaymentRequest request) {
        PaymentResponse response = paymentService.failPayment(paymentId, request.reason());
        return ResponseEntity.ok(response);
    }
    
    /**
     * FR-011: Cancel payment
     */
    @PostMapping("/{paymentId}/cancel")
    @PreAuthorize("hasAnyRole('CUSTOMER', 'BANKER', 'ADMIN')")
    public ResponseEntity<PaymentResponse> cancelPayment(
            @PathVariable String paymentId,
            @RequestBody CancelPaymentRequest request) {
        PaymentResponse response = paymentService.cancelPayment(paymentId, request.reason());
        return ResponseEntity.ok(response);
    }
    
    /**
     * FR-011: Refund payment
     */
    @PostMapping("/{paymentId}/refund")
    @PreAuthorize("hasAnyRole('BANKER', 'ADMIN')")
    public ResponseEntity<PaymentResponse> refundPayment(@PathVariable String paymentId) {
        PaymentResponse response = paymentService.refundPayment(paymentId);
        return ResponseEntity.ok(response);
    }
    
    // Request DTOs
    public record FailPaymentRequest(String reason) {}
    public record CancelPaymentRequest(String reason) {}
}