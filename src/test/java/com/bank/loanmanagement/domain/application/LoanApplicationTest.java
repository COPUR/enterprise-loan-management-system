package com.bank.loanmanagement.domain.application;

import com.bank.loanmanagement.domain.application.events.LoanApplicationApprovedEvent;
import com.bank.loanmanagement.domain.application.events.LoanApplicationSubmittedEvent;
import com.bank.loanmanagement.domain.application.events.UnderwriterAssignedEvent;
import com.bank.loanmanagement.domain.shared.DomainEvent;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Nested;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.*;

/**
 * TDD Unit Tests for LoanApplication Domain Model
 * 
 * Tests the pure domain logic without any infrastructure dependencies.
 * Validates business rules, state transitions, and domain event publishing.
 * 
 * Architecture Compliance Testing:
 * ✅ Clean Code: Tests clearly express business requirements
 * ✅ DDD: Tests validate domain invariants and business rules
 * ✅ Event-Driven: Tests verify domain event publishing
 * ✅ Type Safety: Tests validate input validation and error handling
 */
@DisplayName("LoanApplication Domain Model Tests")
class LoanApplicationTest {

    @Nested
    @DisplayName("Factory Method Tests")
    class FactoryMethodTests {

        @Test
        @DisplayName("Should create valid loan application with required fields")
        void shouldCreateValidLoanApplication() {
            // Given
            String applicationId = "APP1234567";
            Long customerId = 123L;
            LoanType loanType = LoanType.PERSONAL;
            BigDecimal requestedAmount = new BigDecimal("50000.00");
            Integer requestedTermMonths = 36;
            String purpose = "Home improvement";
            String submittedBy = "customer-portal";

            // When
            LoanApplication application = LoanApplication.create(
                applicationId, customerId, loanType, requestedAmount, 
                requestedTermMonths, purpose, submittedBy
            );

            // Then
            assertThat(application).isNotNull();
            assertThat(application.getApplicationId()).isEqualTo(applicationId);
            assertThat(application.getCustomerId()).isEqualTo(customerId);
            assertThat(application.getLoanType()).isEqualTo(loanType);
            assertThat(application.getRequestedAmount()).isEqualTo(requestedAmount);
            assertThat(application.getRequestedTermMonths()).isEqualTo(requestedTermMonths);
            assertThat(application.getPurpose()).isEqualTo(purpose);
            assertThat(application.getApplicationDate()).isEqualTo(LocalDate.now());
            assertThat(application.getStatus()).isEqualTo(ApplicationStatus.PENDING);
            assertThat(application.getPriority()).isEqualTo(ApplicationPriority.STANDARD);
        }

        @Test
        @DisplayName("Should publish LoanApplicationSubmittedEvent when created")
        void shouldPublishSubmittedEventWhenCreated() {
            // When
            LoanApplication application = LoanApplication.create(
                "APP1234567", 123L, LoanType.PERSONAL, 
                new BigDecimal("50000"), 36, "Home improvement", "customer-portal"
            );

            // Then
            List<DomainEvent> events = application.getUncommittedEvents();
            assertThat(events).hasSize(1);
            assertThat(events.get(0)).isInstanceOf(LoanApplicationSubmittedEvent.class);
            
            LoanApplicationSubmittedEvent event = (LoanApplicationSubmittedEvent) events.get(0);
            assertThat(event.getApplicationId()).isEqualTo("APP1234567");
            assertThat(event.getCustomerId()).isEqualTo("123");
            assertThat(event.getLoanType()).isEqualTo(LoanType.PERSONAL);
        }

        @Test
        @DisplayName("Should validate application ID format")
        void shouldValidateApplicationIdFormat() {
            // Given
            String invalidApplicationId = "INVALID";

            // When & Then
            assertThatThrownBy(() -> 
                LoanApplication.create(invalidApplicationId, 123L, LoanType.PERSONAL, 
                                     new BigDecimal("50000"), 36, "Purpose", "submitter")
            )
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Application ID must follow pattern APP#######");
        }

