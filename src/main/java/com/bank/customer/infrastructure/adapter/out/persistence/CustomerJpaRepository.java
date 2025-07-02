package com.bank.customer.infrastructure.adapter.out.persistence;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Spring Data JPA repository for CustomerJpaEntity.
 * Contains only JPA-specific queries and operations.
 */
@Repository
public interface CustomerJpaRepository extends JpaRepository<CustomerJpaEntity, String> {
    
    /**
     * Find customer by email address.
     */
    Optional<CustomerJpaEntity> findByEmail(String email);
    
    /**
     * Check if customer exists with given email.
     */
    boolean existsByEmail(String email);
    
    /**
     * Find customers by status with pagination.
     */
    Page<CustomerJpaEntity> findByStatus(CustomerStatusJpa status, Pageable pageable);
    
    /**
     * Count customers by status.
     */
    long countByStatus(CustomerStatusJpa status);
    
    /**
     * Custom query to find customers with available credit above threshold.
     */
    @Query("""
        SELECT c FROM CustomerJpaEntity c 
        WHERE c.status = :status 
        AND (c.creditLimitAmount - c.usedCreditAmount) >= :minAvailableCredit
        """)
    Page<CustomerJpaEntity> findCustomersWithAvailableCredit(
        @Param("status") CustomerStatusJpa status,
        @Param("minAvailableCredit") java.math.BigDecimal minAvailableCredit,
        Pageable pageable
    );
    
    /**
     * Health check query.
     */
    @Query("SELECT COUNT(c) FROM CustomerJpaEntity c")
    long healthCheck();
}