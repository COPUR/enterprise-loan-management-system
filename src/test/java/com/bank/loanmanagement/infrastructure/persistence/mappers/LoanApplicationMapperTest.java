package com.bank.loanmanagement.infrastructure.persistence.mappers;

import com.bank.loanmanagement.domain.application.*;
import com.bank.loanmanagement.infrastructure.persistence.jpa.LoanApplicationJpaEntity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Nested;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.*;

/**
 * TDD Unit Tests for LoanApplicationMapper
 * 
 * Tests the mapping between domain models and infrastructure JPA entities.
 * Validates bidirectional conversion and data integrity.
 * 
 * Architecture Compliance Testing:
 * ✅ Hexagonal Architecture: Tests adapter pattern between domain and infrastructure
 * ✅ Clean Code: Tests single responsibility for mapping
 * ✅ Type Safety: Tests proper type conversion and null handling
 * ✅ DDD: Tests domain-infrastructure boundary preservation
 */
@DisplayName("LoanApplicationMapper Tests")
class LoanApplicationMapperTest {

    private LoanApplicationMapper mapper;

    @BeforeEach
    void setUp() {
        mapper = new LoanApplicationMapper();
    }

    @Nested
    @DisplayName("Domain to JPA Entity Mapping Tests")
    class DomainToJpaEntityMappingTests {

        @Test
        @DisplayName("Should map complete domain model to JPA entity")
        void shouldMapCompleteDomainModelToJpaEntity() {
            // Given - Complete domain model
            LoanApplication domain = LoanApplication.reconstruct(
                "APP1234567", 123L, LoanType.MORTGAGE, new BigDecimal("300000"), 360,
                "Home purchase", LocalDate.now().minusDays(5), ApplicationStatus.APPROVED, 
                ApplicationPriority.HIGH, new BigDecimal("8000"), 5, new BigDecimal("350000"),
                new BigDecimal("500000"), new BigDecimal("350000"), new BigDecimal("70000"),
                LocalDate.now().minusDays(1), "Approved - excellent credit", 
                new BigDecimal("280000"), new BigDecimal("3.75"), "UW001",
                LocalDateTime.now().minusDays(5), LocalDateTime.now().minusDays(1), 2
            );

            // When
            LoanApplicationJpaEntity jpaEntity = mapper.toJpaEntity(domain);

            // Then
            assertThat(jpaEntity).isNotNull();
            assertThat(jpaEntity.getApplicationId()).isEqualTo(domain.getApplicationId());
            assertThat(jpaEntity.getCustomerId()).isEqualTo(domain.getCustomerId());
            assertThat(jpaEntity.getLoanType()).isEqualTo(domain.getLoanType());
            assertThat(jpaEntity.getRequestedAmount()).isEqualTo(domain.getRequestedAmount());
            assertThat(jpaEntity.getRequestedTermMonths()).isEqualTo(domain.getRequestedTermMonths());
            assertThat(jpaEntity.getPurpose()).isEqualTo(domain.getPurpose());
            assertThat(jpaEntity.getApplicationDate()).isEqualTo(domain.getApplicationDate());
            assertThat(jpaEntity.getStatus()).isEqualTo(domain.getStatus());
            assertThat(jpaEntity.getAssignedUnderwriter()).isEqualTo(domain.getAssignedUnderwriter());
            assertThat(jpaEntity.getPriority()).isEqualTo(domain.getPriority());
            assertThat(jpaEntity.getMonthlyIncome()).isEqualTo(domain.getMonthlyIncome());
            assertThat(jpaEntity.getEmploymentYears()).isEqualTo(domain.getEmploymentYears());
            assertThat(jpaEntity.getCollateralValue()).isEqualTo(domain.getCollateralValue());
            assertThat(jpaEntity.getBusinessRevenue()).isEqualTo(domain.getBusinessRevenue());
            assertThat(jpaEntity.getPropertyValue()).isEqualTo(domain.getPropertyValue());
            assertThat(jpaEntity.getDownPayment()).isEqualTo(domain.getDownPayment());
            assertThat(jpaEntity.getDecisionDate()).isEqualTo(domain.getDecisionDate());
            assertThat(jpaEntity.getDecisionReason()).isEqualTo(domain.getDecisionReason());
            assertThat(jpaEntity.getApprovedAmount()).isEqualTo(domain.getApprovedAmount());
            assertThat(jpaEntity.getApprovedRate()).isEqualTo(domain.getApprovedRate());
            assertThat(jpaEntity.getCreatedAt()).isEqualTo(domain.getCreatedAt());
            assertThat(jpaEntity.getUpdatedAt()).isEqualTo(domain.getUpdatedAt());
            assertThat(jpaEntity.getVersion()).isEqualTo(domain.getVersion());
        }

