package com.bank.customer.infrastructure.persistence;

import com.bank.customer.domain.Customer;
import com.bank.customer.domain.CustomerRepository;
import com.bank.shared.kernel.domain.CustomerId;
import com.bank.shared.kernel.event.DomainEventPublisher;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * JPA implementation of CustomerRepository
 * 
 * Implements Hexagonal Architecture - Infrastructure adapter for persistence
 * Publishes domain events after persistence operations (EDA)
 */
@Repository
public class JpaCustomerRepository implements CustomerRepository {
    
    private final SpringDataCustomerRepository springDataRepository;
    private final DomainEventPublisher eventPublisher;
    
    public JpaCustomerRepository(SpringDataCustomerRepository springDataRepository, 
                                DomainEventPublisher eventPublisher) {
        this.springDataRepository = springDataRepository;
        this.eventPublisher = eventPublisher;
    }
    
    @Override
    public Customer save(Customer customer) {
        Customer savedCustomer = springDataRepository.save(customer);
        
        // Publish domain events after successful persistence (EDA)
        if (savedCustomer.hasUnpublishedEvents()) {
            eventPublisher.publishAll(savedCustomer.getDomainEvents());
            savedCustomer.clearDomainEvents();
        }
        
        return savedCustomer;
    }
    
    @Override
    public Optional<Customer> findById(CustomerId customerId) {
        return springDataRepository.findByCustomerId(customerId.getValue());
    }
    
    @Override
    public Optional<Customer> findByEmail(String email) {
        return springDataRepository.findByEmail(email);
    }
    
    @Override
    public boolean existsByEmail(String email) {
        return springDataRepository.existsByEmail(email);
    }

    @Override
    public boolean existsById(CustomerId customerId) {
        return springDataRepository.existsByCustomerId(customerId.getValue());
    }
    
    public List<Customer> findByStatus(String status) {
        return springDataRepository.findByStatus(status);
    }
    
    public void delete(Customer customer) {
        springDataRepository.delete(customer);
    }

    @Override
    public void deleteById(CustomerId customerId) {
        springDataRepository.findByCustomerId(customerId.getValue())
            .ifPresent(springDataRepository::delete);
    }

    public List<Customer> findHighValueCustomers(java.math.BigDecimal minimumCreditLimit) {
        return springDataRepository.findHighValueCustomers(minimumCreditLimit);
    }
    
    /**
     * Spring Data JPA Repository interface
     */
    interface SpringDataCustomerRepository extends JpaRepository<Customer, Long> {
        
        @Query("SELECT c FROM Customer c WHERE c.customerId.value = :customerId")
        Optional<Customer> findByCustomerId(@Param("customerId") String customerId);
        
        Optional<Customer> findByEmail(String email);
        
        boolean existsByEmail(String email);

        @Query("SELECT COUNT(c) > 0 FROM Customer c WHERE c.customerId.value = :customerId")
        boolean existsByCustomerId(@Param("customerId") String customerId);
        
        @Query("SELECT c FROM Customer c WHERE c.status = :status")
        List<Customer> findByStatus(@Param("status") String status);
        
        @Query("SELECT c FROM Customer c WHERE c.creditProfile.creditLimit.amount >= :minimumLimit")
        List<Customer> findHighValueCustomers(@Param("minimumLimit") java.math.BigDecimal minimumLimit);
    }
}
