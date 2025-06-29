package com.bank.loanmanagement.domain.staff.ports;

import com.bank.loanmanagement.domain.staff.EmployeeStatus;
import com.bank.loanmanagement.domain.staff.Underwriter;
import com.bank.loanmanagement.domain.staff.UnderwriterSpecialization;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * Repository Port for Underwriter Domain
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
public interface UnderwriterRepository {
    
    /**
     * Save an underwriter (create or update)
     */
    Underwriter save(Underwriter underwriter);
    
    /**
     * Find underwriter by ID
     */
    Optional<Underwriter> findById(String underwriterId);
    
    /**
     * Find all underwriters
     */
    List<Underwriter> findAll();
    
    /**
     * Check if underwriter exists
     */
    boolean existsById(String underwriterId);
    
    /**
     * Delete underwriter
     */
    void deleteById(String underwriterId);
    
    /**
     * Business-focused finder methods
     */
    
    /**
     * Find all active underwriters
     */
    List<Underwriter> findActiveUnderwriters();
    
    /**
     * Find underwriters by specialization
     */
    List<Underwriter> findBySpecialization(UnderwriterSpecialization specialization);
    
    /**
     * Find underwriters by specialization and status
     */
    List<Underwriter> findBySpecializationAndStatus(UnderwriterSpecialization specialization, EmployeeStatus status);
    
    /**
     * Find underwriters who can approve a specific loan amount
     */
    List<Underwriter> findByApprovalLimitGreaterThanEqual(BigDecimal amount);
    
    /**
     * Find underwriters by specialization with sufficient approval limit
     */
    List<Underwriter> findBySpecializationAndApprovalLimit(UnderwriterSpecialization specialization, BigDecimal amount);
    
    /**
     * Find the most suitable underwriter for a loan type and amount
     */
    Optional<Underwriter> findMostSuitableUnderwriter(UnderwriterSpecialization specialization, BigDecimal amount);
    
    /**
     * Find senior underwriters (5+ years experience)
     */
    List<Underwriter> findSeniorUnderwriters();
    
    /**
     * Find underwriters with high approval limits (>= $500,000)
     */
    List<Underwriter> findHighApprovalLimitUnderwriters();
    
    /**
     * Find underwriter by email
     */
    Optional<Underwriter> findByEmail(String email);
    
    /**
     * Check if underwriter exists by email
     */
    boolean existsByEmail(String email);
    
    /**
     * Find available underwriters for assignment
     */
    List<Underwriter> findAvailableForAssignment();
    
    /**
     * Find underwriters by experience range
     */
    List<Underwriter> findByExperienceRange(Integer minYears, Integer maxYears);
    
    /**
     * Find underwriters hired within date range
     */
    List<Underwriter> findByHireDateBetween(LocalDate startDate, LocalDate endDate);
    
    /**
     * Find underwriters eligible for approval limit increase
     */
    List<Underwriter> findEligibleForApprovalLimitIncrease();
    
    /**
     * Find underwriters with invalid approval limits for their specialization
     */
    List<Underwriter> findWithInvalidApprovalLimits();
    
    /**
     * Count underwriters by specialization
     */
    long countBySpecialization(UnderwriterSpecialization specialization);
    
    /**
     * Count active underwriters
     */
    long countActiveUnderwriters();
}