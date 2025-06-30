package com.bank.loanmanagement.infrastructure.persistence.repositories;

import com.bank.loanmanagement.domain.application.*;
import com.bank.loanmanagement.infrastructure.persistence.jpa.LoanApplicationJpaEntity;
import com.bank.loanmanagement.infrastructure.persistence.mappers.LoanApplicationMapper;
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
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;

/**
 * Integration Tests for JPA Loan Application Repository
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
@DisplayName("JPA Loan Application Repository Integration Tests")
class JpaLoanApplicationRepositoryIntegrationTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private LoanApplicationJpaRepository jpaRepository;

    private JpaLoanApplicationRepository repository;
    private LoanApplicationMapper mapper;

    @BeforeEach
    void setUp() {
        mapper = new LoanApplicationMapper();
        repository = new JpaLoanApplicationRepository(jpaRepository, mapper);
    }

    @Nested
    @DisplayName("Basic CRUD Operations")
    class BasicCrudOperations {

        @Test
        @DisplayName("Should save and retrieve loan application successfully")
        void shouldSaveAndRetrieveLoanApplication() {
            // Given
            LoanApplication application = createTestLoanApplication("APP1234567", 123L);

            // When
            LoanApplication savedApplication = repository.save(application);
            Optional<LoanApplication> retrievedApplication = repository.findById("APP1234567");

            // Then
            assertThat(savedApplication).isNotNull();
            assertThat(savedApplication.getApplicationId()).isEqualTo("APP1234567");
            assertThat(retrievedApplication).isPresent();
            assertThat(retrievedApplication.get().getApplicationId()).isEqualTo("APP1234567");
            assertThat(retrievedApplication.get().getCustomerId()).isEqualTo(123L);
            assertThat(retrievedApplication.get().getLoanType()).isEqualTo(LoanType.PERSONAL);
            assertThat(retrievedApplication.get().getRequestedAmount()).isEqualTo(new BigDecimal("50000.00"));
        }

        @Test
        @DisplayName("Should update existing loan application")
        void shouldUpdateExistingLoanApplication() {
            // Given
            LoanApplication application = createTestLoanApplication("APP1234568", 124L);
            repository.save(application);

            // Modify the application
            application.assignUnderwriter("UW001", "SYSTEM", "Auto-assignment");

            // When
            LoanApplication updatedApplication = repository.save(application);
            Optional<LoanApplication> retrievedApplication = repository.findById("APP1234568");

            // Then
            assertThat(updatedApplication.getAssignedUnderwriter()).isEqualTo("UW001");
            assertThat(updatedApplication.getStatus()).isEqualTo(ApplicationStatus.UNDER_REVIEW);
            assertThat(retrievedApplication).isPresent();
            assertThat(retrievedApplication.get().getAssignedUnderwriter()).isEqualTo("UW001");
            assertThat(retrievedApplication.get().getStatus()).isEqualTo(ApplicationStatus.UNDER_REVIEW);
        }

        @Test
        @DisplayName("Should delete loan application by ID")
        void shouldDeleteLoanApplicationById() {
            // Given
            LoanApplication application = createTestLoanApplication("APP1234569", 125L);
            repository.save(application);
            assertThat(repository.existsById("APP1234569")).isTrue();

            // When
            repository.deleteById("APP1234569");

            // Then
            assertThat(repository.existsById("APP1234569")).isFalse();
            assertThat(repository.findById("APP1234569")).isEmpty();
        }

        @Test
        @DisplayName("Should return empty optional for non-existent ID")
        void shouldReturnEmptyOptionalForNonExistentId() {
            // When
            Optional<LoanApplication> result = repository.findById("NONEXISTENT");

            // Then
            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("Query Operations")
    class QueryOperations {

        @BeforeEach
        void setupTestData() {
            // Create multiple test applications with different characteristics
            LoanApplication app1 = createTestLoanApplication("APP1000001", 100L);
            app1.assignUnderwriter("UW001", "SYSTEM", "Auto-assignment");
            repository.save(app1);

            LoanApplication app2 = createTestLoanApplication("APP1000002", 101L);
            repository.save(app2);

            LoanApplication app3 = LoanApplication.create(
                "APP1000003", 102L, LoanType.MORTGAGE, 
                new BigDecimal("300000"), 360, "Home purchase", "customer-portal"
            );
            repository.save(app3);

            LoanApplication app4 = LoanApplication.create(
                "APP1000004", 100L, LoanType.BUSINESS, 
                new BigDecimal("750000"), 120, "Business expansion", "business-portal"
            );
            repository.save(app4);

            // Force flush to ensure data is persisted
            entityManager.flush();
            entityManager.clear();
        }

        @Test
        @DisplayName("Should find applications by customer ID")
        void shouldFindApplicationsByCustomerId() {
            // When
            List<LoanApplication> applications = repository.findByCustomerId(100L);

            // Then
            assertThat(applications).hasSize(2);
            assertThat(applications).allMatch(app -> app.getCustomerId().equals(100L));
        }

        @Test
        @DisplayName("Should find applications by status")
        void shouldFindApplicationsByStatus() {
            // When
            List<LoanApplication> pendingApps = repository.findByStatus(ApplicationStatus.PENDING);
            List<LoanApplication> underReviewApps = repository.findByStatus(ApplicationStatus.UNDER_REVIEW);

            // Then
            assertThat(pendingApps).hasSize(3); // app2, app3, app4
            assertThat(underReviewApps).hasSize(1); // app1
        }

        @Test
        @DisplayName("Should find applications by loan type")
        void shouldFindApplicationsByLoanType() {
            // When
            List<LoanApplication> personalLoans = repository.findByLoanType(LoanType.PERSONAL);
            List<LoanApplication> mortgageLoans = repository.findByLoanType(LoanType.MORTGAGE);
            List<LoanApplication> businessLoans = repository.findByLoanType(LoanType.BUSINESS);

            // Then
            assertThat(personalLoans).hasSize(2);
            assertThat(mortgageLoans).hasSize(1);
            assertThat(businessLoans).hasSize(1);
        }

        @Test
        @DisplayName("Should find applications by assigned underwriter")
        void shouldFindApplicationsByAssignedUnderwriter() {
            // When
            List<LoanApplication> uw001Apps = repository.findByAssignedUnderwriter("UW001");
            List<LoanApplication> nullUnderwriterApps = repository.findByAssignedUnderwriter(null);

            // Then
            assertThat(uw001Apps).hasSize(1);
            assertThat(uw001Apps.get(0).getApplicationId()).isEqualTo("APP1000001");
            assertThat(nullUnderwriterApps).isEmpty(); // findByAssignedUnderwriter with null should return empty
        }

        @Test
        @DisplayName("Should find applications by priority")
        void shouldFindApplicationsByPriority() {
            // When
            List<LoanApplication> standardPriorityApps = repository.findByPriority(ApplicationPriority.STANDARD);

            // Then
            assertThat(standardPriorityApps).hasSize(4); // All test apps have standard priority by default
        }

        @Test
        @DisplayName("Should find applications by amount range")
        void shouldFindApplicationsByAmountRange() {
            // When
            List<LoanApplication> smallLoans = repository.findByRequestedAmountBetween(
                new BigDecimal("40000"), new BigDecimal("60000")
            );
            List<LoanApplication> largeLoans = repository.findByRequestedAmountBetween(
                new BigDecimal("200000"), new BigDecimal("800000")
            );

            // Then
            assertThat(smallLoans).hasSize(2); // APP1000001 and APP1000002 (50000 each)
            assertThat(largeLoans).hasSize(2); // APP1000003 (300000) and APP1000004 (750000)
        }

        @Test
        @DisplayName("Should find applications by date range")
        void shouldFindApplicationsByDateRange() {
            // Given
            LocalDate today = LocalDate.now();
            LocalDate yesterday = today.minusDays(1);
            LocalDate tomorrow = today.plusDays(1);

            // When
            List<LoanApplication> todaysApps = repository.findByApplicationDateBetween(today, today);
            List<LoanApplication> thisWeekApps = repository.findByApplicationDateBetween(yesterday, tomorrow);

            // Then
            assertThat(todaysApps).hasSize(4); // All apps created today
            assertThat(thisWeekApps).hasSize(4); // All apps within the range
        }
    }

    @Nested
    @DisplayName("Complex Query Operations")
    class ComplexQueryOperations {

        @BeforeEach
        void setupComplexTestData() {
            // Create applications with various statuses and characteristics
            LoanApplication highPriorityApp = LoanApplication.create(
                "APP2000001", 200L, LoanType.PERSONAL, 
                new BigDecimal("600000"), 60, "Emergency funding", "emergency-portal"
            );
            repository.save(highPriorityApp);

            LoanApplication riskAssessmentApp = LoanApplication.create(
                "APP2000002", 201L, LoanType.BUSINESS, 
                new BigDecimal("150000"), 84, "Business acquisition", "business-portal"
            );
            repository.save(riskAssessmentApp);

            // Force flush
            entityManager.flush();
            entityManager.clear();
        }

        @Test
        @DisplayName("Should find large loan applications")
        void shouldFindLargeLoanApplications() {
            // When
            List<LoanApplication> largeLoans = repository.findLargeLoanApplications();

            // Then
            assertThat(largeLoans).hasSize(1);
            assertThat(largeLoans.get(0).getApplicationId()).isEqualTo("APP2000001");
            assertThat(largeLoans.get(0).getRequestedAmount()).isGreaterThanOrEqualTo(new BigDecimal("500000"));
        }

        @Test
        @DisplayName("Should find applications created today")
        void shouldFindApplicationsCreatedToday() {
            // When
            List<LoanApplication> todaysApps = repository.findApplicationsCreatedToday();

            // Then
            assertThat(todaysApps).hasSize(2);
            assertThat(todaysApps).allMatch(app -> app.getApplicationDate().equals(LocalDate.now()));
        }

        @Test
        @DisplayName("Should find applications for risk assessment")
        void shouldFindApplicationsForRiskAssessment() {
            // When
            List<LoanApplication> riskAssessmentApps = repository.findApplicationsForRiskAssessment();

            // Then
            assertThat(riskAssessmentApps).hasSize(2); // Both apps have amounts >= 100,000
            assertThat(riskAssessmentApps).allMatch(app -> 
                app.getRequestedAmount().compareTo(new BigDecimal("100000")) >= 0
            );
            assertThat(riskAssessmentApps).allMatch(app -> 
                app.getStatus() == ApplicationStatus.PENDING
            );
        }

        @Test
        @DisplayName("Should find pending documents applications")
        void shouldFindPendingDocumentsApplications() {
            // Given - Modify an application to require documents
            Optional<LoanApplication> app = repository.findById("APP2000001");
            assertThat(app).isPresent();
            app.get().requestDocuments("Additional income verification required");
            repository.save(app.get());

            // When
            List<LoanApplication> pendingDocsApps = repository.findPendingDocuments();

            // Then
            assertThat(pendingDocsApps).hasSize(1);
            assertThat(pendingDocsApps.get(0).getStatus()).isEqualTo(ApplicationStatus.PENDING_DOCUMENTS);
            assertThat(pendingDocsApps.get(0).getApplicationId()).isEqualTo("APP2000001");
        }
    }

    @Nested
    @DisplayName("Count Operations")
    class CountOperations {

        @BeforeEach
        void setupCountTestData() {
            // Create multiple applications with different statuses and underwriters
            LoanApplication app1 = createTestLoanApplication("APP3000001", 300L);
            app1.assignUnderwriter("UW001", "SYSTEM", "Auto-assignment");
            repository.save(app1);

            LoanApplication app2 = createTestLoanApplication("APP3000002", 301L);
            app2.assignUnderwriter("UW001", "SYSTEM", "Auto-assignment");
            repository.save(app2);

            LoanApplication app3 = createTestLoanApplication("APP3000003", 302L);
            app3.assignUnderwriter("UW002", "SYSTEM", "Auto-assignment");
            repository.save(app3);

            LoanApplication app4 = createTestLoanApplication("APP3000004", 303L);
            repository.save(app4);

            entityManager.flush();
            entityManager.clear();
        }

        @Test
        @DisplayName("Should count applications by status")
        void shouldCountApplicationsByStatus() {
            // When
            long pendingCount = repository.countByStatus(ApplicationStatus.PENDING);
            long underReviewCount = repository.countByStatus(ApplicationStatus.UNDER_REVIEW);
            long approvedCount = repository.countByStatus(ApplicationStatus.APPROVED);

            // Then
            assertThat(pendingCount).isEqualTo(1L); // app4
            assertThat(underReviewCount).isEqualTo(3L); // app1, app2, app3
            assertThat(approvedCount).isEqualTo(0L);
        }

        @Test
        @DisplayName("Should count applications by assigned underwriter")
        void shouldCountApplicationsByAssignedUnderwriter() {
            // When
            long uw001Count = repository.countByAssignedUnderwriter("UW001");
            long uw002Count = repository.countByAssignedUnderwriter("UW002");
            long uw003Count = repository.countByAssignedUnderwriter("UW003");

            // Then
            assertThat(uw001Count).isEqualTo(2L);
            assertThat(uw002Count).isEqualTo(1L);
            assertThat(uw003Count).isEqualTo(0L);
        }
    }

    @Nested
    @DisplayName("Combined Query Operations")
    class CombinedQueryOperations {

        @BeforeEach
        void setupCombinedTestData() {
            LoanApplication app1 = createTestLoanApplication("APP4000001", 400L);
            app1.assignUnderwriter("UW001", "SYSTEM", "Auto-assignment");
            repository.save(app1);

            LoanApplication app2 = LoanApplication.create(
                "APP4000002", 400L, LoanType.MORTGAGE, 
                new BigDecimal("400000"), 360, "Home purchase", "web-portal"
            );
            app2.assignUnderwriter("UW001", "SYSTEM", "Auto-assignment");
            repository.save(app2);

            entityManager.flush();
            entityManager.clear();
        }

        @Test
        @DisplayName("Should find applications by status and assigned underwriter")
        void shouldFindApplicationsByStatusAndAssignedUnderwriter() {
            // When
            List<LoanApplication> uw001UnderReview = repository.findByStatusAndAssignedUnderwriter(
                ApplicationStatus.UNDER_REVIEW, "UW001"
            );

            // Then
            assertThat(uw001UnderReview).hasSize(2);
            assertThat(uw001UnderReview).allMatch(app -> 
                app.getStatus() == ApplicationStatus.UNDER_REVIEW && 
                "UW001".equals(app.getAssignedUnderwriter())
            );
        }

        @Test
        @DisplayName("Should find applications by customer ID and status")
        void shouldFindApplicationsByCustomerIdAndStatus() {
            // When
            List<LoanApplication> customer400UnderReview = repository.findByCustomerIdAndStatus(
                400L, ApplicationStatus.UNDER_REVIEW
            );

            // Then
            assertThat(customer400UnderReview).hasSize(2);
            assertThat(customer400UnderReview).allMatch(app -> 
                app.getCustomerId().equals(400L) && 
                app.getStatus() == ApplicationStatus.UNDER_REVIEW
            );
        }

        @Test
        @DisplayName("Should find applications by loan type and status")
        void shouldFindApplicationsByLoanTypeAndStatus() {
            // When
            List<LoanApplication> personalUnderReview = repository.findByLoanTypeAndStatus(
                LoanType.PERSONAL, ApplicationStatus.UNDER_REVIEW
            );
            List<LoanApplication> mortgageUnderReview = repository.findByLoanTypeAndStatus(
                LoanType.MORTGAGE, ApplicationStatus.UNDER_REVIEW
            );

            // Then
            assertThat(personalUnderReview).hasSize(1);
            assertThat(mortgageUnderReview).hasSize(1);
        }
    }

    @Nested
    @DisplayName("Data Integrity and Mapping")
    class DataIntegrityAndMapping {

        @Test
        @DisplayName("Should preserve all domain data through persistence round-trip")
        void shouldPreserveAllDomainDataThroughPersistenceRoundTrip() {
            // Given
            LoanApplication originalApp = LoanApplication.create(
                "APP5000001", 500L, LoanType.BUSINESS, 
                new BigDecimal("250000.50"), 72, "Equipment purchase", "business-portal"
            );
            originalApp.assignUnderwriter("UW005", "MANAGER", "Specialized assignment");

            // When
            LoanApplication savedApp = repository.save(originalApp);
            Optional<LoanApplication> retrievedApp = repository.findById("APP5000001");

            // Then
            assertThat(retrievedApp).isPresent();
            LoanApplication retrieved = retrievedApp.get();

            // Verify all fields are preserved
            assertThat(retrieved.getApplicationId()).isEqualTo(originalApp.getApplicationId());
            assertThat(retrieved.getCustomerId()).isEqualTo(originalApp.getCustomerId());
            assertThat(retrieved.getLoanType()).isEqualTo(originalApp.getLoanType());
            assertThat(retrieved.getRequestedAmount()).isEqualTo(originalApp.getRequestedAmount());
            assertThat(retrieved.getRequestedTermMonths()).isEqualTo(originalApp.getRequestedTermMonths());
            assertThat(retrieved.getPurpose()).isEqualTo(originalApp.getPurpose());
            assertThat(retrieved.getApplicationDate()).isEqualTo(originalApp.getApplicationDate());
            assertThat(retrieved.getStatus()).isEqualTo(originalApp.getStatus());
            assertThat(retrieved.getAssignedUnderwriter()).isEqualTo(originalApp.getAssignedUnderwriter());
            assertThat(retrieved.getPriority()).isEqualTo(originalApp.getPriority());
        }

        @Test
        @DisplayName("Should handle business state transitions correctly")
        void shouldHandleBusinessStateTransitionsCorrectly() {
            // Given
            LoanApplication app = createTestLoanApplication("APP5000002", 501L);
            repository.save(app);

            // When - Apply business operations
            app.assignUnderwriter("UW006", "SYSTEM", "Auto-assignment");
            app.approve(new BigDecimal("45000"), new BigDecimal("5.5"), "Good credit history", "UW006");
            
            LoanApplication savedApp = repository.save(app);
            Optional<LoanApplication> retrievedApp = repository.findById("APP5000002");

            // Then
            assertThat(retrievedApp).isPresent();
            LoanApplication retrieved = retrievedApp.get();
            
            assertThat(retrieved.getStatus()).isEqualTo(ApplicationStatus.APPROVED);
            assertThat(retrieved.getAssignedUnderwriter()).isEqualTo("UW006");
            assertThat(retrieved.getApprovedAmount()).isEqualTo(new BigDecimal("45000"));
            assertThat(retrieved.getApprovedRate()).isEqualTo(new BigDecimal("5.5"));
            assertThat(retrieved.getDecisionReason()).isEqualTo("Good credit history");
            assertThat(retrieved.getDecisionDate()).isNotNull();
        }
    }

    // Helper method to create test loan applications
    private LoanApplication createTestLoanApplication(String applicationId, Long customerId) {
        return LoanApplication.create(
            applicationId, 
            customerId, 
            LoanType.PERSONAL, 
            new BigDecimal("50000.00"), 
            36, 
            "Home improvement", 
            "web-portal"
        );
    }
}