package com.bank.loan.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

/**
 * JPA Repository for Customer Entity (Infrastructure Layer)
 * 
 * This repository handles the actual database operations for customer entities.
 * It's separate from the domain repository interface.
 */
@Repository
public interface CustomerJpaRepository extends JpaRepository<CustomerEntity, Long> {
    
    Optional<CustomerEntity> findByCustomerId(String customerId);
    
    Optional<CustomerEntity> findByEmail(String email);
    
    Optional<CustomerEntity> findByPhone(String phone);
    
    List<CustomerEntity> findByStatus(String status);
    
    List<CustomerEntity> findByCustomerType(String customerType);
    
    @Query("SELECT c FROM CustomerEntity c WHERE c.creditScore BETWEEN :minScore AND :maxScore")
    List<CustomerEntity> findByCreditScoreBetween(@Param("minScore") Integer minScore, @Param("maxScore") Integer maxScore);
    
    @Query("SELECT c FROM CustomerEntity c WHERE c.status = 'ACTIVE'")
    List<CustomerEntity> findActiveCustomers();
    
    List<CustomerEntity> findByIslamicBankingPreference(Boolean islamicBankingPreference);
    
    boolean existsByEmail(String email);
    
    boolean existsByPhone(String phone);
    
    List<CustomerEntity> findByCityAndCountry(String city, String country);
    
    long countByStatus(String status);
    
    @Query("SELECT c FROM CustomerEntity c WHERE c.status = 'ACTIVE' AND c.creditScore >= 600")
    List<CustomerEntity> findLoanEligibleCustomers();
}