        @Test
        @DisplayName("Should map minimal domain model to JPA entity")
        void shouldMapMinimalDomainModelToJpaEntity() {
            // Given - Minimal domain model (new application)
            LoanApplication domain = LoanApplication.create(
                "APP1234567", 123L, LoanType.PERSONAL, 
                new BigDecimal("50000"), 36, "Debt consolidation", "customer-portal"
            );

            // When
            LoanApplicationJpaEntity jpaEntity = mapper.toJpaEntity(domain);

            // Then
            assertThat(jpaEntity).isNotNull();
            assertThat(jpaEntity.getApplicationId()).isEqualTo("APP1234567");
            assertThat(jpaEntity.getCustomerId()).isEqualTo(123L);
            assertThat(jpaEntity.getLoanType()).isEqualTo(LoanType.PERSONAL);
            assertThat(jpaEntity.getRequestedAmount()).isEqualTo(new BigDecimal("50000"));
            assertThat(jpaEntity.getRequestedTermMonths()).isEqualTo(36);
            assertThat(jpaEntity.getPurpose()).isEqualTo("Debt consolidation");
            assertThat(jpaEntity.getApplicationDate()).isEqualTo(LocalDate.now());
            assertThat(jpaEntity.getStatus()).isEqualTo(ApplicationStatus.PENDING);
            assertThat(jpaEntity.getPriority()).isEqualTo(ApplicationPriority.STANDARD);
            
            // Optional fields should be null
            assertThat(jpaEntity.getAssignedUnderwriter()).isNull();
            assertThat(jpaEntity.getMonthlyIncome()).isNull();
            assertThat(jpaEntity.getDecisionDate()).isNull();
            assertThat(jpaEntity.getApprovedAmount()).isNull();
        }

        @Test
        @DisplayName("Should handle null domain model")
        void shouldHandleNullDomainModel() {
            // When
            LoanApplicationJpaEntity jpaEntity = mapper.toJpaEntity(null);

            // Then
            assertThat(jpaEntity).isNull();
        }
    }

    @Nested
    @DisplayName("JPA Entity to Domain Mapping Tests")
    class JpaEntityToDomainMappingTests {