        @Test
        @DisplayName("Should validate customer ID is positive")
        void shouldValidateCustomerIdIsPositive() {
            // Given
            Long invalidCustomerId = -1L;

            // When & Then
            assertThatThrownBy(() -> 
                LoanApplication.create("APP1234567", invalidCustomerId, LoanType.PERSONAL, 
                                     new BigDecimal("50000"), 36, "Purpose", "submitter")
            )
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Customer ID must be positive");
        }

        @Test
        @DisplayName("Should validate requested amount range")
        void shouldValidateRequestedAmountRange() {
            // Test minimum amount
            assertThatThrownBy(() -> 
                LoanApplication.create("APP1234567", 123L, LoanType.PERSONAL, 
                                     new BigDecimal("500"), 36, "Purpose", "submitter")
            )
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Requested amount must be at least $1,000");

            // Test maximum amount
            assertThatThrownBy(() -> 
                LoanApplication.create("APP1234567", 123L, LoanType.PERSONAL, 
                                     new BigDecimal("15000000"), 36, "Purpose", "submitter")
            )
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Requested amount cannot exceed $10,000,000");
        }

        @Test
        @DisplayName("Should validate requested term range")
        void shouldValidateRequestedTermRange() {
            // Test minimum term
            assertThatThrownBy(() -> 
                LoanApplication.create("APP1234567", 123L, LoanType.PERSONAL, 
                                     new BigDecimal("50000"), 3, "Purpose", "submitter")
            )
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Loan term must be at least 6 months");

            // Test maximum term
            assertThatThrownBy(() -> 
                LoanApplication.create("APP1234567", 123L, LoanType.PERSONAL, 
                                     new BigDecimal("50000"), 500, "Purpose", "submitter")
            )
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Loan term cannot exceed 480 months");
        }
    }

    @Nested
    @DisplayName("Business Logic Tests")
    class BusinessLogicTests {

        private LoanApplication createSampleApplication() {
            return LoanApplication.create(
                "APP1234567", 123L, LoanType.PERSONAL, 
                new BigDecimal("50000"), 36, "Home improvement", "customer-portal"
            );
        }

        @Test
        @DisplayName("Should assign underwriter successfully")
        void shouldAssignUnderwriterSuccessfully() {
            // Given
            LoanApplication application = createSampleApplication();
            String underwriterId = "UW001";
            String assignedBy = "manager";
            String reason = "Auto-assignment based on specialization";

            // When
            application.assignUnderwriter(underwriterId, assignedBy, reason);

            // Then
            assertThat(application.getAssignedUnderwriter()).isEqualTo(underwriterId);
            assertThat(application.getStatus()).isEqualTo(ApplicationStatus.UNDER_REVIEW);
            
            // Verify domain event was published
            List<DomainEvent> events = application.getUncommittedEvents();
            assertThat(events).hasSize(2); // Submitted + Assigned
            assertThat(events.get(1)).isInstanceOf(UnderwriterAssignedEvent.class);
        }

        @Test
        @DisplayName("Should not assign underwriter to non-pending application")
        void shouldNotAssignUnderwriterToNonPendingApplication() {
            // Given
            LoanApplication application = createSampleApplication();
            application.assignUnderwriter("UW001", "manager", "Initial assignment");
            
            // When & Then
            assertThatThrownBy(() -> 
                application.assignUnderwriter("UW002", "manager", "Reassignment")
            )
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("Can only assign underwriter to pending applications");
        }

        @Test
        @DisplayName("Should approve application successfully")
        void shouldApproveApplicationSuccessfully() {
            // Given
            LoanApplication application = createSampleApplication();
            application.assignUnderwriter("UW001", "manager", "Assignment");
            
            BigDecimal approvedAmount = new BigDecimal("45000");
            BigDecimal approvedRate = new BigDecimal("5.50");
            String reason = "Good credit history and stable income";
            String approverId = "UW001";

            // When
            application.approve(approvedAmount, approvedRate, reason, approverId);

            // Then
            assertThat(application.getStatus()).isEqualTo(ApplicationStatus.APPROVED);
            assertThat(application.getApprovedAmount()).isEqualTo(approvedAmount);
            assertThat(application.getApprovedRate()).isEqualTo(approvedRate);
            assertThat(application.getDecisionDate()).isEqualTo(LocalDate.now());
            assertThat(application.getDecisionReason()).isEqualTo(reason);
            
            // Verify domain event was published
            List<DomainEvent> events = application.getUncommittedEvents();
            assertThat(events).hasSize(3); // Submitted + Assigned + Approved
            assertThat(events.get(2)).isInstanceOf(LoanApplicationApprovedEvent.class);
        }

