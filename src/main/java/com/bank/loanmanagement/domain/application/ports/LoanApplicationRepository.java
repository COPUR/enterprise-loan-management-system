package com.bank.loanmanagement.domain.application.ports;

import com.bank.loanmanagement.domain.application.ApplicationPriority;
import com.bank.loanmanagement.domain.application.ApplicationStatus;
import com.bank.loanmanagement.domain.application.LoanApplication;
import com.bank.loanmanagement.domain.application.LoanType;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * Repository Port for Loan Application Domain
 * 
 * This is a pure domain interface (port) that defines business-focused
 * data access operations without any infrastructure concerns.
 * 
 * Architecture Compliance:
 * ✅ Hexagonal Architecture: Pure port interface without infrastructure dependencies
 * ✅ Clean Code: Business-focused method names and clear contracts
 * ✅ DDD: Repository as domain pattern with business operations
 * ✅ Type Safety: Strong typing with domain value objects
 * ✅ Dependency Inversion: Interface segregation and abstraction
 */
public interface LoanApplicationRepository {
    
    /**
     * Save a loan application (create or update)
     */
    LoanApplication save(LoanApplication loanApplication);
    
    /**
     * Find loan application by ID
     */
    Optional<LoanApplication> findById(String applicationId);
    
    /**
     * Find all loan applications
     */
    List<LoanApplication> findAll();
    
    /**
     * Check if loan application exists
     */
    boolean existsById(String applicationId);
    
    /**
     * Delete loan application
     */
    void deleteById(String applicationId);
    
    /**
     * Business-focused finder methods
     */
    
    /**
     * Find applications by customer
     */
    List<LoanApplication> findByCustomerId(Long customerId);
    
    /**
     * Find applications by status
     */
    List<LoanApplication> findByStatus(ApplicationStatus status);
    
    /**
     * Find applications by loan type
     */
    List<LoanApplication> findByLoanType(LoanType loanType);
    
    /**
     * Find applications assigned to an underwriter
     */
    List<LoanApplication> findByAssignedUnderwriter(String underwriterId);
    
    /**
     * Find applications by priority
     */
    List<LoanApplication> findByPriority(ApplicationPriority priority);
    
    /**
     * Find applications within amount range
     */
    List<LoanApplication> findByRequestedAmountBetween(BigDecimal minAmount, BigDecimal maxAmount);
    
    /**
     * Find applications by date range
     */
    List<LoanApplication> findByApplicationDateBetween(LocalDate startDate, LocalDate endDate);
    
    /**
     * Find applications needing review (overdue)
     */
    List<LoanApplication> findOverdueApplications();
    
    /**
     * Find high-priority pending applications
     */
    List<LoanApplication> findHighPriorityPendingApplications();
    
    /**
     * Find applications by status and underwriter
     */
    List<LoanApplication> findByStatusAndAssignedUnderwriter(ApplicationStatus status, String underwriterId);
    
    /**
     * Find applications requiring documents
     */
    List<LoanApplication> findPendingDocuments();
    
    /**
     * Find applications by customer and status
     */
    List<LoanApplication> findByCustomerIdAndStatus(Long customerId, ApplicationStatus status);
    
    /**
     * Find applications for risk assessment
     */
    List<LoanApplication> findApplicationsForRiskAssessment();
    
    /**
     * Find applications by loan type and status
     */
    List<LoanApplication> findByLoanTypeAndStatus(LoanType loanType, ApplicationStatus status);
    
    /**
     * Count applications by status
     */
    long countByStatus(ApplicationStatus status);
    
    /**
     * Count applications by underwriter
     */
    long countByAssignedUnderwriter(String underwriterId);
    
    /**
     * Find applications created today
     */
    List<LoanApplication> findApplicationsCreatedToday();
    
    /**
     * Find large loan applications (business rule: > $500,000)
     */
    List<LoanApplication> findLargeLoanApplications();
}