        @Test
        @DisplayName("Should map complete JPA entity to domain model")
        void shouldMapCompleteJpaEntityToDomainModel() {
            // Given - Complete JPA entity
            LoanApplicationJpaEntity jpaEntity = LoanApplicationJpaEntity.builder()
                .applicationId("APP1234567")
                .customerId(123L)
                .loanType(LoanType.BUSINESS)
                .requestedAmount(new BigDecimal("250000"))
                .requestedTermMonths(60)
                .purpose("Equipment purchase")
                .applicationDate(LocalDate.now().minusDays(10))
                .status(ApplicationStatus.UNDER_REVIEW)
                .assignedUnderwriter("UW002")
                .priority(ApplicationPriority.HIGH)
                .monthlyIncome(new BigDecimal("15000"))
                .employmentYears(8)
                .collateralValue(new BigDecimal("300000"))
                .businessRevenue(new BigDecimal("1200000"))
                .propertyValue(null)
                .downPayment(null)
                .decisionDate(null)
                .decisionReason(null)
                .approvedAmount(null)
                .approvedRate(null)
                .createdAt(LocalDateTime.now().minusDays(10))
                .updatedAt(LocalDateTime.now().minusDays(2))
                .version(1)
                .build();

            // When
            LoanApplication domain = mapper.toDomainModel(jpaEntity);

            // Then
            assertThat(domain).isNotNull();
            assertThat(domain.getApplicationId()).isEqualTo(jpaEntity.getApplicationId());
            assertThat(domain.getCustomerId()).isEqualTo(jpaEntity.getCustomerId());
            assertThat(domain.getLoanType()).isEqualTo(jpaEntity.getLoanType());
            assertThat(domain.getRequestedAmount()).isEqualTo(jpaEntity.getRequestedAmount());
            assertThat(domain.getRequestedTermMonths()).isEqualTo(jpaEntity.getRequestedTermMonths());
            assertThat(domain.getPurpose()).isEqualTo(jpaEntity.getPurpose());
            assertThat(domain.getApplicationDate()).isEqualTo(jpaEntity.getApplicationDate());
            assertThat(domain.getStatus()).isEqualTo(jpaEntity.getStatus());
            assertThat(domain.getAssignedUnderwriter()).isEqualTo(jpaEntity.getAssignedUnderwriter());
            assertThat(domain.getPriority()).isEqualTo(jpaEntity.getPriority());
            assertThat(domain.getMonthlyIncome()).isEqualTo(jpaEntity.getMonthlyIncome());
            assertThat(domain.getEmploymentYears()).isEqualTo(jpaEntity.getEmploymentYears());
            assertThat(domain.getCollateralValue()).isEqualTo(jpaEntity.getCollateralValue());
            assertThat(domain.getBusinessRevenue()).isEqualTo(jpaEntity.getBusinessRevenue());
            assertThat(domain.getCreatedAt()).isEqualTo(jpaEntity.getCreatedAt());
            assertThat(domain.getUpdatedAt()).isEqualTo(jpaEntity.getUpdatedAt());
            assertThat(domain.getVersion()).isEqualTo(jpaEntity.getVersion());
            
            // Domain model should have no uncommitted events when reconstructed
            assertThat(domain.getUncommittedEvents()).isEmpty();
        }

        @Test
        @DisplayName("Should handle null JPA entity")
        void shouldHandleNullJpaEntity() {
            // When
            LoanApplication domain = mapper.toDomainModel(null);

            // Then
            assertThat(domain).isNull();
        }
    }

    @Nested
    @DisplayName("Bidirectional Mapping Tests")
    class BidirectionalMappingTests {

        @Test
        @DisplayName("Should preserve data integrity in round-trip conversion")
        void shouldPreserveDataIntegrityInRoundTripConversion() {
            // Given - Original domain model
            LoanApplication originalDomain = LoanApplication.reconstruct(
                "APP1234567", 123L, LoanType.AUTO_LOAN, new BigDecimal("45000"), 48,
                "Vehicle purchase", LocalDate.now().minusDays(3), ApplicationStatus.APPROVED,
                ApplicationPriority.STANDARD, new BigDecimal("6000"), 4, new BigDecimal("50000"),
                null, null, null, LocalDate.now().minusDays(1), "Approved - good credit",
                new BigDecimal("42000"), new BigDecimal("4.25"), "UW003",
                LocalDateTime.now().minusDays(3), LocalDateTime.now().minusDays(1), 1
            );

            // When - Round-trip conversion
            LoanApplicationJpaEntity jpaEntity = mapper.toJpaEntity(originalDomain);
            LoanApplication convertedDomain = mapper.toDomainModel(jpaEntity);

            // Then - Data should be preserved
            assertThat(convertedDomain.getApplicationId()).isEqualTo(originalDomain.getApplicationId());
            assertThat(convertedDomain.getCustomerId()).isEqualTo(originalDomain.getCustomerId());
            assertThat(convertedDomain.getLoanType()).isEqualTo(originalDomain.getLoanType());
            assertThat(convertedDomain.getRequestedAmount()).isEqualTo(originalDomain.getRequestedAmount());
            assertThat(convertedDomain.getStatus()).isEqualTo(originalDomain.getStatus());
            assertThat(convertedDomain.getApprovedAmount()).isEqualTo(originalDomain.getApprovedAmount());
            assertThat(convertedDomain.getApprovedRate()).isEqualTo(originalDomain.getApprovedRate());
            assertThat(convertedDomain.getAssignedUnderwriter()).isEqualTo(originalDomain.getAssignedUnderwriter());
            
            // Business methods should work the same
            assertThat(convertedDomain.getAssetValue()).isEqualTo(originalDomain.getAssetValue());
            assertThat(convertedDomain.calculateLoanToValueRatio()).isEqualTo(originalDomain.calculateLoanToValueRatio());
        }

