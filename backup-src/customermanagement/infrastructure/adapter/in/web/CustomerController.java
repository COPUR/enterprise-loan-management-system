
package com.bank.loanmanagement.customermanagement.infrastructure.adapter.in.web;

import com.bank.loanmanagement.customermanagement.domain.model.Customer;
import com.bank.loanmanagement.customermanagement.domain.port.in.CreateCustomerCommand;
import com.bank.loanmanagement.customermanagement.domain.port.in.CustomerManagementUseCase;
import com.bank.loanmanagement.customermanagement.domain.port.in.UpdateCustomerCommand;
import com.bank.loanmanagement.customermanagement.infrastructure.adapter.in.web.dto.CreateCustomerRequest;
import com.bank.loanmanagement.customermanagement.infrastructure.adapter.in.web.dto.CustomerResponse;
import com.bank.loanmanagement.customermanagement.infrastructure.adapter.in.web.dto.UpdateCustomerRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/customers")
@RequiredArgsConstructor
public class CustomerController {
    
    private final CustomerManagementUseCase customerManagementUseCase;
    private final CustomerMapper customerMapper;
    
    @PostMapping
    public ResponseEntity<CustomerResponse> createCustomer(@RequestBody CreateCustomerRequest request) {
        CreateCustomerCommand command = customerMapper.toCreateCommand(request);
        Customer customer = customerManagementUseCase.createCustomer(command);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(customerMapper.toResponse(customer));
    }
    
    @GetMapping("/{customerId}")
    public ResponseEntity<CustomerResponse> getCustomer(@PathVariable Long customerId) {
        Customer customer = customerManagementUseCase.getCustomerById(customerId);
        return ResponseEntity.ok(customerMapper.toResponse(customer));
    }
    
    @GetMapping
    public ResponseEntity<List<CustomerResponse>> getAllCustomers() {
        List<Customer> customers = customerManagementUseCase.getAllCustomers();
        List<CustomerResponse> responses = customers.stream()
                .map(customerMapper::toResponse)
                .collect(Collectors.toList());
        return ResponseEntity.ok(responses);
    }
    
    @PutMapping("/{customerId}")
    public ResponseEntity<CustomerResponse> updateCustomer(
            @PathVariable Long customerId,
            @RequestBody UpdateCustomerRequest request) {
        UpdateCustomerCommand command = customerMapper.toUpdateCommand(customerId, request);
        Customer customer = customerManagementUseCase.updateCustomer(command);
        return ResponseEntity.ok(customerMapper.toResponse(customer));
    }
    
    @DeleteMapping("/{customerId}")
    public ResponseEntity<Void> deleteCustomer(@PathVariable Long customerId) {
        customerManagementUseCase.deleteCustomer(customerId);
        return ResponseEntity.noContent().build();
    }
    
    @PostMapping("/{customerId}/credit/reserve")
    public ResponseEntity<Void> reserveCredit(
            @PathVariable Long customerId,
            @RequestParam BigDecimal amount) {
        customerManagementUseCase.reserveCredit(customerId, amount);
        return ResponseEntity.ok().build();
    }
    
    @PostMapping("/{customerId}/credit/release")
    public ResponseEntity<Void> releaseCredit(
            @PathVariable Long customerId,
            @RequestParam BigDecimal amount) {
        customerManagementUseCase.releaseCredit(customerId, amount);
        return ResponseEntity.ok().build();
    }
}
