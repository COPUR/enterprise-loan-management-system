package com.bank.loanmanagement.loan.infrastructure.persistence.mappers;

import com.bank.loanmanagement.loan.domain.staff.*;
import com.bank.loanmanagement.loan.infrastructure.persistence.jpa.UnderwriterJpaEntity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Nested;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.*;

/**
 * TDD Unit Tests for UnderwriterMapper
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
@DisplayName("UnderwriterMapper Tests")
class UnderwriterMapperTest {

    private UnderwriterMapper mapper;

    @BeforeEach
    void setUp() {
        mapper = new UnderwriterMapper();
    }

    @Nested
    @DisplayName("Domain to JPA Entity Mapping Tests")
    class DomainToJpaEntityMappingTests {

        @Test
        @DisplayName("Should map complete domain model to JPA entity")
        void shouldMapCompleteDomainModelToJpaEntity() {
            // Given - Complete domain model
            Underwriter domain = Underwriter.reconstruct(
                "UW001", "John", "Smith", "john.smith@bank.com", "+1234567890",
                UnderwriterSpecialization.MORTGAGES, 10, new BigDecimal("5000000"),
                EmployeeStatus.ACTIVE, LocalDate.now().minusYears(10),
                LocalDateTime.now().minusYears(10), LocalDateTime.now().minusMonths(6), 3
            );

            // When
            UnderwriterJpaEntity jpaEntity = mapper.toJpaEntity(domain);

            // Then
            assertThat(jpaEntity).isNotNull();
            assertThat(jpaEntity.getUnderwriterId()).isEqualTo(domain.getUnderwriterId());
            assertThat(jpaEntity.getFirstName()).isEqualTo(domain.getFirstName());
            assertThat(jpaEntity.getLastName()).isEqualTo(domain.getLastName());
            assertThat(jpaEntity.getEmail()).isEqualTo(domain.getEmail());
            assertThat(jpaEntity.getPhone()).isEqualTo(domain.getPhone());
            assertThat(jpaEntity.getSpecialization()).isEqualTo(domain.getSpecialization());
            assertThat(jpaEntity.getYearsExperience()).isEqualTo(domain.getYearsExperience());
            assertThat(jpaEntity.getApprovalLimit()).isEqualTo(domain.getApprovalLimit());
            assertThat(jpaEntity.getStatus()).isEqualTo(domain.getStatus());
            assertThat(jpaEntity.getHireDate()).isEqualTo(domain.getHireDate());
            assertThat(jpaEntity.getCreatedAt()).isEqualTo(domain.getCreatedAt());
            assertThat(jpaEntity.getUpdatedAt()).isEqualTo(domain.getUpdatedAt());
            assertThat(jpaEntity.getVersion()).isEqualTo(domain.getVersion());
        }

        @Test
        @DisplayName("Should map new domain model to JPA entity")
        void shouldMapNewDomainModelToJpaEntity() {
            // Given - New domain model
            Underwriter domain = Underwriter.create(
                "UW002", "Jane", "Doe", "jane.doe@bank.com", null,
                UnderwriterSpecialization.BUSINESS_LOANS, 7, new BigDecimal("2500000"),
                LocalDate.now().minusYears(7)
            );

            // When
            UnderwriterJpaEntity jpaEntity = mapper.toJpaEntity(domain);

            // Then
            assertThat(jpaEntity).isNotNull();
            assertThat(jpaEntity.getUnderwriterId()).isEqualTo("UW002");
            assertThat(jpaEntity.getFirstName()).isEqualTo("Jane");
            assertThat(jpaEntity.getLastName()).isEqualTo("Doe");
            assertThat(jpaEntity.getEmail()).isEqualTo("jane.doe@bank.com");
            assertThat(jpaEntity.getPhone()).isNull(); // Optional field
            assertThat(jpaEntity.getSpecialization()).isEqualTo(UnderwriterSpecialization.BUSINESS_LOANS);
            assertThat(jpaEntity.getYearsExperience()).isEqualTo(7);
            assertThat(jpaEntity.getApprovalLimit()).isEqualTo(new BigDecimal("2500000"));
            assertThat(jpaEntity.getStatus()).isEqualTo(EmployeeStatus.ACTIVE);
            assertThat(jpaEntity.getHireDate()).isEqualTo(LocalDate.now().minusYears(7));
            assertThat(jpaEntity.getVersion()).isEqualTo(0);
        }

        @Test
        @DisplayName("Should handle null domain model")
        void shouldHandleNullDomainModel() {
            // When
            UnderwriterJpaEntity jpaEntity = mapper.toJpaEntity(null);

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
            UnderwriterJpaEntity jpaEntity = UnderwriterJpaEntity.builder()
                .underwriterId("UW003")
                .firstName("Bob")
                .lastName("Johnson")
                .email("bob.johnson@bank.com")
                .phone("+9876543210")
                .specialization(UnderwriterSpecialization.PERSONAL_LOANS)
                .yearsExperience(3)
                .approvalLimit(new BigDecimal("75000"))
                .status(EmployeeStatus.ACTIVE)
                .hireDate(LocalDate.now().minusYears(3))
                .createdAt(LocalDateTime.now().minusYears(3))
                .updatedAt(LocalDateTime.now().minusWeeks(2))
                .version(1)
                .build();

            // When
            Underwriter domain = mapper.toDomainModel(jpaEntity);

            // Then
            assertThat(domain).isNotNull();
            assertThat(domain.getUnderwriterId()).isEqualTo(jpaEntity.getUnderwriterId());
            assertThat(domain.getFirstName()).isEqualTo(jpaEntity.getFirstName());
            assertThat(domain.getLastName()).isEqualTo(jpaEntity.getLastName());
            assertThat(domain.getEmail()).isEqualTo(jpaEntity.getEmail());
            assertThat(domain.getPhone()).isEqualTo(jpaEntity.getPhone());
            assertThat(domain.getSpecialization()).isEqualTo(jpaEntity.getSpecialization());
            assertThat(domain.getYearsExperience()).isEqualTo(jpaEntity.getYearsExperience());
            assertThat(domain.getApprovalLimit()).isEqualTo(jpaEntity.getApprovalLimit());
            assertThat(domain.getStatus()).isEqualTo(jpaEntity.getStatus());
            assertThat(domain.getHireDate()).isEqualTo(jpaEntity.getHireDate());
            assertThat(domain.getCreatedAt()).isEqualTo(jpaEntity.getCreatedAt());
            assertThat(domain.getUpdatedAt()).isEqualTo(jpaEntity.getUpdatedAt());
            assertThat(domain.getVersion()).isEqualTo(jpaEntity.getVersion());
        }

        @Test
        @DisplayName("Should handle minimal JPA entity")
        void shouldHandleMinimalJpaEntity() {
            // Given - Minimal JPA entity
            UnderwriterJpaEntity jpaEntity = UnderwriterJpaEntity.builder()
                .underwriterId("UW004")
                .firstName("Min")
                .lastName("Imal")
                .email("minimal@bank.com")
                .phone(null) // Optional
                .specialization(UnderwriterSpecialization.PERSONAL_LOANS)
                .yearsExperience(1)
                .approvalLimit(new BigDecimal("25000"))
                .status(EmployeeStatus.ACTIVE)
                .hireDate(LocalDate.now().minusYears(1))
                .createdAt(LocalDateTime.now().minusYears(1))
                .updatedAt(LocalDateTime.now().minusYears(1))
                .version(0)
                .build();

            // When
            Underwriter domain = mapper.toDomainModel(jpaEntity);

            // Then
            assertThat(domain).isNotNull();
            assertThat(domain.getUnderwriterId()).isEqualTo("UW004");
            assertThat(domain.getPhone()).isNull();
            assertThat(domain.getExperienceLevel()).isEqualTo("Junior");
            assertThat(domain.isSenior()).isFalse();
        }

        @Test
        @DisplayName("Should handle null JPA entity")
        void shouldHandleNullJpaEntity() {
            // When
            Underwriter domain = mapper.toDomainModel(null);

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
            Underwriter originalDomain = Underwriter.reconstruct(
                "UW005", "Alice", "Cooper", "alice.cooper@bank.com", "+5555555555",
                UnderwriterSpecialization.MORTGAGES, 12, new BigDecimal("8000000"),
                EmployeeStatus.ACTIVE, LocalDate.now().minusYears(12),
                LocalDateTime.now().minusYears(12), LocalDateTime.now().minusDays(30), 2
            );

            // When - Round-trip conversion
            UnderwriterJpaEntity jpaEntity = mapper.toJpaEntity(originalDomain);
            Underwriter convertedDomain = mapper.toDomainModel(jpaEntity);

            // Then - Data should be preserved
            assertThat(convertedDomain.getUnderwriterId()).isEqualTo(originalDomain.getUnderwriterId());
            assertThat(convertedDomain.getFirstName()).isEqualTo(originalDomain.getFirstName());
            assertThat(convertedDomain.getLastName()).isEqualTo(originalDomain.getLastName());
            assertThat(convertedDomain.getEmail()).isEqualTo(originalDomain.getEmail());
            assertThat(convertedDomain.getSpecialization()).isEqualTo(originalDomain.getSpecialization());
            assertThat(convertedDomain.getYearsExperience()).isEqualTo(originalDomain.getYearsExperience());
            assertThat(convertedDomain.getApprovalLimit()).isEqualTo(originalDomain.getApprovalLimit());
            assertThat(convertedDomain.getStatus()).isEqualTo(originalDomain.getStatus());
            
            // Business methods should work the same
            assertThat(convertedDomain.getFullName()).isEqualTo(originalDomain.getFullName());
            assertThat(convertedDomain.isSenior()).isEqualTo(originalDomain.isSenior());
            assertThat(convertedDomain.getExperienceLevel()).isEqualTo(originalDomain.getExperienceLevel());
            assertThat(convertedDomain.canApprove(new BigDecimal("1000000")))
                .isEqualTo(originalDomain.canApprove(new BigDecimal("1000000")));
        }

        @Test
        @DisplayName("Should handle business logic after round-trip conversion")
        void shouldHandleBusinessLogicAfterRoundTripConversion() {
            // Given - Active underwriter
            Underwriter originalDomain = Underwriter.create(
                "UW006", "Test", "User", "test.user@bank.com", null,
                UnderwriterSpecialization.BUSINESS_LOANS, 6, new BigDecimal("1500000"),
                LocalDate.now().minusYears(6)
            );

            // When - Round-trip conversion
            UnderwriterJpaEntity jpaEntity = mapper.toJpaEntity(originalDomain);
            Underwriter convertedDomain = mapper.toDomainModel(jpaEntity);

            // Then - Business logic should work correctly
            assertThat(convertedDomain.getStatus()).isEqualTo(EmployeeStatus.ACTIVE);
            assertThat(convertedDomain.isAvailableForNewLoans()).isTrue();
            assertThat(convertedDomain.canHandleUrgentCases()).isTrue(); // Senior + Active
            assertThat(convertedDomain.specializesIn("BUSINESS")).isTrue();
            
            // Should be able to approve appropriate amounts
            assertThat(convertedDomain.canApprove(new BigDecimal("1000000"))).isTrue();
            assertThat(convertedDomain.canApprove(new BigDecimal("2000000"))).isFalse();
            
            // Should be able to update status
            convertedDomain.updateStatus(EmployeeStatus.ON_LEAVE);
            assertThat(convertedDomain.getStatus()).isEqualTo(EmployeeStatus.ON_LEAVE);
            assertThat(convertedDomain.isAvailableForNewLoans()).isFalse();
        }
    }

    @Nested
    @DisplayName("Update JPA Entity Tests")
    class UpdateJpaEntityTests {

        @Test
        @DisplayName("Should update JPA entity with domain changes")
        void shouldUpdateJpaEntityWithDomainChanges() {
            // Given - Existing JPA entity
            UnderwriterJpaEntity jpaEntity = UnderwriterJpaEntity.builder()
                .underwriterId("UW007")
                .firstName("Update")
                .lastName("Test")
                .email("update.test@bank.com")
                .specialization(UnderwriterSpecialization.PERSONAL_LOANS)
                .yearsExperience(4)
                .approvalLimit(new BigDecimal("80000"))
                .status(EmployeeStatus.ACTIVE)
                .hireDate(LocalDate.now().minusYears(4))
                .createdAt(LocalDateTime.now().minusYears(4))
                .updatedAt(LocalDateTime.now().minusYears(4))
                .version(0)
                .build();

            LocalDateTime originalUpdatedAt = jpaEntity.getUpdatedAt();

            // And modified domain model
            Underwriter domain = mapper.toDomainModel(jpaEntity);
            domain.updateStatus(EmployeeStatus.INACTIVE);

            // When
            mapper.updateJpaEntity(jpaEntity, domain);

            // Then
            assertThat(jpaEntity.getStatus()).isEqualTo(EmployeeStatus.INACTIVE);
            assertThat(jpaEntity.getUpdatedAt()).isAfter(originalUpdatedAt);
            assertThat(jpaEntity.getVersion()).isEqualTo(domain.getVersion());
            
            // Immutable fields should not change
            assertThat(jpaEntity.getUnderwriterId()).isEqualTo("UW007");
            assertThat(jpaEntity.getFirstName()).isEqualTo("Update");
            assertThat(jpaEntity.getLastName()).isEqualTo("Test");
            assertThat(jpaEntity.getEmail()).isEqualTo("update.test@bank.com");
            assertThat(jpaEntity.getSpecialization()).isEqualTo(UnderwriterSpecialization.PERSONAL_LOANS);
            assertThat(jpaEntity.getYearsExperience()).isEqualTo(4);
            assertThat(jpaEntity.getApprovalLimit()).isEqualTo(new BigDecimal("80000"));
        }

        @Test
        @DisplayName("Should handle null parameters gracefully")
        void shouldHandleNullParametersGracefully() {
            // Given
            UnderwriterJpaEntity jpaEntity = UnderwriterJpaEntity.builder()
                .underwriterId("UW008")
                .status(EmployeeStatus.ACTIVE)
                .build();

            // When & Then - Should not throw exceptions
            assertThatCode(() -> mapper.updateJpaEntity(null, null)).doesNotThrowAnyException();
            assertThatCode(() -> mapper.updateJpaEntity(jpaEntity, null)).doesNotThrowAnyException();
            assertThatCode(() -> mapper.updateJpaEntity(null, 
                Underwriter.create("UW008", "Test", "User", "test@bank.com", null,
                                 UnderwriterSpecialization.PERSONAL_LOANS, 5, 
                                 new BigDecimal("100000"), LocalDate.now().minusYears(5))))
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
            Underwriter domain = Underwriter.create(
                "UW009", "Valid", "Test", "valid.test@bank.com", null,
                UnderwriterSpecialization.MORTGAGES, 8, new BigDecimal("3000000"),
                LocalDate.now().minusYears(8)
            );
            UnderwriterJpaEntity jpaEntity = mapper.toJpaEntity(domain);

            // When & Then
            assertThat(mapper.isValidMapping(domain, jpaEntity)).isTrue();
        }

        @Test
        @DisplayName("Should detect inconsistent mapping")
        void shouldDetectInconsistentMapping() {
            // Given - Inconsistent domain and JPA entity
            Underwriter domain = Underwriter.create(
                "UW010", "Inconsistent", "Test", "inconsistent@bank.com", null,
                UnderwriterSpecialization.BUSINESS_LOANS, 6, new BigDecimal("1000000"),
                LocalDate.now().minusYears(6)
            );
            UnderwriterJpaEntity jpaEntity = UnderwriterJpaEntity.builder()
                .underwriterId("UW999") // Different ID
                .email("different@bank.com") // Different email
                .specialization(UnderwriterSpecialization.PERSONAL_LOANS) // Different specialization
                .build();

            // When & Then
            assertThat(mapper.isValidMapping(domain, jpaEntity)).isFalse();
        }

        @Test
        @DisplayName("Should throw exception for inconsistent data")
        void shouldThrowExceptionForInconsistentData() {
            // Given - Inconsistent domain and JPA entity
            Underwriter domain = Underwriter.create(
                "UW011", "Exception", "Test", "exception@bank.com", null,
                UnderwriterSpecialization.MORTGAGES, 9, new BigDecimal("4000000"),
                LocalDate.now().minusYears(9)
            );
            UnderwriterJpaEntity jpaEntity = UnderwriterJpaEntity.builder()
                .underwriterId("UW999")
                .email("different@bank.com")
                .specialization(UnderwriterSpecialization.PERSONAL_LOANS)
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
            Underwriter domain = Underwriter.create(
                "UW012", "Null", "Test", "null.test@bank.com", null,
                UnderwriterSpecialization.PERSONAL_LOANS, 2, new BigDecimal("50000"),
                LocalDate.now().minusYears(2)
            );

            // When & Then
            assertThat(mapper.isValidMapping(null, null)).isFalse();
            assertThat(mapper.isValidMapping(domain, null)).isFalse();
            assertThat(mapper.isValidMapping(null, new UnderwriterJpaEntity())).isFalse();
        }

        @Test
        @DisplayName("Should check if underwriter can be safely mapped")
        void shouldCheckIfUnderwriterCanBeSafelyMapped() {
            // Given - Valid JPA entity
            UnderwriterJpaEntity validEntity = UnderwriterJpaEntity.builder()
                .underwriterId("UW013")
                .firstName("Safe")
                .lastName("Mapping")
                .email("safe.mapping@bank.com")
                .specialization(UnderwriterSpecialization.BUSINESS_LOANS)
                .yearsExperience(7)
                .approvalLimit(new BigDecimal("2000000"))
                .status(EmployeeStatus.ACTIVE)
                .hireDate(LocalDate.now().minusYears(7))
                .createdAt(LocalDateTime.now().minusYears(7))
                .updatedAt(LocalDateTime.now().minusYears(7))
                .version(0)
                .build();

            // And invalid JPA entity (would fail domain validation)
            UnderwriterJpaEntity invalidEntity = UnderwriterJpaEntity.builder()
                .underwriterId("INVALID") // Invalid format
                .firstName("Invalid")
                .lastName("Entity")
                .email("invalid-email") // Invalid email format
                .specialization(UnderwriterSpecialization.PERSONAL_LOANS)
                .yearsExperience(-1) // Invalid experience
                .approvalLimit(new BigDecimal("500")) // Invalid approval limit
                .status(EmployeeStatus.ACTIVE)
                .hireDate(LocalDate.now().plusDays(1)) // Future hire date
                .build();

            // When & Then
            assertThat(mapper.canMapSafely(validEntity)).isTrue();
            assertThat(mapper.canMapSafely(invalidEntity)).isFalse();
            assertThat(mapper.canMapSafely(null)).isFalse();
        }
    }

    @Nested
    @DisplayName("Specialization Mapping Tests")
    class SpecializationMappingTests {

        @Test
        @DisplayName("Should correctly map all specialization types")
        void shouldCorrectlyMapAllSpecializationTypes() {
            LocalDate hireDate = LocalDate.now().minusYears(5);
            
            // Test each specialization
            for (UnderwriterSpecialization specialization : UnderwriterSpecialization.values()) {
                // Given
                Underwriter domain = Underwriter.create(
                    "UW" + specialization.ordinal(), "Test", "Spec", 
                    "test.spec" + specialization.ordinal() + "@bank.com", null,
                    specialization, 5, new BigDecimal("1000000"), hireDate
                );

                // When
                UnderwriterJpaEntity jpaEntity = mapper.toJpaEntity(domain);
                Underwriter convertedDomain = mapper.toDomainModel(jpaEntity);

                // Then
                assertThat(jpaEntity.getSpecialization()).isEqualTo(specialization);
                assertThat(convertedDomain.getSpecialization()).isEqualTo(specialization);
                assertThat(convertedDomain.getSpecialization().getDisplayName())
                    .isEqualTo(domain.getSpecialization().getDisplayName());
            }
        }
    }

    @Nested
    @DisplayName("Status Mapping Tests")
    class StatusMappingTests {

        @Test
        @DisplayName("Should correctly map all employee status types")
        void shouldCorrectlyMapAllEmployeeStatusTypes() {
            LocalDate hireDate = LocalDate.now().minusYears(3);
            
            // Test each status
            for (EmployeeStatus status : EmployeeStatus.values()) {
                // Given
                Underwriter domain = Underwriter.reconstruct(
                    "UW" + status.ordinal(), "Test", "Status", 
                    "test.status" + status.ordinal() + "@bank.com", null,
                    UnderwriterSpecialization.PERSONAL_LOANS, 3, new BigDecimal("75000"),
                    status, hireDate, LocalDateTime.now().minusYears(3), 
                    LocalDateTime.now(), 1
                );

                // When
                UnderwriterJpaEntity jpaEntity = mapper.toJpaEntity(domain);
                Underwriter convertedDomain = mapper.toDomainModel(jpaEntity);

                // Then
                assertThat(jpaEntity.getStatus()).isEqualTo(status);
                assertThat(convertedDomain.getStatus()).isEqualTo(status);
                assertThat(convertedDomain.getStatus().getDisplayName())
                    .isEqualTo(domain.getStatus().getDisplayName());
            }
        }
    }
}