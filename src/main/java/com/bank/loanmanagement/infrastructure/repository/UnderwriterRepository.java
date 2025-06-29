package com.bank.loanmanagement.infrastructure.repository;

import com.bank.loanmanagement.domain.staff.Underwriter;
import com.bank.loanmanagement.domain.staff.UnderwriterSpecialization;
import com.bank.loanmanagement.domain.staff.EmployeeStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

/**
 * Underwriter Repository Interface
 * 
 * Provides data access operations for Underwriter entities with business-focused queries.
 * Follows repository pattern with proper abstraction from infrastructure concerns.
 * 
 * Architecture Guardrails Compliance:
 * ✅ Request Parsing: N/A (Repository Interface)
 * ✅ Validation: Query parameter validation through method signatures
 * ✅ Response Types: Returns domain entities and typed collections
 * ✅ Type Safety: Strong typing with generics and BigDecimal
 * ✅ Dependency Inversion: Interface-based repository pattern
 */
@Repository
public interface UnderwriterRepository extends JpaRepository<Underwriter, String> {
    
    /**
     * Find all active underwriters
     */
    List<Underwriter> findByStatus(EmployeeStatus status);
    
    /**
     * Find underwriters by specialization and status
     */
    List<Underwriter> findBySpecializationAndStatus(
            UnderwriterSpecialization specialization, 
            EmployeeStatus status);
    
    /**
     * Find underwriters who can approve a specific loan amount
     */
    List<Underwriter> findByApprovalLimitGreaterThanEqualAndStatus(
            BigDecimal amount, 
            EmployeeStatus status);
    
    /**
     * Find underwriters by specialization with sufficient approval limit
     */
    @Query("SELECT u FROM Underwriter u WHERE u.specialization = :specialization " +
           "AND u.approvalLimit >= :amount AND u.status = :status " +
           "ORDER BY u.approvalLimit ASC")
    List<Underwriter> findBySpecializationAndApprovalLimitAndStatus(
            @Param("specialization") UnderwriterSpecialization specialization,
            @Param("amount") BigDecimal amount,
            @Param("status") EmployeeStatus status);
    
    /**
     * Find the most suitable underwriter for a loan type and amount
     */
    @Query("SELECT u FROM Underwriter u WHERE u.specialization = :specialization " +
           "AND u.approvalLimit >= :amount AND u.status = :status " +
           "ORDER BY u.yearsExperience DESC, u.approvalLimit ASC")
    Optional<Underwriter> findMostSuitableUnderwriter(
            @Param("specialization") UnderwriterSpecialization specialization,
            @Param("amount") BigDecimal amount,
            @Param("status") EmployeeStatus status);
    
    /**
     * Find underwriters by experience level
     */
    @Query("SELECT u FROM Underwriter u WHERE u.yearsExperience >= :minYears " +
           "AND u.yearsExperience <= :maxYears AND u.status = :status")
    List<Underwriter> findByExperienceRange(
            @Param("minYears") Integer minYears,
            @Param("maxYears") Integer maxYears,
            @Param("status") EmployeeStatus status);
    
    /**
     * Find senior underwriters (5+ years experience)
     */
    @Query("SELECT u FROM Underwriter u WHERE u.yearsExperience >= 5 AND u.status = :status")
    List<Underwriter> findSeniorUnderwriters(@Param("status") EmployeeStatus status);
    
    /**
     * Find underwriters with high approval limits (>= $500,000)
     */
    @Query("SELECT u FROM Underwriter u WHERE u.approvalLimit >= 500000 AND u.status = :status")
    List<Underwriter> findHighApprovalLimitUnderwriters(@Param("status") EmployeeStatus status);
    
    /**
     * Count underwriters by specialization
     */
    @Query("SELECT u.specialization, COUNT(u) FROM Underwriter u " +
           "WHERE u.status = :status GROUP BY u.specialization")
    List<Object[]> countBySpecialization(@Param("status") EmployeeStatus status);
    
    /**
     * Find underwriters by email domain (for organization queries)
     */
    @Query("SELECT u FROM Underwriter u WHERE u.email LIKE %:domain% AND u.status = :status")
    List<Underwriter> findByEmailDomain(
            @Param("domain") String domain, 
            @Param("status") EmployeeStatus status);
    
    /**
     * Find underwriters hired within a date range
     */
    @Query("SELECT u FROM Underwriter u WHERE u.hireDate >= :startDate " +
           "AND u.hireDate <= :endDate AND u.status = :status")
    List<Underwriter> findByHireDateRange(
            @Param("startDate") java.time.LocalDate startDate,
            @Param("endDate") java.time.LocalDate endDate,
            @Param("status") EmployeeStatus status);
    
    /**
     * Find available underwriters for workload balancing
     * This is a business method that could be used for assignment algorithms
     */
    @Query("SELECT u FROM Underwriter u WHERE u.status = :status " +
           "ORDER BY u.yearsExperience DESC")
    List<Underwriter> findAvailableForAssignment(@Param("status") EmployeeStatus status);
    
    /**
     * Check if underwriter exists by email
     */
    boolean existsByEmail(String email);
    
    /**
     * Find underwriter by email
     */
    Optional<Underwriter> findByEmail(String email);
    
    /**
     * Custom query to find underwriters with invalid approval limits for their specialization
     */
    @Query("SELECT u FROM Underwriter u WHERE " +
           "(u.specialization = 'PERSONAL_LOANS' AND u.approvalLimit > 100000) OR " +
           "(u.specialization = 'BUSINESS_LOANS' AND u.approvalLimit > 5000000) OR " +
           "(u.specialization = 'MORTGAGES' AND u.approvalLimit > 10000000)")
    List<Underwriter> findWithInvalidApprovalLimits();
    
    /**
     * Find underwriters who need approval limit review (based on experience)
     */
    @Query("SELECT u FROM Underwriter u WHERE " +
           "u.yearsExperience >= 10 AND u.approvalLimit < 1000000 AND u.status = :status")
    List<Underwriter> findEligibleForApprovalLimitIncrease(@Param("status") EmployeeStatus status);
    
    /**
     * Get underwriter statistics for reporting
     */
    @Query("SELECT " +
           "COUNT(u) as totalCount, " +
           "AVG(u.yearsExperience) as avgExperience, " +
           "AVG(u.approvalLimit) as avgApprovalLimit, " +
           "MIN(u.approvalLimit) as minApprovalLimit, " +
           "MAX(u.approvalLimit) as maxApprovalLimit " +
           "FROM Underwriter u WHERE u.status = :status")
    Object[] getUnderwriterStatistics(@Param("status") EmployeeStatus status);
}