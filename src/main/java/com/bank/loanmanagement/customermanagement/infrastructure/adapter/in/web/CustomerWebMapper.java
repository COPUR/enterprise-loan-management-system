package com.bank.loanmanagement.customermanagement.infrastructure.adapter.in.web;

import com.bank.loanmanagement.customermanagement.domain.model.Customer;
import com.bank.loanmanagement.customermanagement.domain.model.CustomerId;
import com.bank.loanmanagement.customermanagement.domain.port.in.CreateCustomerCommand;
import com.bank.loanmanagement.customermanagement.domain.port.in.UpdateCustomerCommand;
import com.bank.loanmanagement.customermanagement.infrastructure.adapter.in.web.dto.CreateCustomerRequest;
import com.bank.loanmanagement.customermanagement.infrastructure.adapter.in.web.dto.CustomerResponse;
import com.bank.loanmanagement.customermanagement.infrastructure.adapter.in.web.dto.UpdateCustomerRequest;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * Mapper between web DTOs and domain commands/models.
 */
@Component
public class CustomerWebMapper {
    
    /**
     * Convert web request to domain command.
     */
    public CreateCustomerCommand toCommand(CreateCustomerRequest request) {
        return new CreateCustomerCommand(
            request.firstName(),
            request.lastName(),
            request.email(),
            request.phoneNumber(),
            request.initialCreditLimit()
        );
    }
    
    /**
     * Convert web request to domain command.
     */
    public UpdateCustomerCommand toCommand(String customerId, UpdateCustomerRequest request) {
        return new UpdateCustomerCommand(
            CustomerId.of(customerId),
            Optional.ofNullable(request.firstName()),
            Optional.ofNullable(request.lastName()),
            Optional.ofNullable(request.email()),
            Optional.ofNullable(request.phoneNumber())
        );
    }
    
    /**
     * Convert domain model to web response.
     */
    public CustomerResponse toResponse(Customer customer) {
        return new CustomerResponse(
            customer.getId().getValue(),
            customer.getName().getFirstName(),
            customer.getName().getLastName(),
            customer.getName().getFullName(),
            customer.getEmail().getValue(),
            customer.getPhoneNumber().getValue(),
            customer.getCreditLimit().getAmount().getAmount(),
            customer.getCreditLimit().getAmount().getCurrency().getCurrencyCode(),
            customer.getUsedCredit().getAmount(),
            customer.getUsedCredit().getCurrency().getCurrencyCode(),
            customer.getAvailableCredit().getAmount(),
            customer.getAvailableCredit().getCurrency().getCurrencyCode(),
            customer.getStatus().name(),
            customer.getCreatedAt(),
            customer.getUpdatedAt()
        );
    }
}