        @Test
        @DisplayName("Should handle business logic after round-trip conversion")
        void shouldHandleBusinessLogicAfterRoundTripConversion() {
            // Given - Pending application
            LoanApplication originalDomain = LoanApplication.create(
                "APP1234567", 123L, LoanType.PERSONAL, 
                new BigDecimal("30000"), 24, "Home improvement", "mobile-app"
            );
            originalDomain.markEventsAsCommitted(); // Simulate persistence

            // When - Round-trip conversion
            LoanApplicationJpaEntity jpaEntity = mapper.toJpaEntity(originalDomain);
            LoanApplication convertedDomain = mapper.toDomainModel(jpaEntity);

            // Then - Business logic should work correctly
            assertThat(convertedDomain.getStatus()).isEqualTo(ApplicationStatus.PENDING);
            
            // Should be able to assign underwriter
            convertedDomain.assignUnderwriter("UW001", "system", "Auto-assignment");
            assertThat(convertedDomain.getStatus()).isEqualTo(ApplicationStatus.UNDER_REVIEW);
            assertThat(convertedDomain.getAssignedUnderwriter()).isEqualTo("UW001");
            assertThat(convertedDomain.getUncommittedEvents()).hasSize(1);
        }
    }

    @Nested
    @DisplayName("Update JPA Entity Tests")
    class UpdateJpaEntityTests {

        @Test
        @DisplayName("Should update JPA entity with domain changes")
        void shouldUpdateJpaEntityWithDomainChanges() {
            // Given - Existing JPA entity
            LoanApplicationJpaEntity jpaEntity = LoanApplicationJpaEntity.builder()
                .applicationId("APP1234567")
                .customerId(123L)
                .loanType(LoanType.PERSONAL)
                .status(ApplicationStatus.PENDING)
                .priority(ApplicationPriority.STANDARD)
                .version(0)
                .build();

            // And modified domain model
            LoanApplication domain = mapper.toDomainModel(jpaEntity);
            domain.assignUnderwriter("UW001", "manager", "Assignment");
            domain.approve(new BigDecimal("25000"), new BigDecimal("5.5"), 
                          "Good credit", "UW001");

            // When
            mapper.updateJpaEntity(jpaEntity, domain);

            // Then
            assertThat(jpaEntity.getStatus()).isEqualTo(ApplicationStatus.APPROVED);
            assertThat(jpaEntity.getAssignedUnderwriter()).isEqualTo("UW001");
            assertThat(jpaEntity.getApprovedAmount()).isEqualTo(new BigDecimal("25000"));
            assertThat(jpaEntity.getApprovedRate()).isEqualTo(new BigDecimal("5.5"));
            assertThat(jpaEntity.getDecisionDate()).isEqualTo(LocalDate.now());
            assertThat(jpaEntity.getDecisionReason()).isEqualTo("Good credit");
            
            // Immutable fields should not change
            assertThat(jpaEntity.getApplicationId()).isEqualTo("APP1234567");
            assertThat(jpaEntity.getCustomerId()).isEqualTo(123L);
            assertThat(jpaEntity.getLoanType()).isEqualTo(LoanType.PERSONAL);
        }

