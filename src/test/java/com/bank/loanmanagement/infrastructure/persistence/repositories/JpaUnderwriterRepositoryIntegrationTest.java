package com.bank.loanmanagement.infrastructure.persistence.repositories;

import com.bank.loanmanagement.domain.staff.*;
import com.bank.loanmanagement.infrastructure.persistence.jpa.UnderwriterJpaEntity;
import com.bank.loanmanagement.infrastructure.persistence.mappers.UnderwriterMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;

/**
 * Integration Tests for JPA Underwriter Repository
 * 
 * Tests the infrastructure adapter's integration with the database.
 * Validates that domain operations translate correctly to persistence operations.
 * 
 * Architecture Validation:
 * ✅ Hexagonal Architecture: Infrastructure adapter integration testing
 * ✅ DDD: Domain-infrastructure boundary testing
 * ✅ Repository Pattern: CRUD and query operations validation
 * ✅ Data Integrity: Round-trip conversion and persistence validation
 */
@DataJpaTest
@ActiveProfiles("test")
@TestPropertySource(properties = {
    "spring.jpa.hibernate.ddl-auto=create-drop",
    "spring.jpa.show-sql=true",
    "spring.datasource.url=jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE",
    "logging.level.org.hibernate.SQL=DEBUG"
})
@DisplayName("JPA Underwriter Repository Integration Tests")
class JpaUnderwriterRepositoryIntegrationTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private UnderwriterJpaRepository jpaRepository;

    private JpaUnderwriterRepository repository;
    private UnderwriterMapper mapper;

    @BeforeEach
    void setUp() {
        mapper = new UnderwriterMapper();
        repository = new JpaUnderwriterRepository(jpaRepository, mapper);
    }

    @Nested
    @DisplayName("Basic CRUD Operations")
    class BasicCrudOperations {

        @Test
        @DisplayName("Should save and retrieve underwriter successfully")
        void shouldSaveAndRetrieveUnderwriter() {
            // Given
            Underwriter underwriter = createTestUnderwriter("UW001", "John Smith");

            // When
            Underwriter savedUnderwriter = repository.save(underwriter);
            Optional<Underwriter> retrievedUnderwriter = repository.findById("UW001");

            // Then
            assertThat(savedUnderwriter).isNotNull();
            assertThat(savedUnderwriter.getUnderwriterId()).isEqualTo("UW001");
            assertThat(retrievedUnderwriter).isPresent();
            assertThat(retrievedUnderwriter.get().getUnderwriterId()).isEqualTo("UW001");
            assertThat(retrievedUnderwriter.get().getFullName()).isEqualTo("John Smith");
            assertThat(retrievedUnderwriter.get().getSpecialization()).isEqualTo(UnderwriterSpecialization.PERSONAL_LOANS);
            assertThat(retrievedUnderwriter.get().getApprovalLimit()).isEqualTo(new BigDecimal("100000"));
        }

        @Test
        @DisplayName("Should update existing underwriter")
        void shouldUpdateExistingUnderwriter() {
            // Given
            Underwriter underwriter = createTestUnderwriter("UW002", "Jane Doe");
            repository.save(underwriter);

            // Modify the underwriter
            underwriter.updateApprovalLimit(new BigDecimal("150000"));
            underwriter.updateSpecialization(UnderwriterSpecialization.MORTGAGE_LOANS);

            // When
            Underwriter updatedUnderwriter = repository.save(underwriter);
            Optional<Underwriter> retrievedUnderwriter = repository.findById("UW002");

            // Then
            assertThat(updatedUnderwriter.getApprovalLimit()).isEqualTo(new BigDecimal("150000"));
            assertThat(updatedUnderwriter.getSpecialization()).isEqualTo(UnderwriterSpecialization.MORTGAGE_LOANS);
            assertThat(retrievedUnderwriter).isPresent();
            assertThat(retrievedUnderwriter.get().getApprovalLimit()).isEqualTo(new BigDecimal("150000"));
            assertThat(retrievedUnderwriter.get().getSpecialization()).isEqualTo(UnderwriterSpecialization.MORTGAGE_LOANS);
        }

        @Test
        @DisplayName("Should delete underwriter by ID")
        void shouldDeleteUnderwriterById() {
            // Given
            Underwriter underwriter = createTestUnderwriter("UW003", "Bob Johnson");
            repository.save(underwriter);
            assertThat(repository.existsById("UW003")).isTrue();

            // When
            repository.deleteById("UW003");

            // Then
            assertThat(repository.existsById("UW003")).isFalse();
            assertThat(repository.findById("UW003")).isEmpty();
        }

        @Test
        @DisplayName("Should return empty optional for non-existent ID")
        void shouldReturnEmptyOptionalForNonExistentId() {
            // When
            Optional<Underwriter> result = repository.findById("NONEXISTENT");

            // Then
            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("Query Operations")
    class QueryOperations {

        @BeforeEach
        void setupTestData() {
            // Create multiple test underwriters with different characteristics
            Underwriter uw1 = createTestUnderwriter("UW100", "Alice Brown");
            uw1.updateSpecialization(UnderwriterSpecialization.PERSONAL_LOANS);
            uw1.updateApprovalLimit(new BigDecimal("75000"));
            repository.save(uw1);

            Underwriter uw2 = createTestUnderwriter("UW101", "Charlie Davis");
            uw2.updateSpecialization(UnderwriterSpecialization.MORTGAGE_LOANS);
            uw2.updateApprovalLimit(new BigDecimal("500000"));
            repository.save(uw2);

            Underwriter uw3 = createTestUnderwriter("UW102", "Diana Evans");
            uw3.updateSpecialization(UnderwriterSpecialization.BUSINESS_LOANS);
            uw3.updateApprovalLimit(new BigDecimal("250000"));
            repository.save(uw3);

            Underwriter uw4 = createTestUnderwriter("UW103", "Frank Wilson");
            uw4.updateSpecialization(UnderwriterSpecialization.PERSONAL_LOANS);
            uw4.updateApprovalLimit(new BigDecimal("50000"));
            uw4.suspend("Performance review");
            repository.save(uw4);

            // Force flush to ensure data is persisted
            entityManager.flush();
            entityManager.clear();
        }

        @Test
        @DisplayName("Should find underwriters by specialization")
        void shouldFindUnderwritersBySpecialization() {
            // When
            List<Underwriter> personalLoanUW = repository.findBySpecialization(UnderwriterSpecialization.PERSONAL_LOANS);
            List<Underwriter> mortgageUW = repository.findBySpecialization(UnderwriterSpecialization.MORTGAGE_LOANS);
            List<Underwriter> businessUW = repository.findBySpecialization(UnderwriterSpecialization.BUSINESS_LOANS);

            // Then
            assertThat(personalLoanUW).hasSize(2);
            assertThat(mortgageUW).hasSize(1);
            assertThat(businessUW).hasSize(1);
        }

        @Test
        @DisplayName("Should find underwriters by status")
        void shouldFindUnderwritersByStatus() {
            // When
            List<Underwriter> activeUW = repository.findByStatus(EmployeeStatus.ACTIVE);
            List<Underwriter> suspendedUW = repository.findByStatus(EmployeeStatus.SUSPENDED);

            // Then
            assertThat(activeUW).hasSize(3);
            assertThat(suspendedUW).hasSize(1);
            assertThat(suspendedUW.get(0).getUnderwriterId()).isEqualTo("UW103");
        }

        @Test
        @DisplayName("Should find underwriters by approval limit range")
        void shouldFindUnderwritersByApprovalLimitRange() {
            // When
            List<Underwriter> midRangeUW = repository.findByApprovalLimitBetween(
                new BigDecimal("60000"), new BigDecimal("300000")
            );
            List<Underwriter> highLimitUW = repository.findByApprovalLimitBetween(
                new BigDecimal("400000"), new BigDecimal("600000")
            );

            // Then
            assertThat(midRangeUW).hasSize(2); // UW100 (75000) and UW102 (250000)
            assertThat(highLimitUW).hasSize(1); // UW101 (500000)
        }

        @Test
        @DisplayName("Should find underwriters by experience level")
        void shouldFindUnderwritersByExperienceLevel() {
            // When
            List<Underwriter> experiencedUW = repository.findByExperienceYearsGreaterThanEqual(5);

            // Then
            assertThat(experiencedUW).hasSize(4); // All test underwriters have 5+ years experience
            assertThat(experiencedUW).allMatch(uw -> uw.getYearsOfExperience() >= 5);
        }

        @Test
        @DisplayName("Should find underwriters hired within date range")
        void shouldFindUnderwritersHiredWithinDateRange() {
            // Given
            LocalDate startDate = LocalDate.now().minusMonths(1);
            LocalDate endDate = LocalDate.now().plusMonths(1);

            // When
            List<Underwriter> recentHires = repository.findByHireDateBetween(startDate, endDate);

            // Then
            assertThat(recentHires).hasSize(4); // All test underwriters hired today
        }
    }

    @Nested
    @DisplayName("Business Logic Query Operations")
    class BusinessLogicQueryOperations {

        @BeforeEach
        void setupBusinessTestData() {
            // Create underwriters with specific business characteristics
            Underwriter seniorUW = Underwriter.create(
                "UW200", "Senior McSenior", "senior.mcsenior@bank.com", 
                UnderwriterSpecialization.MORTGAGE_LOANS, new BigDecimal("750000"), 
                15, LocalDate.now().minusYears(10)
            );
            repository.save(seniorUW);

            Underwriter juniorUW = Underwriter.create(
                "UW201", "Junior McJunior", "junior.mcjunior@bank.com", 
                UnderwriterSpecialization.PERSONAL_LOANS, new BigDecimal("50000"), 
                2, LocalDate.now().minusYears(1)
            );
            repository.save(juniorUW);

            Underwriter midLevelUW = Underwriter.create(
                "UW202", "Mid Level", "mid.level@bank.com", 
                UnderwriterSpecialization.BUSINESS_LOANS, new BigDecimal("200000"), 
                7, LocalDate.now().minusYears(5)
            );
            repository.save(midLevelUW);

            entityManager.flush();
            entityManager.clear();
        }

        @Test
        @DisplayName("Should find available underwriters")
        void shouldFindAvailableUnderwriters() {
            // When
            List<Underwriter> availableUW = repository.findAvailableUnderwriters();

            // Then
            assertThat(availableUW).hasSize(3);
            assertThat(availableUW).allMatch(uw -> uw.getStatus() == EmployeeStatus.ACTIVE);
        }

        @Test
        @DisplayName("Should find senior underwriters")
        void shouldFindSeniorUnderwriters() {
            // When
            List<Underwriter> seniorUW = repository.findSeniorUnderwriters();

            // Then
            assertThat(seniorUW).hasSize(2); // Senior (15 years) and Mid-level (7 years) have 5+ years
            assertThat(seniorUW).allMatch(uw -> uw.getYearsOfExperience() >= 5);
        }

        @Test
        @DisplayName("Should find underwriters by specialization and minimum approval limit")
        void shouldFindUnderwritersBySpecializationAndMinimumApprovalLimit() {
            // When
            List<Underwriter> mortgageHighLimit = repository.findBySpecializationAndApprovalLimitGreaterThanEqual(
                UnderwriterSpecialization.MORTGAGE_LOANS, new BigDecimal("500000")
            );
            List<Underwriter> businessMidLimit = repository.findBySpecializationAndApprovalLimitGreaterThanEqual(
                UnderwriterSpecialization.BUSINESS_LOANS, new BigDecimal("150000")
            );

            // Then
            assertThat(mortgageHighLimit).hasSize(1);
            assertThat(mortgageHighLimit.get(0).getUnderwriterId()).isEqualTo("UW200");
            assertThat(businessMidLimit).hasSize(1);
            assertThat(businessMidLimit.get(0).getUnderwriterId()).isEqualTo("UW202");
        }

        @Test
        @DisplayName("Should find qualified underwriters for loan amount")
        void shouldFindQualifiedUnderwritersForLoanAmount() {
            // When
            List<Underwriter> qualifiedFor100K = repository.findQualifiedUnderwritersForAmount(new BigDecimal("100000"));
            List<Underwriter> qualifiedFor300K = repository.findQualifiedUnderwritersForAmount(new BigDecimal("300000"));
            List<Underwriter> qualifiedFor800K = repository.findQualifiedUnderwritersForAmount(new BigDecimal("800000"));

            // Then
            assertThat(qualifiedFor100K).hasSize(2); // Senior (750k) and Mid-level (200k)
            assertThat(qualifiedFor300K).hasSize(1); // Senior (750k)
            assertThat(qualifiedFor800K).hasSize(0); // None have approval limit >= 800k
        }
    }

    @Nested
    @DisplayName("Count Operations")
    class CountOperations {

        @BeforeEach
        void setupCountTestData() {
            // Create underwriters with different characteristics for counting
            Underwriter uw1 = createTestUnderwriter("UW300", "Count One");
            uw1.updateSpecialization(UnderwriterSpecialization.PERSONAL_LOANS);
            repository.save(uw1);

            Underwriter uw2 = createTestUnderwriter("UW301", "Count Two");
            uw2.updateSpecialization(UnderwriterSpecialization.PERSONAL_LOANS);
            repository.save(uw2);

            Underwriter uw3 = createTestUnderwriter("UW302", "Count Three");
            uw3.updateSpecialization(UnderwriterSpecialization.MORTGAGE_LOANS);
            repository.save(uw3);

            Underwriter uw4 = createTestUnderwriter("UW303", "Count Four");
            uw4.updateSpecialization(UnderwriterSpecialization.BUSINESS_LOANS);
            uw4.suspend("Temporary suspension");
            repository.save(uw4);

            entityManager.flush();
            entityManager.clear();
        }

        @Test
        @DisplayName("Should count underwriters by specialization")
        void shouldCountUnderwritersBySpecialization() {
            // When
            long personalCount = repository.countBySpecialization(UnderwriterSpecialization.PERSONAL_LOANS);
            long mortgageCount = repository.countBySpecialization(UnderwriterSpecialization.MORTGAGE_LOANS);
            long businessCount = repository.countBySpecialization(UnderwriterSpecialization.BUSINESS_LOANS);

            // Then
            assertThat(personalCount).isEqualTo(2L);
            assertThat(mortgageCount).isEqualTo(1L);
            assertThat(businessCount).isEqualTo(1L);
        }

        @Test
        @DisplayName("Should count underwriters by status")
        void shouldCountUnderwritersByStatus() {
            // When
            long activeCount = repository.countByStatus(EmployeeStatus.ACTIVE);
            long suspendedCount = repository.countByStatus(EmployeeStatus.SUSPENDED);
            long terminatedCount = repository.countByStatus(EmployeeStatus.TERMINATED);

            // Then
            assertThat(activeCount).isEqualTo(3L);
            assertThat(suspendedCount).isEqualTo(1L);
            assertThat(terminatedCount).isEqualTo(0L);
        }

        @Test
        @DisplayName("Should count active underwriters by specialization")
        void shouldCountActiveUnderwritersBySpecialization() {
            // When
            long activePersonal = repository.countActiveBySpecialization(UnderwriterSpecialization.PERSONAL_LOANS);
            long activeMortgage = repository.countActiveBySpecialization(UnderwriterSpecialization.MORTGAGE_LOANS);
            long activeBusiness = repository.countActiveBySpecialization(UnderwriterSpecialization.BUSINESS_LOANS);

            // Then
            assertThat(activePersonal).isEqualTo(2L);
            assertThat(activeMortgage).isEqualTo(1L);
            assertThat(activeBusiness).isEqualTo(0L); // UW303 is suspended
        }
    }

    @Nested
    @DisplayName("Data Integrity and Mapping")
    class DataIntegrityAndMapping {

        @Test
        @DisplayName("Should preserve all domain data through persistence round-trip")
        void shouldPreserveAllDomainDataThroughPersistenceRoundTrip() {
            // Given
            Underwriter originalUW = Underwriter.create(
                "UW400", "Test Underwriter", "test.underwriter@bank.com",
                UnderwriterSpecialization.MORTGAGE_LOANS, new BigDecimal("350000.75"),
                8, LocalDate.now().minusYears(3)
            );

            // When
            Underwriter savedUW = repository.save(originalUW);
            Optional<Underwriter> retrievedUW = repository.findById("UW400");

            // Then
            assertThat(retrievedUW).isPresent();
            Underwriter retrieved = retrievedUW.get();

            // Verify all fields are preserved
            assertThat(retrieved.getUnderwriterId()).isEqualTo(originalUW.getUnderwriterId());
            assertThat(retrieved.getFullName()).isEqualTo(originalUW.getFullName());
            assertThat(retrieved.getEmail()).isEqualTo(originalUW.getEmail());
            assertThat(retrieved.getSpecialization()).isEqualTo(originalUW.getSpecialization());
            assertThat(retrieved.getApprovalLimit()).isEqualTo(originalUW.getApprovalLimit());
            assertThat(retrieved.getYearsOfExperience()).isEqualTo(originalUW.getYearsOfExperience());
            assertThat(retrieved.getHireDate()).isEqualTo(originalUW.getHireDate());
            assertThat(retrieved.getStatus()).isEqualTo(originalUW.getStatus());
        }

        @Test
        @DisplayName("Should handle business state transitions correctly")
        void shouldHandleBusinessStateTransitionsCorrectly() {
            // Given
            Underwriter uw = createTestUnderwriter("UW401", "State Test");
            repository.save(uw);

            // When - Apply business operations
            uw.updateApprovalLimit(new BigDecimal("200000"));
            uw.updateSpecialization(UnderwriterSpecialization.BUSINESS_LOANS);
            uw.suspend("Annual review");

            Underwriter savedUW = repository.save(uw);
            Optional<Underwriter> retrievedUW = repository.findById("UW401");

            // Then
            assertThat(retrievedUW).isPresent();
            Underwriter retrieved = retrievedUW.get();

            assertThat(retrieved.getApprovalLimit()).isEqualTo(new BigDecimal("200000"));
            assertThat(retrieved.getSpecialization()).isEqualTo(UnderwriterSpecialization.BUSINESS_LOANS);
            assertThat(retrieved.getStatus()).isEqualTo(EmployeeStatus.SUSPENDED);
        }

        @Test
        @DisplayName("Should handle domain business rules correctly")
        void shouldHandleDomainBusinessRulesCorrectly() {
            // Given
            Underwriter juniorUW = Underwriter.create(
                "UW402", "Junior Test", "junior@bank.com",
                UnderwriterSpecialization.PERSONAL_LOANS, new BigDecimal("25000"),
                1, LocalDate.now().minusYears(1)
            );
            repository.save(juniorUW);

            Optional<Underwriter> retrievedUW = repository.findById("UW402");
            assertThat(retrievedUW).isPresent();

            Underwriter retrieved = retrievedUW.get();

            // Then - Verify domain business rules work correctly
            assertThat(retrieved.isJuniorUnderwriter()).isTrue();
            assertThat(retrieved.isSeniorUnderwriter()).isFalse();
            assertThat(retrieved.canApprove(new BigDecimal("20000"))).isTrue();
            assertThat(retrieved.canApprove(new BigDecimal("30000"))).isFalse();
            assertThat(retrieved.isSpecializedFor(UnderwriterSpecialization.PERSONAL_LOANS)).isTrue();
            assertThat(retrieved.isSpecializedFor(UnderwriterSpecialization.MORTGAGE_LOANS)).isFalse();
        }
    }

    // Helper method to create test underwriters
    private Underwriter createTestUnderwriter(String underwriterId, String fullName) {
        return Underwriter.create(
            underwriterId,
            fullName,
            fullName.toLowerCase().replace(" ", ".") + "@bank.com",
            UnderwriterSpecialization.PERSONAL_LOANS,
            new BigDecimal("100000"),
            5,
            LocalDate.now().minusYears(2)
        );
    }
}