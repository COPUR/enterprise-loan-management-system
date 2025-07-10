package com.loanmanagement.analytics;

import com.loanmanagement.customer.domain.model.Customer;

import java.util.List;

/**
 * Repository interface for customer analytics operations
 */
public interface CustomerRepository {
    
    /**
     * Find all active customers for analytics purposes
     * @return List of active customers
     */
    List<Customer> findAllActiveCustomers();
    
    /**
     * Find customers by risk level
     * @param riskLevel The risk level (LOW, MEDIUM, HIGH)
     * @return List of customers matching the risk level
     */
    List<Customer> findCustomersByRiskLevel(String riskLevel);
    
    /**
     * Find customers with high credit utilization
     * @param utilizationThreshold The utilization threshold (e.g., 0.75 for 75%)
     * @return List of customers with utilization above threshold
     */
    List<Customer> findCustomersWithHighUtilization(java.math.BigDecimal utilizationThreshold);
}