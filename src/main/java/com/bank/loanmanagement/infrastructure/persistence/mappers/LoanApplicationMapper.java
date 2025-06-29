package com.bank.loanmanagement.infrastructure.persistence.mappers;

import com.bank.loanmanagement.domain.application.LoanApplication;
import com.bank.loanmanagement.infrastructure.persistence.jpa.LoanApplicationJpaEntity;
import org.springframework.stereotype.Component;

/**
 * Mapper between LoanApplication domain model and JPA infrastructure entity
 * 
 * Implements proper Hexagonal Architecture by providing clean separation
 * between domain and infrastructure layers.
 * 
 * Architecture Compliance:
 * ✅ Hexagonal Architecture: Proper adapter pattern implementation
 * ✅ Clean Code: Single responsibility for domain-infrastructure mapping
 * ✅ DDD: Infrastructure service for cross-layer communication
 * ✅ Type Safety: Strong typing and null safety
 */
@Component
public class LoanApplicationMapper {
    
    /**
     * Convert domain model to JPA entity for persistence
     */
    public LoanApplicationJpaEntity toJpaEntity(LoanApplication domain) {
        if (domain == null) {
            return null;
        }
        
        return LoanApplicationJpaEntity.builder()
            .applicationId(domain.getApplicationId())
            .customerId(domain.getCustomerId())
            .loanType(domain.getLoanType())
            .requestedAmount(domain.getRequestedAmount())
            .requestedTermMonths(domain.getRequestedTermMonths())
            .purpose(domain.getPurpose())
            .applicationDate(domain.getApplicationDate())
            .status(domain.getStatus())
            .assignedUnderwriter(domain.getAssignedUnderwriter())
            .priority(domain.getPriority())
            .monthlyIncome(domain.getMonthlyIncome())
            .employmentYears(domain.getEmploymentYears())
            .collateralValue(domain.getCollateralValue())
            .businessRevenue(domain.getBusinessRevenue())
            .propertyValue(domain.getPropertyValue())
            .downPayment(domain.getDownPayment())
            .decisionDate(domain.getDecisionDate())
            .decisionReason(domain.getDecisionReason())
            .approvedAmount(domain.getApprovedAmount())
            .approvedRate(domain.getApprovedRate())
            .createdAt(domain.getCreatedAt())
            .updatedAt(domain.getUpdatedAt())
            .version(domain.getVersion())
            .build();
    }
    
    /**
     * Convert JPA entity to domain model for business logic
     */
    public LoanApplication toDomainModel(LoanApplicationJpaEntity jpaEntity) {
        if (jpaEntity == null) {
            return null;
        }
        
        return LoanApplication.reconstruct(
            jpaEntity.getApplicationId(),
            jpaEntity.getCustomerId(),
            jpaEntity.getLoanType(),
            jpaEntity.getRequestedAmount(),
            jpaEntity.getRequestedTermMonths(),
            jpaEntity.getPurpose(),
            jpaEntity.getApplicationDate(),
            jpaEntity.getStatus(),
            jpaEntity.getPriority(),
            jpaEntity.getMonthlyIncome(),
            jpaEntity.getEmploymentYears(),
            jpaEntity.getCollateralValue(),
            jpaEntity.getBusinessRevenue(),
            jpaEntity.getPropertyValue(),
            jpaEntity.getDownPayment(),
            jpaEntity.getDecisionDate(),
            jpaEntity.getDecisionReason(),
            jpaEntity.getApprovedAmount(),
            jpaEntity.getApprovedRate(),
            jpaEntity.getAssignedUnderwriter(),
            jpaEntity.getCreatedAt(),
            jpaEntity.getUpdatedAt(),
            jpaEntity.getVersion()
        );
    }
    
    /**
     * Update JPA entity with changes from domain model
     * Preserves JPA entity identity while applying domain changes
     */
    public void updateJpaEntity(LoanApplicationJpaEntity jpaEntity, LoanApplication domain) {
        if (jpaEntity == null || domain == null) {
            return;
        }
        
        // Update mutable fields from domain
        jpaEntity.setStatus(domain.getStatus());
        jpaEntity.setAssignedUnderwriter(domain.getAssignedUnderwriter());
        jpaEntity.setPriority(domain.getPriority());
        jpaEntity.setDecisionDate(domain.getDecisionDate());
        jpaEntity.setDecisionReason(domain.getDecisionReason());
        jpaEntity.setApprovedAmount(domain.getApprovedAmount());
        jpaEntity.setApprovedRate(domain.getApprovedRate());
        jpaEntity.setUpdatedAt(domain.getUpdatedAt());
        jpaEntity.setVersion(domain.getVersion());
    }
    
    /**
     * Business method to check if mapping is valid
     */
    public boolean isValidMapping(LoanApplication domain, LoanApplicationJpaEntity jpaEntity) {
        if (domain == null || jpaEntity == null) {
            return false;
        }
        
        // Check critical business identifiers match
        return domain.getApplicationId().equals(jpaEntity.getApplicationId()) &&
               domain.getCustomerId().equals(jpaEntity.getCustomerId()) &&
               domain.getLoanType().equals(jpaEntity.getLoanType());
    }
    
    /**
     * Business method to validate domain-entity consistency
     */
    public void validateConsistency(LoanApplication domain, LoanApplicationJpaEntity jpaEntity) {
        if (!isValidMapping(domain, jpaEntity)) {
            throw new IllegalArgumentException(
                String.format("Domain model and JPA entity are inconsistent for application: %s", 
                             domain != null ? domain.getApplicationId() : "null"));
        }
    }
}