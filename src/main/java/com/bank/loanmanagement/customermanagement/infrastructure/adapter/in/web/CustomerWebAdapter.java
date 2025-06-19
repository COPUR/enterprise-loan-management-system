package com.bank.loanmanagement.customermanagement.infrastructure.adapter.in.web;

import com.bank.loanmanagement.customermanagement.application.service.CustomerAlreadyExistsException;
import com.bank.loanmanagement.customermanagement.application.service.CustomerNotFoundException;
import com.bank.loanmanagement.customermanagement.domain.model.Customer;
import com.bank.loanmanagement.customermanagement.domain.model.CustomerId;
import com.bank.loanmanagement.customermanagement.domain.model.InsufficientCreditException;
import com.bank.loanmanagement.customermanagement.domain.port.in.*;
import com.bank.loanmanagement.customermanagement.infrastructure.adapter.in.web.dto.*;
import com.bank.loanmanagement.sharedkernel.domain.Money;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.net.URI;
import java.util.Currency;
import java.util.List;
import java.util.Optional;

/**
 * REST controller for customer management operations.
 * Web adapter that translates HTTP requests to use case calls.
 */
@RestController
@RequestMapping("/api/v1/customers")
public class CustomerWebAdapter {
    
    private static final Logger logger = LoggerFactory.getLogger(CustomerWebAdapter.class);
    
    private final CustomerManagementUseCase customerManagementUseCase;
    private final CustomerWebMapper webMapper;
    
    public CustomerWebAdapter(
        CustomerManagementUseCase customerManagementUseCase,
        CustomerWebMapper webMapper
    ) {
        this.customerManagementUseCase = customerManagementUseCase;
        this.webMapper = webMapper;
    }
    
    @PostMapping
    public ResponseEntity<CustomerResponse> createCustomer(
        @Valid @RequestBody CreateCustomerRequest request
    ) {
        logger.info("Creating customer via REST API: {}", request.email());
        
        try {
            CreateCustomerCommand command = webMapper.toCommand(request);
            Customer customer = customerManagementUseCase.createCustomer(command);
            CustomerResponse response = webMapper.toResponse(customer);
            
            URI location = URI.create("/api/v1/customers/" + customer.getId().getValue());
            return ResponseEntity.created(location).body(response);
            
        } catch (CustomerAlreadyExistsException e) {
            logger.warn("Customer creation failed - already exists: {}", request.email());
            throw e;
        }
    }
    
    @GetMapping("/{customerId}")
    public ResponseEntity<CustomerResponse> getCustomer(
        @PathVariable String customerId
    ) {
        logger.debug("Getting customer via REST API: {}", customerId);
        
        FindCustomerQuery query = new FindCustomerQuery(CustomerId.of(customerId));
        Optional<Customer> customer = customerManagementUseCase.findCustomer(query);
        
        return customer
            .map(webMapper::toResponse)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }
    
    @PutMapping("/{customerId}")
    public ResponseEntity<CustomerResponse> updateCustomer(
        @PathVariable String customerId,
        @Valid @RequestBody UpdateCustomerRequest request
    ) {
        logger.info("Updating customer via REST API: {}", customerId);
        
        try {
            UpdateCustomerCommand command = webMapper.toCommand(customerId, request);
            Customer customer = customerManagementUseCase.updateCustomer(command);
            CustomerResponse response = webMapper.toResponse(customer);
            
            return ResponseEntity.ok(response);
            
        } catch (CustomerNotFoundException e) {
            return ResponseEntity.notFound().build();
        } catch (CustomerAlreadyExistsException e) {
            logger.warn("Customer update failed - email already exists: {}", request.email());
            throw e;
        }
    }
    
