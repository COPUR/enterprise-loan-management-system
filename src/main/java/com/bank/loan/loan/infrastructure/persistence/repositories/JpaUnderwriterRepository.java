package com.bank.loanmanagement.loan.infrastructure.persistence.repositories;

import com.bank.loanmanagement.loan.domain.staff.EmployeeStatus;
import com.bank.loanmanagement.loan.domain.staff.Underwriter;
import com.bank.loanmanagement.loan.domain.staff.UnderwriterSpecialization;
import com.bank.loanmanagement.loan.domain.staff.ports.UnderwriterRepository;
import com.bank.loanmanagement.loan.infrastructure.persistence.jpa.UnderwriterJpaEntity;
import com.bank.loanmanagement.loan.infrastructure.persistence.mappers.UnderwriterMapper;
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
 * JPA Repository Infrastructure Implementation for Underwriters
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
public class JpaUnderwriterRepository implements UnderwriterRepository {
    
    private final UnderwriterJpaRepository jpaRepository;
    private final UnderwriterMapper mapper;
    
    @Override
    public Underwriter save(Underwriter underwriter) {
        log.debug("Saving underwriter: {}", underwriter.getUnderwriterId());
        
        UnderwriterJpaEntity jpaEntity = mapper.toJpaEntity(underwriter);
        UnderwriterJpaEntity savedEntity = jpaRepository.save(jpaEntity);
        
        Underwriter savedDomain = mapper.toDomainModel(savedEntity);
        log.debug("Successfully saved underwriter: {}", savedDomain.getUnderwriterId());
        
        return savedDomain;
    }
    
    @Override
    public Optional<Underwriter> findById(String underwriterId) {
        log.debug("Finding underwriter by ID: {}", underwriterId);
        
        return jpaRepository.findById(underwriterId)
                .map(mapper::toDomainModel);
    }
    
    @Override
    public List<Underwriter> findAll() {
        log.debug("Finding all underwriters");
        
        return jpaRepository.findAll()
                .stream()
                .map(mapper::toDomainModel)
                .collect(Collectors.toList());
    }
    
    @Override
    public boolean existsById(String underwriterId) {
        return jpaRepository.existsById(underwriterId);
    }
    
    @Override
    public void deleteById(String underwriterId) {
        log.debug("Deleting underwriter: {}", underwriterId);
        jpaRepository.deleteById(underwriterId);
    }
    
    @Override
    public List<Underwriter> findActiveUnderwriters() {
        return jpaRepository.findByStatus(EmployeeStatus.ACTIVE)
                .stream()
                .map(mapper::toDomainModel)
                .collect(Collectors.toList());
    }
    
    @Override
    public List<Underwriter> findBySpecialization(UnderwriterSpecialization specialization) {
        return jpaRepository.findBySpecialization(specialization)
                .stream()
                .map(mapper::toDomainModel)
                .collect(Collectors.toList());
    }
    
    @Override
    public List<Underwriter> findBySpecializationAndStatus(UnderwriterSpecialization specialization, EmployeeStatus status) {
        return jpaRepository.findBySpecializationAndStatus(specialization, status)
                .stream()
                .map(mapper::toDomainModel)
                .collect(Collectors.toList());
    }
    
    @Override
    public List<Underwriter> findByApprovalLimitGreaterThanEqual(BigDecimal amount) {
        return jpaRepository.findByApprovalLimitGreaterThanEqualAndStatus(amount, EmployeeStatus.ACTIVE)
                .stream()
                .map(mapper::toDomainModel)
                .collect(Collectors.toList());
    }
    
    @Override
    public List<Underwriter> findBySpecializationAndApprovalLimit(UnderwriterSpecialization specialization, BigDecimal amount) {
        return jpaRepository.findBySpecializationAndApprovalLimitAndStatus(specialization, amount, EmployeeStatus.ACTIVE)
                .stream()
                .map(mapper::toDomainModel)
                .collect(Collectors.toList());
    }
    
    @Override
    public Optional<Underwriter> findMostSuitableUnderwriter(UnderwriterSpecialization specialization, BigDecimal amount) {
        return jpaRepository.findMostSuitableUnderwriter(specialization, amount, EmployeeStatus.ACTIVE)
                .map(mapper::toDomainModel);
    }
    
    @Override
    public List<Underwriter> findSeniorUnderwriters() {
        return jpaRepository.findSeniorUnderwriters(EmployeeStatus.ACTIVE)
                .stream()
                .map(mapper::toDomainModel)
                .collect(Collectors.toList());
    }
    
    @Override
    public List<Underwriter> findHighApprovalLimitUnderwriters() {
        return jpaRepository.findHighApprovalLimitUnderwriters(EmployeeStatus.ACTIVE)
                .stream()
                .map(mapper::toDomainModel)
                .collect(Collectors.toList());
    }
    
    @Override
    public Optional<Underwriter> findByEmail(String email) {
        return jpaRepository.findByEmail(email)
                .map(mapper::toDomainModel);
    }
    
    @Override
    public boolean existsByEmail(String email) {
        return jpaRepository.existsByEmail(email);
    }
    
    @Override
    public List<Underwriter> findAvailableForAssignment() {
        return jpaRepository.findAvailableForAssignment(EmployeeStatus.ACTIVE)
                .stream()
                .map(mapper::toDomainModel)
                .collect(Collectors.toList());
    }
    
    @Override
    public List<Underwriter> findByExperienceRange(Integer minYears, Integer maxYears) {
        return jpaRepository.findByExperienceRange(minYears, maxYears, EmployeeStatus.ACTIVE)
                .stream()
                .map(mapper::toDomainModel)
                .collect(Collectors.toList());
    }
    
