package com.bank.loanmanagement.microservices.client;

import com.bank.loanmanagement.domain.payment.Payment;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.ResponseEntity;

@FeignClient(
    name = "payment-service",
    url = "${service.communication.payment-service.url:http://payment-service:8083}",
    fallback = PaymentServiceClientFallback.class
)
public interface PaymentServiceClient {

    @PostMapping("/api/v1/payments/process")
    ResponseEntity<Payment> processPayment(@RequestBody Payment payment);

    @PostMapping("/api/v1/payments/{paymentId}/reverse")
    ResponseEntity<Boolean> reversePayment(@PathVariable("paymentId") String paymentId);

    @GetMapping("/api/v1/payments/{paymentId}")
    ResponseEntity<Payment> getPayment(@PathVariable("paymentId") String paymentId);

    @GetMapping("/api/v1/payments/loan/{loanId}")
    ResponseEntity<Payment[]> getPaymentsByLoanId(@PathVariable("loanId") String loanId);
}