        @Test
        @DisplayName("Should handle null parameters gracefully")
        void shouldHandleNullParametersGracefully() {
            // Given
            LoanApplicationJpaEntity jpaEntity = LoanApplicationJpaEntity.builder()
                .applicationId("APP1234567")
                .status(ApplicationStatus.PENDING)
                .build();

            // When & Then - Should not throw exceptions
            assertThatCode(() -> mapper.updateJpaEntity(null, null)).doesNotThrowAnyException();
            assertThatCode(() -> mapper.updateJpaEntity(jpaEntity, null)).doesNotThrowAnyException();
            assertThatCode(() -> mapper.updateJpaEntity(null, 
                LoanApplication.create("APP1234567", 123L, LoanType.PERSONAL, 
                                     new BigDecimal("50000"), 36, "Purpose", "submitter")))
                .doesNotThrowAnyException();
        }
    }

    @Nested
    @DisplayName("Validation Tests")
    class ValidationTests {

        @Test
        @DisplayName("Should validate mapping consistency")
        void shouldValidateMappingConsistency() {
            // Given - Valid domain and JPA entity
            LoanApplication domain = LoanApplication.create(
                "APP1234567", 123L, LoanType.BUSINESS, 
                new BigDecimal("100000"), 60, "Equipment", "portal"
            );
            LoanApplicationJpaEntity jpaEntity = mapper.toJpaEntity(domain);

            // When & Then
            assertThat(mapper.isValidMapping(domain, jpaEntity)).isTrue();
        }

        @Test
        @DisplayName("Should detect inconsistent mapping")
        void shouldDetectInconsistentMapping() {
            // Given - Inconsistent domain and JPA entity
            LoanApplication domain = LoanApplication.create(
                "APP1234567", 123L, LoanType.BUSINESS, 
                new BigDecimal("100000"), 60, "Equipment", "portal"
            );
            LoanApplicationJpaEntity jpaEntity = LoanApplicationJpaEntity.builder()
                .applicationId("APP9999999") // Different ID
                .customerId(456L) // Different customer
                .loanType(LoanType.PERSONAL) // Different type
                .build();

            // When & Then
            assertThat(mapper.isValidMapping(domain, jpaEntity)).isFalse();
        }

        @Test
        @DisplayName("Should throw exception for inconsistent data")
        void shouldThrowExceptionForInconsistentData() {
            // Given - Inconsistent domain and JPA entity
            LoanApplication domain = LoanApplication.create(
                "APP1234567", 123L, LoanType.BUSINESS, 
                new BigDecimal("100000"), 60, "Equipment", "portal"
            );
            LoanApplicationJpaEntity jpaEntity = LoanApplicationJpaEntity.builder()
                .applicationId("APP9999999")
                .customerId(456L)
                .loanType(LoanType.PERSONAL)
                .build();

            // When & Then
            assertThatThrownBy(() -> mapper.validateConsistency(domain, jpaEntity))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Domain model and JPA entity are inconsistent");
        }

        @Test
        @DisplayName("Should handle null values in validation")
        void shouldHandleNullValuesInValidation() {
            // Given
            LoanApplication domain = LoanApplication.create(
                "APP1234567", 123L, LoanType.PERSONAL, 
                new BigDecimal("50000"), 36, "Purpose", "submitter"
            );

            // When & Then
            assertThat(mapper.isValidMapping(null, null)).isFalse();
            assertThat(mapper.isValidMapping(domain, null)).isFalse();
            assertThat(mapper.isValidMapping(null, new LoanApplicationJpaEntity())).isFalse();
        }
    }
}