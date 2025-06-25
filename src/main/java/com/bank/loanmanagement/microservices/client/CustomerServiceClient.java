package com.bank.loanmanagement.microservices.client;

import com.bank.loanmanagement.customermanagement.domain.model.Customer;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.ResponseEntity;

import java.util.Optional;

@FeignClient(
    name = "customer-service",
    url = "${service.communication.customer-service.url:http://customer-service:8081}",
    fallback = CustomerServiceClientFallback.class
)
public interface CustomerServiceClient {

    @GetMapping("/api/v1/customers/{customerId}")
    ResponseEntity<Customer> getCustomer(@PathVariable("customerId") String customerId);

    @GetMapping("/api/v1/customers/{customerId}/credit-limit")
    ResponseEntity<Double> getCreditLimit(@PathVariable("customerId") String customerId);

    @PostMapping("/api/v1/customers/{customerId}/credit-limit/reserve")
    ResponseEntity<Boolean> reserveCreditLimit(
        @PathVariable("customerId") String customerId, 
        @RequestParam("amount") Double amount
    );

    @PostMapping("/api/v1/customers/{customerId}/credit-limit/release")
    ResponseEntity<Boolean> releaseCreditLimit(
        @PathVariable("customerId") String customerId, 
        @RequestParam("amount") Double amount
    );

    @GetMapping("/api/v1/customers/{customerId}/eligibility")
    ResponseEntity<Boolean> checkLoanEligibility(@PathVariable("customerId") String customerId);
}