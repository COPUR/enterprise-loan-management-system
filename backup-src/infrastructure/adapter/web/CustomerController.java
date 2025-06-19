
package com.bank.loanmanagement.infrastructure.adapter.web;

import com.bank.loanmanagement.application.dto.CustomerCreateRequest;
import com.bank.loanmanagement.application.dto.CustomerUpdateRequest;
import com.bank.loanmanagement.application.dto.CustomerResponse;
import com.bank.loanmanagement.application.port.in.CustomerManagementUseCase;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/api/v1/customers")
@RequiredArgsConstructor
@Tag(name = "Customer Management", description = "Customer management operations")
public class CustomerController {
    
    private final CustomerManagementUseCase customerManagementUseCase;
    
    @PostMapping
    @Operation(summary = "Create a new customer")
    public ResponseEntity<CustomerResponse> createCustomer(@Valid @RequestBody CustomerCreateRequest request) {
        CustomerResponse response = customerManagementUseCase.createCustomer(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
    
    @PutMapping("/{customerId}")
    @Operation(summary = "Update customer information")
    public ResponseEntity<CustomerResponse> updateCustomer(
            @PathVariable Long customerId,
            @Valid @RequestBody CustomerUpdateRequest request) {
        CustomerResponse response = customerManagementUseCase.updateCustomer(customerId, request);
        return ResponseEntity.ok(response);
    }
    
    @GetMapping("/{customerId}")
    @Operation(summary = "Get customer by ID")
    public ResponseEntity<CustomerResponse> getCustomerById(@PathVariable Long customerId) {
        CustomerResponse response = customerManagementUseCase.getCustomerById(customerId);
        return ResponseEntity.ok(response);
    }
    
    @GetMapping("/email/{email}")
    @Operation(summary = "Get customer by email")
    public ResponseEntity<CustomerResponse> getCustomerByEmail(@PathVariable String email) {
        CustomerResponse response = customerManagementUseCase.getCustomerByEmail(email);
        return ResponseEntity.ok(response);
    }
    
    @GetMapping
    @Operation(summary = "Get all customers")
    public ResponseEntity<List<CustomerResponse>> getAllCustomers() {
        List<CustomerResponse> response = customerManagementUseCase.getAllCustomers();
        return ResponseEntity.ok(response);
    }
    
    @GetMapping("/active")
    @Operation(summary = "Get all active customers")
    public ResponseEntity<List<CustomerResponse>> getActiveCustomers() {
        List<CustomerResponse> response = customerManagementUseCase.getActiveCustomers();
        return ResponseEntity.ok(response);
    }
    
    @PutMapping("/{customerId}/suspend")
    @Operation(summary = "Suspend customer")
    public ResponseEntity<Void> suspendCustomer(@PathVariable Long customerId) {
        customerManagementUseCase.suspendCustomer(customerId);
        return ResponseEntity.ok().build();
    }
    
    @PutMapping("/{customerId}/activate")
    @Operation(summary = "Activate customer")
    public ResponseEntity<Void> activateCustomer(@PathVariable Long customerId) {
        customerManagementUseCase.activateCustomer(customerId);
        return ResponseEntity.ok().build();
    }
    
    @PutMapping("/{customerId}/block")
    @Operation(summary = "Block customer")
    public ResponseEntity<Void> blockCustomer(@PathVariable Long customerId) {
        customerManagementUseCase.blockCustomer(customerId);
        return ResponseEntity.ok().build();
    }
    
    @PutMapping("/{customerId}/credit-limit")
    @Operation(summary = "Update customer credit limit")
    public ResponseEntity<String> updateCreditLimit(
            @PathVariable Long customerId,
            @RequestParam BigDecimal creditLimit) {
        boolean updated = customerManagementUseCase.updateCreditLimit(customerId, creditLimit);
        if (updated) {
            return ResponseEntity.ok("Credit limit updated successfully");
        } else {
            return ResponseEntity.badRequest().body("Credit limit cannot be set below current usage");
        }
    }
    
    @GetMapping("/{customerId}/available-credit")
    @Operation(summary = "Get customer available credit")
    public ResponseEntity<BigDecimal> getAvailableCredit(@PathVariable Long customerId) {
        BigDecimal availableCredit = customerManagementUseCase.getAvailableCredit(customerId);
        return ResponseEntity.ok(availableCredit);
    }
    
    @DeleteMapping("/{customerId}")
    @Operation(summary = "Delete customer")
    public ResponseEntity<Void> deleteCustomer(@PathVariable Long customerId) {
        customerManagementUseCase.deleteCustomer(customerId);
        return ResponseEntity.noContent().build();
    }
}