        @Test
        @DisplayName("Should not approve application not under review")
        void shouldNotApproveApplicationNotUnderReview() {
            // Given
            LoanApplication application = createSampleApplication();
            
            // When & Then
            assertThatThrownBy(() -> 
                application.approve(new BigDecimal("45000"), new BigDecimal("5.50"), 
                                  "Reason", "UW001")
            )
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("Can only approve applications under review");
        }

        @Test
        @DisplayName("Should not approve amount exceeding requested amount")
        void shouldNotApproveAmountExceedingRequestedAmount() {
            // Given
            LoanApplication application = createSampleApplication();
            application.assignUnderwriter("UW001", "manager", "Assignment");
            
            BigDecimal excessiveAmount = new BigDecimal("60000"); // More than requested 50000

            // When & Then
            assertThatThrownBy(() -> 
                application.approve(excessiveAmount, new BigDecimal("5.50"), 
                                  "Reason", "UW001")
            )
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Approved amount cannot exceed requested amount");
        }

        @Test
        @DisplayName("Should reject application successfully")
        void shouldRejectApplicationSuccessfully() {
            // Given
            LoanApplication application = createSampleApplication();
            application.assignUnderwriter("UW001", "manager", "Assignment");
            String rejectionReason = "Insufficient income";

            // When
            application.reject(rejectionReason);

            // Then
            assertThat(application.getStatus()).isEqualTo(ApplicationStatus.REJECTED);
            assertThat(application.getDecisionDate()).isEqualTo(LocalDate.now());
            assertThat(application.getDecisionReason()).isEqualTo(rejectionReason);
        }

        @Test
        @DisplayName("Should request documents successfully")
        void shouldRequestDocumentsSuccessfully() {
            // Given
            LoanApplication application = createSampleApplication();
            application.assignUnderwriter("UW001", "manager", "Assignment");
            String documentReason = "Need proof of income";

            // When
            application.requestDocuments(documentReason);

            // Then
            assertThat(application.getStatus()).isEqualTo(ApplicationStatus.PENDING_DOCUMENTS);
            assertThat(application.getDecisionReason()).isEqualTo(documentReason);
        }

        @Test
        @DisplayName("Should resume review after documents received")
        void shouldResumeReviewAfterDocumentsReceived() {
            // Given
            LoanApplication application = createSampleApplication();
            application.assignUnderwriter("UW001", "manager", "Assignment");
            application.requestDocuments("Need documents");

            // When
            application.resumeReview();

            // Then
            assertThat(application.getStatus()).isEqualTo(ApplicationStatus.UNDER_REVIEW);
        }

        @Test
        @DisplayName("Should escalate priority correctly")
        void shouldEscalatePriorityCorrectly() {
            // Given
            LoanApplication application = createSampleApplication();
            assertThat(application.getPriority()).isEqualTo(ApplicationPriority.STANDARD);

            // When & Then
            application.escalatePriority();
            assertThat(application.getPriority()).isEqualTo(ApplicationPriority.HIGH);

            application.escalatePriority();
            assertThat(application.getPriority()).isEqualTo(ApplicationPriority.URGENT);

            application.escalatePriority();
            assertThat(application.getPriority()).isEqualTo(ApplicationPriority.URGENT); // Should stay at URGENT
        }
    }

    @Nested
    @DisplayName("Business Calculation Tests")
    class BusinessCalculationTests {

