package com.bank.loanmanagement.infrastructure.persistence.mappers;

import com.bank.loanmanagement.domain.staff.Underwriter;
import com.bank.loanmanagement.infrastructure.persistence.jpa.UnderwriterJpaEntity;
import org.springframework.stereotype.Component;

/**
 * Mapper between Underwriter domain model and JPA infrastructure entity
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
public class UnderwriterMapper {
    
    /**
     * Convert domain model to JPA entity for persistence
     */
    public UnderwriterJpaEntity toJpaEntity(Underwriter domain) {
        if (domain == null) {
            return null;
        }
        
        return UnderwriterJpaEntity.builder()
            .underwriterId(domain.getUnderwriterId())
            .firstName(domain.getFirstName())
            .lastName(domain.getLastName())
            .email(domain.getEmail())
            .phone(domain.getPhone())
            .specialization(domain.getSpecialization())
            .yearsExperience(domain.getYearsExperience())
            .approvalLimit(domain.getApprovalLimit())
            .status(domain.getStatus())
            .hireDate(domain.getHireDate())
            .createdAt(domain.getCreatedAt())
            .updatedAt(domain.getUpdatedAt())
            .version(domain.getVersion())
            .build();
    }
    
    /**
     * Convert JPA entity to domain model for business logic
     */
    public Underwriter toDomainModel(UnderwriterJpaEntity jpaEntity) {
        if (jpaEntity == null) {
            return null;
        }
        
        return Underwriter.reconstruct(
            jpaEntity.getUnderwriterId(),
            jpaEntity.getFirstName(),
            jpaEntity.getLastName(),
            jpaEntity.getEmail(),
            jpaEntity.getPhone(),
            jpaEntity.getSpecialization(),
            jpaEntity.getYearsExperience(),
            jpaEntity.getApprovalLimit(),
            jpaEntity.getStatus(),
            jpaEntity.getHireDate(),
            jpaEntity.getCreatedAt(),
            jpaEntity.getUpdatedAt(),
            jpaEntity.getVersion()
        );
    }
    
    /**
     * Update JPA entity with changes from domain model
     * Preserves JPA entity identity while applying domain changes
     */
    public void updateJpaEntity(UnderwriterJpaEntity jpaEntity, Underwriter domain) {
        if (jpaEntity == null || domain == null) {
            return;
        }
        
        // Update mutable fields from domain
        jpaEntity.setStatus(domain.getStatus());
        jpaEntity.setUpdatedAt(domain.getUpdatedAt());
        jpaEntity.setVersion(domain.getVersion());
    }
    
    /**
     * Business method to check if mapping is valid
     */
    public boolean isValidMapping(Underwriter domain, UnderwriterJpaEntity jpaEntity) {
        if (domain == null || jpaEntity == null) {
            return false;
        }
        
        // Check critical business identifiers match
        return domain.getUnderwriterId().equals(jpaEntity.getUnderwriterId()) &&
               domain.getEmail().equals(jpaEntity.getEmail()) &&
               domain.getSpecialization().equals(jpaEntity.getSpecialization());
    }
    
    /**
     * Business method to validate domain-entity consistency
     */
    public void validateConsistency(Underwriter domain, UnderwriterJpaEntity jpaEntity) {
        if (!isValidMapping(domain, jpaEntity)) {
            throw new IllegalArgumentException(
                String.format("Domain model and JPA entity are inconsistent for underwriter: %s", 
                             domain != null ? domain.getUnderwriterId() : "null"));
        }
    }
    
    /**
     * Business method to check if underwriter can be safely mapped
     */
    public boolean canMapSafely(UnderwriterJpaEntity jpaEntity) {
        if (jpaEntity == null) {
            return false;
        }
        
        try {
            // Test if the entity can be converted to domain model
            Underwriter domain = toDomainModel(jpaEntity);
            return domain != null;
        } catch (Exception e) {
            // Domain validation failed
            return false;
        }
    }
}