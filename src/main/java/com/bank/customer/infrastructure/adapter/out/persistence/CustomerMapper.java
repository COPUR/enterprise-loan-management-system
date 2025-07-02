package com.bank.customer.infrastructure.adapter.out.persistence;

import com.bank.loan.loan.domain.customer.*;
import com.bank.loan.sharedkernel.domain.Money;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Currency;

/**
 * Mapper between Customer domain model and CustomerJpaEntity.
 * Handles conversion logic between domain and persistence layers.
 */
@Component
public class CustomerMapper {
    
    /**
     * Convert domain Customer to JPA entity.
     */
    public CustomerJpaEntity toEntity(Customer customer) {
        if (customer == null) {
            return null;
        }
        
        CustomerJpaEntity entity = new CustomerJpaEntity(
            customer.getId().getValue(),
            customer.getName().getFirstName(),
            customer.getName().getLastName(),
            customer.getEmail().getValue(),
            customer.getPhoneNumber().getValue(),
            customer.getCreditLimit().getAmount().getAmount(),
            customer.getCreditLimit().getAmount().getCurrency().getCurrencyCode(),
            customer.getUsedCredit().getAmount(),
            customer.getUsedCredit().getCurrency().getCurrencyCode(),
            mapStatusToJpa(customer.getStatus())
        );
        
        // Set timestamps if available (for updates)
        if (customer.getCreatedAt() != null) {
            entity.setCreatedAt(customer.getCreatedAt());
        }
        if (customer.getUpdatedAt() != null) {
            entity.setUpdatedAt(customer.getUpdatedAt());
        }
        
        return entity;
    }
    
    /**
     * Convert JPA entity to domain Customer.
     */
    public Customer toDomain(CustomerJpaEntity entity) {
        if (entity == null) {
            return null;
        }
        
        // Create value objects
        CustomerId customerId = CustomerId.of(entity.getId());
        PersonalName name = PersonalName.of(entity.getFirstName(), entity.getLastName());
        EmailAddress email = EmailAddress.of(entity.getEmail());
        PhoneNumber phoneNumber = PhoneNumber.of(entity.getPhoneNumber());
        
        Money creditLimitMoney = Money.of(
            entity.getCreditLimitAmount(),
            Currency.getInstance(entity.getCreditLimitCurrency())
        );
        CreditLimit creditLimit = CreditLimit.of(creditLimitMoney);
        
        Money usedCredit = Money.of(
            entity.getUsedCreditAmount(),
            Currency.getInstance(entity.getUsedCreditCurrency())
        );
        
        CustomerStatus status = mapStatusFromJpa(entity.getStatus());
        
        // Create customer using factory method, then set internal state
        Customer customer = Customer.create(customerId, name, email, phoneNumber, creditLimit);
        
        // Use package-private setters to reconstruct state from persistence
        customer.setUsedCredit(usedCredit);
        customer.setStatus(status);
        customer.setCreatedAt(entity.getCreatedAt());
        customer.setUpdatedAt(entity.getUpdatedAt());
        
        // Clear domain events from reconstruction
        customer.clearDomainEvents();
        
        return customer;
    }
    
    /**
     * Map domain CustomerStatus to JPA enum.
     */
    private CustomerStatusJpa mapStatusToJpa(CustomerStatus status) {
        return switch (status) {
            case PENDING -> CustomerStatusJpa.PENDING;
            case ACTIVE -> CustomerStatusJpa.ACTIVE;
            case SUSPENDED -> CustomerStatusJpa.SUSPENDED;
            case CLOSED -> CustomerStatusJpa.CLOSED;
            case BLOCKED -> CustomerStatusJpa.BLOCKED;
        };
    }
    
    /**
     * Map JPA enum to domain CustomerStatus.
     */
    private CustomerStatus mapStatusFromJpa(CustomerStatusJpa status) {
        return switch (status) {
            case PENDING -> CustomerStatus.PENDING;
            case ACTIVE -> CustomerStatus.ACTIVE;
            case SUSPENDED -> CustomerStatus.SUSPENDED;
            case CLOSED -> CustomerStatus.CLOSED;
            case BLOCKED -> CustomerStatus.BLOCKED;
        };
    }
}