        private LoanApplication createApplicationWithFinancialData() {
            LoanApplication application = LoanApplication.reconstruct(
                "APP1234567", 123L, LoanType.MORTGAGE, new BigDecimal("300000"), 360,
                "Home purchase", LocalDate.now(), ApplicationStatus.PENDING, ApplicationPriority.STANDARD,
                new BigDecimal("8000"), 5, new BigDecimal("350000"), // monthlyIncome, employmentYears, collateralValue
                null, new BigDecimal("350000"), new BigDecimal("70000"), // businessRevenue, propertyValue, downPayment
                null, null, null, null, "UW001", // decisionDate, decisionReason, approvedAmount, approvedRate, assignedUnderwriter
                java.time.LocalDateTime.now(), java.time.LocalDateTime.now(), 0
            );
            return application;
        }

        @Test
        @DisplayName("Should calculate debt-to-income ratio correctly")
        void shouldCalculateDebtToIncomeRatioCorrectly() {
            // Given
            LoanApplication application = createApplicationWithFinancialData();
            BigDecimal monthlyDebt = new BigDecimal("2000");

            // When
            BigDecimal dtiRatio = application.calculateDebtToIncomeRatio(monthlyDebt);

            // Then
            BigDecimal expectedRatio = monthlyDebt.divide(application.getMonthlyIncome(), 4, BigDecimal.ROUND_HALF_UP);
            assertThat(dtiRatio).isEqualTo(expectedRatio);
            assertThat(dtiRatio).isEqualTo(new BigDecimal("0.2500")); // 2000/8000 = 0.25
        }

        @Test
        @DisplayName("Should calculate loan-to-value ratio correctly")
        void shouldCalculateLoanToValueRatioCorrectly() {
            // Given
            LoanApplication application = createApplicationWithFinancialData();

            // When
            BigDecimal ltvRatio = application.calculateLoanToValueRatio();

            // Then
            BigDecimal expectedRatio = application.getRequestedAmount()
                                                 .divide(application.getAssetValue(), 4, BigDecimal.ROUND_HALF_UP);
            assertThat(ltvRatio).isEqualTo(expectedRatio);
            assertThat(ltvRatio).isEqualTo(new BigDecimal("0.8571")); // 300000/350000 ≈ 0.8571
        }

        @Test
        @DisplayName("Should get asset value based on loan type")
        void shouldGetAssetValueBasedOnLoanType() {
            // Test MORTGAGE
            LoanApplication mortgageApp = createApplicationWithFinancialData();
            assertThat(mortgageApp.getAssetValue()).isEqualTo(mortgageApp.getPropertyValue());

            // Test BUSINESS
            LoanApplication businessApp = LoanApplication.reconstruct(
                "APP1234568", 124L, LoanType.BUSINESS, new BigDecimal("100000"), 60,
                "Equipment purchase", LocalDate.now(), ApplicationStatus.PENDING, ApplicationPriority.STANDARD,
                new BigDecimal("10000"), 3, new BigDecimal("120000"), // collateralValue
                new BigDecimal("500000"), null, null, // businessRevenue, propertyValue, downPayment
                null, null, null, null, null,
                java.time.LocalDateTime.now(), java.time.LocalDateTime.now(), 0
            );
            assertThat(businessApp.getAssetValue()).isEqualTo(businessApp.getCollateralValue());

            // Test PERSONAL (unsecured)
            LoanApplication personalApp = LoanApplication.create(
                "APP1234569", 125L, LoanType.PERSONAL, 
                new BigDecimal("25000"), 48, "Debt consolidation", "customer"
            );
            assertThat(personalApp.getAssetValue()).isNull();
        }

        @Test
        @DisplayName("Should check if additional documentation is required")
        void shouldCheckIfAdditionalDocumentationIsRequired() {
            // Test MORTGAGE requiring property value and down payment
            LoanApplication incompleteApp = LoanApplication.reconstruct(
                "APP1234567", 123L, LoanType.MORTGAGE, new BigDecimal("300000"), 360,
                "Home purchase", LocalDate.now(), ApplicationStatus.PENDING, ApplicationPriority.STANDARD,
                new BigDecimal("8000"), 5, null, // Missing collateralValue
                null, null, null, // Missing propertyValue and downPayment
                null, null, null, null, null,
                java.time.LocalDateTime.now(), java.time.LocalDateTime.now(), 0
            );
            assertThat(incompleteApp.requiresAdditionalDocumentation()).isTrue();

            // Test complete application
            LoanApplication completeApp = createApplicationWithFinancialData();
            assertThat(completeApp.requiresAdditionalDocumentation()).isFalse();
        }

