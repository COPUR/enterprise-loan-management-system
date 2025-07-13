package com.bank.loan.domain.repository;

import com.bank.loan.domain.model.Customer;
import com.bank.loan.domain.model.CustomerStatus;
import com.bank.loan.domain.model.CustomerType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

/**
 * Repository interface for Customer entity operations
 */
@Repository
public interface CustomerRepository extends JpaRepository<Customer, Long> {
    
    /**
     * Find customer by email
     */
    Optional<Customer> findByEmail(String email);
    
    /**
     * Find customer by phone
     */
    Optional<Customer> findByPhone(String phone);
    
    /**
     * Find customers by status
     */
    List<Customer> findByStatus(CustomerStatus status);
    
    /**
     * Find customers by type
     */
    List<Customer> findByCustomerType(CustomerType customerType);
    
    /**
     * Find customers by name (case insensitive)
     */
    @Query("SELECT c FROM Customer c WHERE LOWER(CONCAT(c.firstName, ' ', c.lastName)) LIKE LOWER(CONCAT('%', :name, '%'))")
    List<Customer> findByNameContainingIgnoreCase(@Param("name") String name);
    
    /**
     * Find customers by first name and last name
     */
    List<Customer> findByFirstNameIgnoreCaseAndLastNameIgnoreCase(String firstName, String lastName);
    
    /**
     * Find customers with credit score range
     */
    List<Customer> findByCreditScoreBetween(Integer minScore, Integer maxScore);
    
    /**
     * Find customers with high credit limit
     */
    List<Customer> findByCreditLimitGreaterThan(BigDecimal minCreditLimit);
    
    /**
     * Find active customers
     */
    @Query("SELECT c FROM Customer c WHERE c.status = 'ACTIVE'")
    List<Customer> findActiveCustomers();
    
    /**
     * Find Islamic banking customers
     */
    List<Customer> findByIslamicBankingPreference(Boolean islamicBankingPreference);
    
    /**
     * Check if email exists
     */
    boolean existsByEmail(String email);
    
    /**
     * Check if phone exists
     */
    boolean existsByPhone(String phone);
    
    /**
     * Find customers by city
     */
    List<Customer> findByCity(String city);
    
    /**
     * Find customers by country
     */
    List<Customer> findByCountry(String country);
    
    /**
     * Find customers eligible for loans (active status, good credit score)
     */
    @Query("SELECT c FROM Customer c WHERE c.status = 'ACTIVE' AND c.creditScore >= :minCreditScore")
    List<Customer> findLoanEligibleCustomers(@Param("minCreditScore") Integer minCreditScore);
    
    /**
     * Find customers with pagination
     */
    Page<Customer> findByStatus(CustomerStatus status, Pageable pageable);
    
    /**
     * Count customers by status
     */
    long countByStatus(CustomerStatus status);
    
    /**
     * Find customers with specific filters
     */
    @Query("SELECT c FROM Customer c WHERE " +
           "(:status IS NULL OR c.status = :status) AND " +
           "(:customerType IS NULL OR c.customerType = :customerType) AND " +
           "(:minCreditScore IS NULL OR c.creditScore >= :minCreditScore) AND " +
           "(:maxCreditScore IS NULL OR c.creditScore <= :maxCreditScore) AND " +
           "(:city IS NULL OR LOWER(c.city) = LOWER(:city))")
    List<Customer> findCustomersWithFilters(
        @Param("status") CustomerStatus status,
        @Param("customerType") CustomerType customerType,
        @Param("minCreditScore") Integer minCreditScore,
        @Param("maxCreditScore") Integer maxCreditScore,
        @Param("city") String city
    );
}