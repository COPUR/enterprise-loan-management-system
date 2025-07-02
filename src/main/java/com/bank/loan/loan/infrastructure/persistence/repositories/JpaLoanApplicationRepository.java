package com.bank.loanmanagement.loan.infrastructure.persistence.repositories;

import com.bank.loanmanagement.loan.domain.application.*;
import com.bank.loanmanagement.loan.domain.application.ports.LoanApplicationRepository;
import com.bank.loanmanagement.loan.infrastructure.persistence.jpa.LoanApplicationJpaEntity;
import com.bank.loanmanagement.loan.infrastructure.persistence.mappers.LoanApplicationMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * JPA Repository Infrastructure Implementation for Loan Applications
 * 
 * Implements the domain repository port using JPA infrastructure.
 * Uses mappers to convert between domain models and JPA entities.
 * 
 * Architecture Compliance:
 * ✅ Hexagonal Architecture: Infrastructure adapter implementing domain port
 * ✅ Clean Code: Single responsibility for data access
 * ✅ DDD: Infrastructure service translating domain operations to persistence
 * ✅ Type Safety: Strong typing with proper error handling
 */
@Repository
@RequiredArgsConstructor
@Slf4j
public class JpaLoanApplicationRepository implements LoanApplicationRepository {
    
    private final LoanApplicationJpaRepository jpaRepository;
    private final LoanApplicationMapper mapper;
    
    @Override
    public LoanApplication save(LoanApplication loanApplication) {
        log.debug("Saving loan application: {}", loanApplication.getApplicationId());
        
        LoanApplicationJpaEntity jpaEntity = mapper.toJpaEntity(loanApplication);
        LoanApplicationJpaEntity savedEntity = jpaRepository.save(jpaEntity);
        
        LoanApplication savedDomain = mapper.toDomainModel(savedEntity);
        log.debug("Successfully saved loan application: {}", savedDomain.getApplicationId());
        
        return savedDomain;
    }
    
    @Override
    public Optional<LoanApplication> findById(String applicationId) {
        log.debug("Finding loan application by ID: {}", applicationId);
        
        return jpaRepository.findById(applicationId)
                .map(mapper::toDomainModel);
    }
    
    @Override
    public List<LoanApplication> findAll() {
        log.debug("Finding all loan applications");
        
        return jpaRepository.findAll()
                .stream()
                .map(mapper::toDomainModel)
                .collect(Collectors.toList());
    }
    
    @Override
    public boolean existsById(String applicationId) {
        return jpaRepository.existsById(applicationId);
    }
    
    @Override
    public void deleteById(String applicationId) {
        log.debug("Deleting loan application: {}", applicationId);
        jpaRepository.deleteById(applicationId);
    }
    
    @Override
    public List<LoanApplication> findByCustomerId(Long customerId) {
        return jpaRepository.findByCustomerId(customerId)
                .stream()
                .map(mapper::toDomainModel)
                .collect(Collectors.toList());
    }
    
    @Override
    public List<LoanApplication> findByStatus(ApplicationStatus status) {
        return jpaRepository.findByStatus(status)
                .stream()
                .map(mapper::toDomainModel)
                .collect(Collectors.toList());
    }
    
    @Override
    public List<LoanApplication> findByLoanType(LoanType loanType) {
        return jpaRepository.findByLoanType(loanType)
                .stream()
                .map(mapper::toDomainModel)
                .collect(Collectors.toList());
    }
    
    @Override
    public List<LoanApplication> findByAssignedUnderwriter(String underwriterId) {
        return jpaRepository.findByAssignedUnderwriter(underwriterId)
                .stream()
                .map(mapper::toDomainModel)
                .collect(Collectors.toList());
    }
    
    @Override
    public List<LoanApplication> findByPriority(ApplicationPriority priority) {
        return jpaRepository.findByPriority(priority)
                .stream()
                .map(mapper::toDomainModel)
                .collect(Collectors.toList());
    }
    
    @Override
    public List<LoanApplication> findByRequestedAmountBetween(BigDecimal minAmount, BigDecimal maxAmount) {
        return jpaRepository.findByRequestedAmountBetween(minAmount, maxAmount)
                .stream()
                .map(mapper::toDomainModel)
                .collect(Collectors.toList());
    }
    
    @Override
    public List<LoanApplication> findByApplicationDateBetween(LocalDate startDate, LocalDate endDate) {
        return jpaRepository.findByApplicationDateBetween(startDate, endDate)
                .stream()
                .map(mapper::toDomainModel)
                .collect(Collectors.toList());
    }
    
    @Override
    public List<LoanApplication> findOverdueApplications() {
        return jpaRepository.findOverdueApplications()
                .stream()
                .map(mapper::toDomainModel)
                .collect(Collectors.toList());
    }
    
    @Override
    public List<LoanApplication> findHighPriorityPendingApplications() {
        return jpaRepository.findHighPriorityPendingApplications()
                .stream()
                .map(mapper::toDomainModel)
                .collect(Collectors.toList());
    }
    