        @Test
        @DisplayName("Should check if application is overdue for review")
        void shouldCheckIfApplicationIsOverdueForReview() {
            // Given - Application from 10 days ago
            LocalDate pastDate = LocalDate.now().minusDays(10);
            LoanApplication overdueApp = LoanApplication.reconstruct(
                "APP1234567", 123L, LoanType.PERSONAL, new BigDecimal("50000"), 36,
                "Purpose", pastDate, ApplicationStatus.UNDER_REVIEW, ApplicationPriority.STANDARD,
                null, null, null, null, null, null, null, null, null, null, "UW001",
                java.time.LocalDateTime.now(), java.time.LocalDateTime.now(), 0
            );

            // When & Then
            assertThat(overdueApp.isOverdueForReview()).isTrue();

            // Test recent application
            LoanApplication recentApp = createApplicationWithFinancialData();
            assertThat(recentApp.isOverdueForReview()).isFalse(); // PENDING status
        }
    }

    @Nested
    @DisplayName("Domain Event Tests")
    class DomainEventTests {

        @Test
        @DisplayName("Should track uncommitted events correctly")
        void shouldTrackUncommittedEventsCorrectly() {
            // Given
            LoanApplication application = LoanApplication.create(
                "APP1234567", 123L, LoanType.PERSONAL, 
                new BigDecimal("50000"), 36, "Purpose", "submitter"
            );

            // When - Perform multiple operations
            application.assignUnderwriter("UW001", "manager", "Assignment");
            application.approve(new BigDecimal("45000"), new BigDecimal("5.5"), 
                              "Good credit", "UW001");

            // Then
            List<DomainEvent> events = application.getUncommittedEvents();
            assertThat(events).hasSize(3);
            assertThat(events.get(0)).isInstanceOf(LoanApplicationSubmittedEvent.class);
            assertThat(events.get(1)).isInstanceOf(UnderwriterAssignedEvent.class);
            assertThat(events.get(2)).isInstanceOf(LoanApplicationApprovedEvent.class);
        }

        @Test
        @DisplayName("Should clear events when marked as committed")
        void shouldClearEventsWhenMarkedAsCommitted() {
            // Given
            LoanApplication application = LoanApplication.create(
                "APP1234567", 123L, LoanType.PERSONAL, 
                new BigDecimal("50000"), 36, "Purpose", "submitter"
            );
            assertThat(application.getUncommittedEvents()).hasSize(1);

            // When
            application.markEventsAsCommitted();

            // Then
            assertThat(application.getUncommittedEvents()).isEmpty();
        }

        @Test
        @DisplayName("Should provide event count for monitoring")
        void shouldProvideEventCountForMonitoring() {
            // Given
            LoanApplication application = LoanApplication.create(
                "APP1234567", 123L, LoanType.PERSONAL, 
                new BigDecimal("50000"), 36, "Purpose", "submitter"
            );
            application.assignUnderwriter("UW001", "manager", "Assignment");

            // When & Then
            assertThat(application.getUncommittedEventCount()).isEqualTo(2);
        }
    }

    @Nested
    @DisplayName("Reconstruction Tests")
    class ReconstructionTests {