    @GetMapping
    public ResponseEntity<Page<CustomerResponse>> getAllCustomers(
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "20") int size,
        @RequestParam(defaultValue = "createdAt") String sortBy,
        @RequestParam(defaultValue = "desc") String sortDirection
    ) {
        logger.debug("Getting all customers via REST API - page: {}, size: {}", page, size);
        
        GetAllCustomersQuery query = new GetAllCustomersQuery(page, size, sortBy, sortDirection);
        List<Customer> customers = customerManagementUseCase.getAllCustomers(query);
        
        List<CustomerResponse> responseList = customers.stream()
            .map(webMapper::toResponse)
            .toList();
        
        // For simplicity, creating a basic page. In real implementation, 
        // would get total count from repository
        Page<CustomerResponse> responsePage = new PageImpl<>(
            responseList, 
            Pageable.ofSize(size).withPage(page), 
            responseList.size()
        );
        
        return ResponseEntity.ok(responsePage);
    }
    
    @PostMapping("/{customerId}/activate")
    public ResponseEntity<Void> activateCustomer(
        @PathVariable String customerId
    ) {
        logger.info("Activating customer via REST API: {}", customerId);
        
        try {
            ActivateCustomerCommand command = new ActivateCustomerCommand(CustomerId.of(customerId));
            customerManagementUseCase.activateCustomer(command);
            
            return ResponseEntity.ok().build();
            
        } catch (CustomerNotFoundException e) {
            return ResponseEntity.notFound().build();
        } catch (IllegalStateException e) {
            logger.warn("Customer activation failed: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        }
    }
    
    @PostMapping("/{customerId}/suspend")
    public ResponseEntity<Void> suspendCustomer(
        @PathVariable String customerId,
        @Valid @RequestBody SuspendCustomerRequest request
    ) {
        logger.info("Suspending customer via REST API: {}", customerId);
        
        try {
            SuspendCustomerCommand command = new SuspendCustomerCommand(
                CustomerId.of(customerId), 
                request.reason()
            );
            customerManagementUseCase.suspendCustomer(command);
            
            return ResponseEntity.ok().build();
            
        } catch (CustomerNotFoundException e) {
            return ResponseEntity.notFound().build();
        } catch (IllegalStateException e) {
            logger.warn("Customer suspension failed: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        }
    }
    
    @PostMapping("/{customerId}/credit/reserve")
    public ResponseEntity<Void> reserveCredit(
        @PathVariable String customerId,
        @Valid @RequestBody ReserveCreditRequest request
    ) {
        logger.info("Reserving credit via REST API for customer: {}", customerId);
        
        try {
            Money amount = Money.of(request.amount(), Currency.getInstance(request.currency()));
            ReserveCreditCommand command = new ReserveCreditCommand(
                CustomerId.of(customerId), 
                amount, 
                request.reason()
            );
            customerManagementUseCase.reserveCredit(command);
            
            return ResponseEntity.ok().build();
            
        } catch (CustomerNotFoundException e) {
            return ResponseEntity.notFound().build();
        } catch (InsufficientCreditException e) {
            logger.warn("Credit reservation failed: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        }
    }
    
    @PostMapping("/{customerId}/credit/release")
    public ResponseEntity<Void> releaseCredit(
        @PathVariable String customerId,
        @Valid @RequestBody ReleaseCreditRequest request
    ) {
        logger.info("Releasing credit via REST API for customer: {}", customerId);
        
        try {
            Money amount = Money.of(request.amount(), Currency.getInstance(request.currency()));
            ReleaseCreditCommand command = new ReleaseCreditCommand(
                CustomerId.of(customerId), 
                amount, 
                request.reason()
            );
            customerManagementUseCase.releaseCredit(command);
            
            return ResponseEntity.ok().build();
            
        } catch (CustomerNotFoundException e) {
            return ResponseEntity.notFound().build();
        } catch (IllegalArgumentException e) {
            logger.warn("Credit release failed: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        }
    }
    
    @GetMapping("/{customerId}/loan-eligibility")
    public ResponseEntity<LoanEligibilityResponse> checkLoanEligibility(
        @PathVariable String customerId,
        @RequestParam String amount,
        @RequestParam(defaultValue = "USD") String currency
    ) {
        logger.debug("Checking loan eligibility via REST API for customer: {}", customerId);
        
        try {
            Money loanAmount = Money.of(new java.math.BigDecimal(amount), Currency.getInstance(currency));
            CheckLoanEligibilityQuery query = new CheckLoanEligibilityQuery(
                CustomerId.of(customerId), 
                loanAmount
            );
            
            boolean eligible = customerManagementUseCase.checkLoanEligibility(query);
            LoanEligibilityResponse response = new LoanEligibilityResponse(eligible, loanAmount.toString());
            
            return ResponseEntity.ok(response);
            
        } catch (CustomerNotFoundException e) {
            return ResponseEntity.notFound().build();
        }
    }
    
    // Exception handlers
    @ExceptionHandler(CustomerAlreadyExistsException.class)
    public ResponseEntity<ErrorResponse> handleCustomerAlreadyExists(CustomerAlreadyExistsException e) {
        ErrorResponse error = new ErrorResponse("CUSTOMER_ALREADY_EXISTS", e.getMessage());
        return ResponseEntity.status(HttpStatus.CONFLICT).body(error);
    }
    
    @ExceptionHandler(CustomerNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleCustomerNotFound(CustomerNotFoundException e) {
        ErrorResponse error = new ErrorResponse("CUSTOMER_NOT_FOUND", e.getMessage());
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
    }
    
    @ExceptionHandler(InsufficientCreditException.class)
    public ResponseEntity<ErrorResponse> handleInsufficientCredit(InsufficientCreditException e) {
        ErrorResponse error = new ErrorResponse("INSUFFICIENT_CREDIT", e.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }
    
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArgument(IllegalArgumentException e) {
        ErrorResponse error = new ErrorResponse("INVALID_REQUEST", e.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }
    
    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<ErrorResponse> handleIllegalState(IllegalStateException e) {
        ErrorResponse error = new ErrorResponse("INVALID_STATE", e.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }
}