    @Override
    public List<LoanApplication> findByStatusAndAssignedUnderwriter(ApplicationStatus status, String underwriterId) {
        return jpaRepository.findByStatusAndAssignedUnderwriter(status, underwriterId)
                .stream()
                .map(mapper::toDomainModel)
                .collect(Collectors.toList());
    }
    
    @Override
    public List<LoanApplication> findPendingDocuments() {
        return jpaRepository.findByStatus(ApplicationStatus.PENDING_DOCUMENTS)
                .stream()
                .map(mapper::toDomainModel)
                .collect(Collectors.toList());
    }
    
    @Override
    public List<LoanApplication> findByCustomerIdAndStatus(Long customerId, ApplicationStatus status) {
        return jpaRepository.findByCustomerIdAndStatus(customerId, status)
                .stream()
                .map(mapper::toDomainModel)
                .collect(Collectors.toList());
    }
    
    @Override
    public List<LoanApplication> findApplicationsForRiskAssessment() {
        return jpaRepository.findApplicationsForRiskAssessment()
                .stream()
                .map(mapper::toDomainModel)
                .collect(Collectors.toList());
    }
    
    @Override
    public List<LoanApplication> findByLoanTypeAndStatus(LoanType loanType, ApplicationStatus status) {
        return jpaRepository.findByLoanTypeAndStatus(loanType, status)
                .stream()
                .map(mapper::toDomainModel)
                .collect(Collectors.toList());
    }
    
    @Override
    public long countByStatus(ApplicationStatus status) {
        return jpaRepository.countByStatus(status);
    }
    
    @Override
    public long countByAssignedUnderwriter(String underwriterId) {
        return jpaRepository.countByAssignedUnderwriter(underwriterId);
    }
    
    @Override
    public List<LoanApplication> findApplicationsCreatedToday() {
        LocalDate today = LocalDate.now();
        return jpaRepository.findByApplicationDate(today)
                .stream()
                .map(mapper::toDomainModel)
                .collect(Collectors.toList());
    }
    
    @Override
    public List<LoanApplication> findLargeLoanApplications() {
        BigDecimal largeAmountThreshold = new BigDecimal("500000");
        return jpaRepository.findByRequestedAmountGreaterThanEqual(largeAmountThreshold)
                .stream()
                .map(mapper::toDomainModel)
                .collect(Collectors.toList());
    }
}

/**
 * JPA Repository Interface for LoanApplicationJpaEntity
 */
interface LoanApplicationJpaRepository extends JpaRepository<LoanApplicationJpaEntity, String> {
    
    List<LoanApplicationJpaEntity> findByCustomerId(Long customerId);
    List<LoanApplicationJpaEntity> findByStatus(ApplicationStatus status);
    List<LoanApplicationJpaEntity> findByLoanType(LoanType loanType);
    List<LoanApplicationJpaEntity> findByAssignedUnderwriter(String underwriterId);
    List<LoanApplicationJpaEntity> findByPriority(ApplicationPriority priority);
    List<LoanApplicationJpaEntity> findByRequestedAmountBetween(BigDecimal minAmount, BigDecimal maxAmount);
    List<LoanApplicationJpaEntity> findByApplicationDateBetween(LocalDate startDate, LocalDate endDate);
    List<LoanApplicationJpaEntity> findByStatusAndAssignedUnderwriter(ApplicationStatus status, String underwriterId);
    List<LoanApplicationJpaEntity> findByCustomerIdAndStatus(Long customerId, ApplicationStatus status);
    List<LoanApplicationJpaEntity> findByLoanTypeAndStatus(LoanType loanType, ApplicationStatus status);
    List<LoanApplicationJpaEntity> findByApplicationDate(LocalDate applicationDate);
    List<LoanApplicationJpaEntity> findByRequestedAmountGreaterThanEqual(BigDecimal amount);
    
    long countByStatus(ApplicationStatus status);
    long countByAssignedUnderwriter(String underwriterId);
    
    @Query("SELECT la FROM LoanApplicationJpaEntity la WHERE " +
           "la.status = 'UNDER_REVIEW' AND " +
           "DATEDIFF(CURRENT_DATE, la.applicationDate) > " +
           "CASE la.priority " +
           "WHEN 'URGENT' THEN 1 " +
           "WHEN 'HIGH' THEN 3 " +
           "WHEN 'STANDARD' THEN 7 " +
           "WHEN 'LOW' THEN 14 END")
    List<LoanApplicationJpaEntity> findOverdueApplications();
    
    @Query("SELECT la FROM LoanApplicationJpaEntity la WHERE " +
           "la.status = 'PENDING' AND la.priority IN ('HIGH', 'URGENT') " +
           "ORDER BY la.priority DESC, la.applicationDate ASC")
    List<LoanApplicationJpaEntity> findHighPriorityPendingApplications();
    
    @Query("SELECT la FROM LoanApplicationJpaEntity la WHERE " +
           "la.status = 'PENDING' AND la.requestedAmount >= 100000")
    List<LoanApplicationJpaEntity> findApplicationsForRiskAssessment();
}