    @Override
    public List<Underwriter> findByHireDateBetween(LocalDate startDate, LocalDate endDate) {
        return jpaRepository.findByHireDateRange(startDate, endDate, EmployeeStatus.ACTIVE)
                .stream()
                .map(mapper::toDomainModel)
                .collect(Collectors.toList());
    }
    
    @Override
    public List<Underwriter> findEligibleForApprovalLimitIncrease() {
        return jpaRepository.findEligibleForApprovalLimitIncrease(EmployeeStatus.ACTIVE)
                .stream()
                .map(mapper::toDomainModel)
                .collect(Collectors.toList());
    }
    
    @Override
    public List<Underwriter> findWithInvalidApprovalLimits() {
        return jpaRepository.findWithInvalidApprovalLimits()
                .stream()
                .map(mapper::toDomainModel)
                .collect(Collectors.toList());
    }
    
    @Override
    public long countBySpecialization(UnderwriterSpecialization specialization) {
        return jpaRepository.countBySpecializationAndStatus(specialization, EmployeeStatus.ACTIVE);
    }
    
    @Override
    public long countActiveUnderwriters() {
        return jpaRepository.countByStatus(EmployeeStatus.ACTIVE);
    }
}

/**
 * JPA Repository Interface for UnderwriterJpaEntity
 */
interface UnderwriterJpaRepository extends JpaRepository<UnderwriterJpaEntity, String> {
    
    List<UnderwriterJpaEntity> findByStatus(EmployeeStatus status);
    List<UnderwriterJpaEntity> findBySpecialization(UnderwriterSpecialization specialization);
    List<UnderwriterJpaEntity> findBySpecializationAndStatus(UnderwriterSpecialization specialization, EmployeeStatus status);
    List<UnderwriterJpaEntity> findByApprovalLimitGreaterThanEqualAndStatus(BigDecimal amount, EmployeeStatus status);
    Optional<UnderwriterJpaEntity> findByEmail(String email);
    boolean existsByEmail(String email);
    
    long countByStatus(EmployeeStatus status);
    long countBySpecializationAndStatus(UnderwriterSpecialization specialization, EmployeeStatus status);
    
    @Query("SELECT u FROM UnderwriterJpaEntity u WHERE u.specialization = :specialization " +
           "AND u.approvalLimit >= :amount AND u.status = :status " +
           "ORDER BY u.approvalLimit ASC")
    List<UnderwriterJpaEntity> findBySpecializationAndApprovalLimitAndStatus(
            @Param("specialization") UnderwriterSpecialization specialization,
            @Param("amount") BigDecimal amount,
            @Param("status") EmployeeStatus status);
    
    @Query("SELECT u FROM UnderwriterJpaEntity u WHERE u.specialization = :specialization " +
           "AND u.approvalLimit >= :amount AND u.status = :status " +
           "ORDER BY u.yearsExperience DESC, u.approvalLimit ASC")
    Optional<UnderwriterJpaEntity> findMostSuitableUnderwriter(
            @Param("specialization") UnderwriterSpecialization specialization,
            @Param("amount") BigDecimal amount,
            @Param("status") EmployeeStatus status);
    
    @Query("SELECT u FROM UnderwriterJpaEntity u WHERE u.yearsExperience >= :minYears " +
           "AND u.yearsExperience <= :maxYears AND u.status = :status")
    List<UnderwriterJpaEntity> findByExperienceRange(
            @Param("minYears") Integer minYears,
            @Param("maxYears") Integer maxYears,
            @Param("status") EmployeeStatus status);
    
    @Query("SELECT u FROM UnderwriterJpaEntity u WHERE u.yearsExperience >= 5 AND u.status = :status")
    List<UnderwriterJpaEntity> findSeniorUnderwriters(@Param("status") EmployeeStatus status);
    
    @Query("SELECT u FROM UnderwriterJpaEntity u WHERE u.approvalLimit >= 500000 AND u.status = :status")
    List<UnderwriterJpaEntity> findHighApprovalLimitUnderwriters(@Param("status") EmployeeStatus status);
    
    @Query("SELECT u FROM UnderwriterJpaEntity u WHERE u.status = :status " +
           "ORDER BY u.yearsExperience DESC")
    List<UnderwriterJpaEntity> findAvailableForAssignment(@Param("status") EmployeeStatus status);
    
    @Query("SELECT u FROM UnderwriterJpaEntity u WHERE u.hireDate >= :startDate " +
           "AND u.hireDate <= :endDate AND u.status = :status")
    List<UnderwriterJpaEntity> findByHireDateRange(
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate,
            @Param("status") EmployeeStatus status);
    
    @Query("SELECT u FROM UnderwriterJpaEntity u WHERE " +
           "(u.specialization = 'PERSONAL_LOANS' AND u.approvalLimit > 100000) OR " +
           "(u.specialization = 'BUSINESS_LOANS' AND u.approvalLimit > 5000000) OR " +
           "(u.specialization = 'MORTGAGES' AND u.approvalLimit > 10000000)")
    List<UnderwriterJpaEntity> findWithInvalidApprovalLimits();
    
    @Query("SELECT u FROM UnderwriterJpaEntity u WHERE " +
           "u.yearsExperience >= 10 AND u.approvalLimit < 1000000 AND u.status = :status")
    List<UnderwriterJpaEntity> findEligibleForApprovalLimitIncrease(@Param("status") EmployeeStatus status);
}