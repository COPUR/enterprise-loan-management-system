package com.bank.customer.infrastructure.adapter.out.persistence;

import com.bank.loan.loan.domain.customer.Customer;
import com.bank.loan.loan.domain.customer.CustomerId;
import com.bank.loan.loan.domain.customer.EmailAddress;
import com.bank.customer.domain.port.out.CustomerRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Repository adapter that implements the domain CustomerRepository interface.
 * Bridges between domain and JPA persistence layer.
 */
@Repository
@Transactional
public class CustomerRepositoryAdapter implements CustomerRepository {
    
    private final CustomerJpaRepository jpaRepository;
    private final CustomerMapper mapper;
    
    public CustomerRepositoryAdapter(
        CustomerJpaRepository jpaRepository,
        CustomerMapper mapper
    ) {
        this.jpaRepository = jpaRepository;
        this.mapper = mapper;
    }
    
    @Override
    public Customer save(Customer customer) {
        CustomerJpaEntity entity = mapper.toEntity(customer);
        CustomerJpaEntity savedEntity = jpaRepository.save(entity);
        return mapper.toDomain(savedEntity);
    }
    
    @Override
    @Transactional(readOnly = true)
    public Optional<Customer> findById(CustomerId customerId) {
        return jpaRepository.findById(customerId.getValue())
            .map(mapper::toDomain);
    }
    
    @Override
    @Transactional(readOnly = true)
    public Optional<Customer> findByEmail(EmailAddress email) {
        return jpaRepository.findByEmail(email.getValue())
            .map(mapper::toDomain);
    }
    
    @Override
    @Transactional(readOnly = true)
    public boolean existsByEmail(EmailAddress email) {
        return jpaRepository.existsByEmail(email.getValue());
    }
    
    @Override
    @Transactional(readOnly = true)
    public List<Customer> findAll(int page, int size, String sortBy, String sortDirection) {
        Sort.Direction direction = "desc".equalsIgnoreCase(sortDirection) 
            ? Sort.Direction.DESC 
            : Sort.Direction.ASC;
        
        Pageable pageable = PageRequest.of(page, size, Sort.by(direction, mapSortField(sortBy)));
        
        Page<CustomerJpaEntity> entityPage = jpaRepository.findAll(pageable);
        
        return entityPage.getContent()
            .stream()
            .map(mapper::toDomain)
            .collect(Collectors.toList());
    }
    
    @Override
    @Transactional(readOnly = true)
    public long count() {
        return jpaRepository.count();
    }
    
    @Override
    public void deleteById(CustomerId customerId) {
        jpaRepository.deleteById(customerId.getValue());
    }
    
    @Override
    @Transactional(readOnly = true)
    public void healthCheck() {
        jpaRepository.healthCheck();
    }
    
    /**
     * Map domain sort fields to JPA entity fields.
     */
    private String mapSortField(String sortBy) {
        return switch (sortBy.toLowerCase()) {
            case "id", "customerid" -> "id";
            case "name", "firstname" -> "firstName";
            case "lastname" -> "lastName";
            case "email" -> "email";
            case "phone", "phonenumber" -> "phoneNumber";
            case "status" -> "status";
            case "createdat", "created" -> "createdAt";
            case "updatedat", "updated" -> "updatedAt";
            case "creditlimit" -> "creditLimitAmount";
            case "usedcredit" -> "usedCreditAmount";
            default -> "createdAt"; // Default sort field
        };
    }
}