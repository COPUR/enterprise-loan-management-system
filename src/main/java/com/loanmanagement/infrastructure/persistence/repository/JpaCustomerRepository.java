package com.loanmanagement.infrastructure.persistence.repository;

import com.loanmanagement.domain.model.entity.Customer;
import com.loanmanagement.loan.port.CustomerRepository;
import com.loanmanagement.infrastructure.persistence.entity.CustomerJpaEntity;
import com.loanmanagement.infrastructure.persistence.mapper.CustomerMapper;
import org.springframework.stereotype.Component;
import java.util.Optional;

/**
 * Infrastructure implementation of CustomerRepository port.
 * This class adapts Spring Data JPA to the domain's CustomerRepository interface,
 * following Hexagonal Architecture principles by keeping infrastructure concerns
 * separate from domain logic.
 */
@Component
public class JpaCustomerRepository implements CustomerRepository {

    private final SpringDataCustomerRepository jpaRepository;
    private final CustomerMapper customerMapper;

    /**
     * Constructor injection for better testability and explicit dependencies.
     *
     * @param jpaRepository Spring Data JPA repository for database operations
     * @param customerMapper Mapper for converting between domain and persistence entities
     */
    public JpaCustomerRepository(SpringDataCustomerRepository jpaRepository, CustomerMapper customerMapper) {
        this.jpaRepository = jpaRepository;
        this.customerMapper = customerMapper;
    }

    @Override
    public Optional<Customer> findById(Long id) {
        if (id == null) {
            return Optional.empty();
        }

        return jpaRepository.findById(id)
                .map(customerMapper::toDomain);
    }

    @Override
    public Customer save(Customer customer) {
        if (customer == null) {
            throw new IllegalArgumentException("Customer cannot be null");
        }

        CustomerJpaEntity entity = customerMapper.toEntity(customer);
        CustomerJpaEntity savedEntity = jpaRepository.save(entity);
        return customerMapper.toDomain(savedEntity);
    }
}
