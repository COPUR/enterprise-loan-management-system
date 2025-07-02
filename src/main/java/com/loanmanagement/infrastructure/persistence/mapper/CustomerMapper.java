package com.loanmanagement.infrastructure.persistence.mapper;

import com.loanmanagement.domain.model.entity.Customer;
import com.loanmanagement.domain.model.value.Money;
import com.loanmanagement.infrastructure.persistence.entity.CustomerJpaEntity;
import org.springframework.stereotype.Component;

@Component
public class CustomerMapper {

    public Customer toDomain(CustomerJpaEntity entity) {
        if (entity == null) {
            return null;
        }

        return new Customer(
            entity.getId(),
            entity.getName(),
            entity.getSurname(),
            new Money(entity.getCreditLimit())
        );
    }

    public CustomerJpaEntity toEntity(Customer customer) {
        if (customer == null) {
            return null;
        }

        CustomerJpaEntity entity = new CustomerJpaEntity();
        entity.setId(customer.getId());
        entity.setName(customer.getName());
        entity.setSurname(customer.getSurname());
        entity.setCreditLimit(customer.getCreditLimit().getAmount());

        return entity;
    }
}
