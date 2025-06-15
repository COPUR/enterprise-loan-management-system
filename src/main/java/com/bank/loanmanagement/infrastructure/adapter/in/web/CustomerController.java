
package com.bank.loanmanagement.infrastructure.adapter.in.web;

import com.bank.loanmanagement.application.service.CustomerAlreadyExistsException;
import com.bank.loanmanagement.application.service.CustomerNotFoundException;
import com.bank.loanmanagement.domain.model.Customer;
import com.bank.loanmanagement.domain.port.in.CreateCustomerCommand;
import com.bank.loanmanagement.domain.port.in.CustomerManagementUseCase;
import com.bank.loanmanagement.domain.port.in.UpdateCustomerCommand;
import com.bank.loanmanagement.infrastructure.adapter.in.web.dto.CreateCustomerRequest;
import com.bank.loanmanagement.infrastructure.adapter.in.web.dto.CustomerResponse;
import com.bank.loanmanagement.infrastructure.adapter.in.web.dto.UpdateCustomerRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.math.BigDecimal;
import java.util.List;

/**
 * REST controller for customer management operations.
 * Adapter that translates HTTP requests to domain use cases.
 */
@RestController
@RequestMapping("/api/v1/customers")
@CrossOrigin(origins = "*")
public class CustomerController {
    
    private final CustomerManagementUseCase customerManagementUseCase;
    private final CustomerMapper customerMapper;
    
    public CustomerController(CustomerManagementUseCase customerManagementUseCase, 
                            CustomerMapper customerMapper) {
        this.customerManagementUseCase = customerManagementUseCase;
        this.customerMapper = customerMapper;
    }
    
    @PostMapping
    public ResponseEntity<CustomerResponse> createCustomer(@Valid @RequestBody CreateCustomerRequest request) {
        try {
            CreateCustomerCommand command = customerMapper.toCommand(request);
            Customer customer = customerManagementUseCase.createCustomer(command);
            CustomerResponse response = customerMapper.toResponse(customer);
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (CustomerAlreadyExistsException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT).build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }
    
    @GetMapping("/{customerId}")
    public ResponseEntity<CustomerResponse> getCustomer(@PathVariable Long customerId) {
        return customerManagementUseCase.getCustomerById(customerId)
                .map(customer -> ResponseEntity.ok(customerMapper.toResponse(customer)))
                .orElse(ResponseEntity.notFound().build());
    }
    
    @GetMapping
    public ResponseEntity<List<CustomerResponse>> getAllCustomers(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        List<Customer> customers = customerManagementUseCase.getAllCustomers(page, size);
        List<CustomerResponse> responses = customers.stream()
                .map(customerMapper::toResponse)
                .toList();
        return ResponseEntity.ok(responses);
    }
    
    @PutMapping("/{customerId}")
    public ResponseEntity<CustomerResponse> updateCustomer(
            @PathVariable Long customerId,
            @Valid @RequestBody UpdateCustomerRequest request) {
        try {
            UpdateCustomerCommand command = customerMapper.toCommand(customerId, request);
            Customer customer = customerManagementUseCase.updateCustomer(command);
            CustomerResponse response = customerMapper.toResponse(customer);
            return ResponseEntity.ok(response);
        } catch (CustomerNotFoundException e) {
            return ResponseEntity.notFound().build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }
    
    @PostMapping("/{customerId}/credit/reserve")
    public ResponseEntity<Void> reserveCredit(
            @PathVariable Long customerId,
            @RequestParam BigDecimal amount) {
        try {
            customerManagementUseCase.reserveCustomerCredit(customerId, amount);
            return ResponseEntity.ok().build();
        } catch (CustomerNotFoundException e) {
            return ResponseEntity.notFound().build();
        } catch (IllegalStateException e) {
            return ResponseEntity.badRequest().build();
        }
    }
    
    @PostMapping("/{customerId}/credit/release")
    public ResponseEntity<Void> releaseCredit(
            @PathVariable Long customerId,
            @RequestParam BigDecimal amount) {
        try {
            customerManagementUseCase.releaseCustomerCredit(customerId, amount);
            return ResponseEntity.ok().build();
        } catch (CustomerNotFoundException e) {
            return ResponseEntity.notFound().build();
        } catch (IllegalStateException e) {
            return ResponseEntity.badRequest().build();
        }
    }
    
    @GetMapping("/{customerId}/loan-eligibility")
    public ResponseEntity<Boolean> checkLoanEligibility(
            @PathVariable Long customerId,
            @RequestParam BigDecimal loanAmount) {
        try {
            boolean eligible = customerManagementUseCase.isCustomerEligibleForLoan(customerId, loanAmount);
            return ResponseEntity.ok(eligible);
        } catch (CustomerNotFoundException e) {
            return ResponseEntity.notFound().build();
        }
    }
    
    @PutMapping("/{customerId}/credit-score")
    public ResponseEntity<Void> updateCreditScore(
            @PathVariable Long customerId,
            @RequestParam Integer creditScore) {
        try {
            customerManagementUseCase.updateCustomerCreditScore(customerId, creditScore);
            return ResponseEntity.ok().build();
        } catch (CustomerNotFoundException e) {
            return ResponseEntity.notFound().build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }
    
    @PostMapping("/{customerId}/deactivate")
    public ResponseEntity<Void> deactivateCustomer(@PathVariable Long customerId) {
        try {
            customerManagementUseCase.deactivateCustomer(customerId);
            return ResponseEntity.ok().build();
        } catch (CustomerNotFoundException e) {
            return ResponseEntity.notFound().build();
        }
    }
    
    @PostMapping("/{customerId}/reactivate")
    public ResponseEntity<Void> reactivateCustomer(@PathVariable Long customerId) {
        try {
            customerManagementUseCase.reactivateCustomer(customerId);
            return ResponseEntity.ok().build();
        } catch (CustomerNotFoundException e) {
            return ResponseEntity.notFound().build();
        }
    }
}
