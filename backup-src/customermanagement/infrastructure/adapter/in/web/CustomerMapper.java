
package com.bank.loanmanagement.customermanagement.infrastructure.adapter.in.web;

import com.bank.loanmanagement.customermanagement.domain.model.Customer;
import com.bank.loanmanagement.customermanagement.domain.port.in.CreateCustomerCommand;
import com.bank.loanmanagement.customermanagement.domain.port.in.UpdateCustomerCommand;
import com.bank.loanmanagement.customermanagement.infrastructure.adapter.in.web.dto.CreateCustomerRequest;
import com.bank.loanmanagement.customermanagement.infrastructure.adapter.in.web.dto.CustomerResponse;
import com.bank.loanmanagement.customermanagement.infrastructure.adapter.in.web.dto.UpdateCustomerRequest;
import org.springframework.stereotype.Component;

@Component
public class CustomerMapper {
    
    public CreateCustomerCommand toCreateCommand(CreateCustomerRequest request) {
        return CreateCustomerCommand.builder()
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .email(request.getEmail())
                .phoneNumber(request.getPhoneNumber())
                .creditScore(request.getCreditScore())
                .monthlyIncome(request.getMonthlyIncome())
                .creditLimit(request.getCreditLimit())
                .build();
    }
    
    public UpdateCustomerCommand toUpdateCommand(Long customerId, UpdateCustomerRequest request) {
        return UpdateCustomerCommand.builder()
                .customerId(customerId)
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .email(request.getEmail())
                .phoneNumber(request.getPhoneNumber())
                .creditScore(request.getCreditScore())
                .monthlyIncome(request.getMonthlyIncome())
                .creditLimit(request.getCreditLimit())
                .status(request.getStatus())
                .build();
    }
    
    public CustomerResponse toResponse(Customer customer) {
        return CustomerResponse.builder()
                .customerId(customer.getCustomerId())
                .firstName(customer.getFirstName())
                .lastName(customer.getLastName())
                .email(customer.getEmail())
                .phoneNumber(customer.getPhoneNumber())
                .creditScore(customer.getCreditScore())
                .monthlyIncome(customer.getMonthlyIncome())
                .creditLimit(customer.getCreditLimit())
                .availableCredit(customer.getAvailableCredit())
                .status(customer.getStatus())
                .createdAt(customer.getCreatedAt())
                .updatedAt(customer.getUpdatedAt())
                .build();
    }
}