        @Test
        @DisplayName("Should reconstruct application from infrastructure data")
        void shouldReconstructApplicationFromInfrastructureData() {
            // Given - Infrastructure data
            String applicationId = "APP1234567";
            Long customerId = 123L;
            LoanType loanType = LoanType.BUSINESS;
            BigDecimal requestedAmount = new BigDecimal("250000");
            Integer requestedTermMonths = 60;
            String purpose = "Equipment purchase";
            LocalDate applicationDate = LocalDate.now().minusDays(5);
            ApplicationStatus status = ApplicationStatus.APPROVED;
            ApplicationPriority priority = ApplicationPriority.HIGH;
            String assignedUnderwriter = "UW002";
            BigDecimal approvedAmount = new BigDecimal("200000");
            BigDecimal approvedRate = new BigDecimal("4.25");
            java.time.LocalDateTime createdAt = java.time.LocalDateTime.now().minusDays(5);
            java.time.LocalDateTime updatedAt = java.time.LocalDateTime.now().minusDays(1);

            // When
            LoanApplication application = LoanApplication.reconstruct(
                applicationId, customerId, loanType, requestedAmount, requestedTermMonths,
                purpose, applicationDate, status, priority, null, null, null, null, null, null,
                LocalDate.now().minusDays(1), "Approved due to excellent credit", 
                approvedAmount, approvedRate, assignedUnderwriter, createdAt, updatedAt, 1
            );

            // Then
            assertThat(application.getApplicationId()).isEqualTo(applicationId);
            assertThat(application.getCustomerId()).isEqualTo(customerId);
            assertThat(application.getLoanType()).isEqualTo(loanType);
            assertThat(application.getRequestedAmount()).isEqualTo(requestedAmount);
            assertThat(application.getStatus()).isEqualTo(status);
            assertThat(application.getAssignedUnderwriter()).isEqualTo(assignedUnderwriter);
            assertThat(application.getApprovedAmount()).isEqualTo(approvedAmount);
            assertThat(application.getApprovedRate()).isEqualTo(approvedRate);
            
            // Reconstructed applications should have no uncommitted events
            assertThat(application.getUncommittedEvents()).isEmpty();
        }
    }

    @Nested
    @DisplayName("Equality and Identity Tests")
    class EqualityAndIdentityTests {

        @Test
        @DisplayName("Should implement equality based on application ID")
        void shouldImplementEqualityBasedOnApplicationId() {
            // Given
            LoanApplication app1 = LoanApplication.create(
                "APP1234567", 123L, LoanType.PERSONAL, 
                new BigDecimal("50000"), 36, "Purpose", "submitter"
            );
            
            LoanApplication app2 = LoanApplication.create(
                "APP1234567", 456L, LoanType.BUSINESS, // Different customer and type
                new BigDecimal("100000"), 60, "Different purpose", "other-submitter"
            );
            
            LoanApplication app3 = LoanApplication.create(
                "APP9876543", 123L, LoanType.PERSONAL, 
                new BigDecimal("50000"), 36, "Purpose", "submitter"
            );

            // When & Then
            assertThat(app1).isEqualTo(app2); // Same application ID
            assertThat(app1).isNotEqualTo(app3); // Different application ID
            assertThat(app1.hashCode()).isEqualTo(app2.hashCode());
            assertThat(app1.hashCode()).isNotEqualTo(app3.hashCode());
        }

        @Test
        @DisplayName("Should implement getId method for aggregate root")
        void shouldImplementGetIdMethodForAggregateRoot() {
            // Given
            LoanApplication application = LoanApplication.create(
                "APP1234567", 123L, LoanType.PERSONAL, 
                new BigDecimal("50000"), 36, "Purpose", "submitter"
            );

            // When & Then
            assertThat(application.getId()).isEqualTo("APP1234567");
        }

        @Test
        @DisplayName("Should provide meaningful string representation")
        void shouldProvideMeaningfulStringRepresentation() {
            // Given
            LoanApplication application = LoanApplication.create(
                "APP1234567", 123L, LoanType.PERSONAL, 
                new BigDecimal("50000"), 36, "Purpose", "submitter"
            );

            // When
            String toString = application.toString();

            // Then
            assertThat(toString).contains("APP1234567");
            assertThat(toString).contains("123");
            assertThat(toString).contains("PERSONAL");
            assertThat(toString).contains("50000");
            assertThat(toString).contains("PENDING");
            assertThat(toString).contains("STANDARD");
            assertThat(toString).contains("uncommittedEvents=1");
        }
    }
}