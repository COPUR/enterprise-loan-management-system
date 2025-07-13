package com.bank.loan.domain.repository;

import com.bank.loan.domain.model.Customer;
import com.bank.loan.domain.model.CustomerId;
import com.bank.loan.domain.model.CustomerStatus;
import com.bank.loan.domain.model.CustomerType;
import com.bank.loan.domain.model.Money;

import java.util.List;
import java.util.Optional;

/**
 * Clean Domain Repository Interface for Customer
 * 
 * This interface defines the contract for customer persistence operations
 * without any infrastructure dependencies. It follows DDD principles.
 */
public interface CustomerDomainRepository {
    
    /**
     * Save a customer (create or update)
     */
    Customer save(Customer customer);
    
    /**
     * Find customer by ID
     */
    Optional<Customer> findById(CustomerId customerId);
    
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
     * Find customers with specific credit score range
     */
    List<Customer> findByCreditScoreRange(Integer minScore, Integer maxScore);
    
    /**
     * Find customers eligible for loan of specific amount
     */
    List<Customer> findLoanEligibleCustomers(Money requestedAmount);
    
    /**
     * Find active customers
     */
    List<Customer> findActiveCustomers();
    
    /**
     * Find Islamic banking customers
     */
    List<Customer> findIslamicBankingCustomers();
    
    /**
     * Check if email exists
     */
    boolean existsByEmail(String email);
    
    /**
     * Check if phone exists
     */
    boolean existsByPhone(String phone);
    
    /**
     * Delete customer by ID
     */
    void deleteById(CustomerId customerId);
    
    /**
     * Count customers by status
     */
    long countByStatus(CustomerStatus status);
    
    /**
     * Find customers by location
     */
    List<Customer> findByLocation(String city, String country);
}