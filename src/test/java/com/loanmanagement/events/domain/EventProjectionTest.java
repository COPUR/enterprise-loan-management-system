package com.loanmanagement.events.domain;

import com.loanmanagement.events.domain.model.*;
import com.loanmanagement.events.domain.projection.*;
import com.loanmanagement.loan.domain.model.LoanId;
import com.loanmanagement.shared.domain.Money;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Tests for Event Projections (Read Models)
 * Comprehensive testing of projection building, updating, and querying
 */
@DisplayName("Event Projection Tests")
class EventProjectionTest {

    private LoanSummaryProjection loanSummaryProjection;
    private CustomerLoanHistoryProjection customerHistoryProjection;
    private PaymentSummaryProjection paymentSummaryProjection;
    private LoanAnalyticsProjection analyticsProjection;

    @Mock
    private ProjectionRepository projectionRepository;
    
    @Mock
    private ProjectionEventStore projectionEventStore;
    
    @Mock
    private ProjectionQueryService queryService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        loanSummaryProjection = new LoanSummaryProjection(projectionRepository, projectionEventStore);
        customerHistoryProjection = new CustomerLoanHistoryProjection(projectionRepository, projectionEventStore);
        paymentSummaryProjection = new PaymentSummaryProjection(projectionRepository, projectionEventStore);
        analyticsProjection = new LoanAnalyticsProjection(projectionRepository, projectionEventStore);
    }

    @Nested
    @DisplayName("Projection Creation Tests")
    class ProjectionCreationTests {

        @Test
        @DisplayName("Should create loan summary projection from application event")
        void shouldCreateLoanSummaryProjectionFromApplicationEvent() {
            // Given
            LoanApplicationSubmittedEvent event = createLoanApplicationEvent();
            when(projectionRepository.findLoanSummary(any())).thenReturn(Optional.empty());

            // When
            ProjectionResult result = loanSummaryProjection.handle(event);

            // Then
            assertTrue(result.isSuccessful());
            assertEquals(ProjectionOperation.CREATE, result.getOperation());
            
            verify(projectionRepository, times(1)).saveLoanSummary(any(LoanSummaryView.class));
            
            // Verify the created projection
            LoanSummaryView expectedView = LoanSummaryView.builder()
                    .loanId(event.getLoanId().toString())
                    .customerId(event.getCustomerId())
                    .requestedAmount(event.getLoanAmount())
                    .status("APPLIED")
                    .applicationDate(event.getApplicationDate())
                    .lastUpdated(event.getOccurredOn())
                    .build();
            
            verify(projectionRepository).saveLoanSummary(argThat(view -> 
                view.getLoanId().equals(expectedView.getLoanId()) &&
                view.getCustomerId().equals(expectedView.getCustomerId()) &&
                view.getRequestedAmount().equals(expectedView.getRequestedAmount())
            ));
        }

        @Test
        @DisplayName("Should create customer history projection from application event")
        void shouldCreateCustomerHistoryProjectionFromApplicationEvent() {
            // Given
            LoanApplicationSubmittedEvent event = createLoanApplicationEvent();
            when(projectionRepository.findCustomerHistory(any())).thenReturn(Optional.empty());

            // When
            ProjectionResult result = customerHistoryProjection.handle(event);

            // Then
            assertTrue(result.isSuccessful());
            assertEquals(ProjectionOperation.CREATE, result.getOperation());
            
            verify(projectionRepository, times(1)).saveCustomerHistory(any(CustomerLoanHistoryView.class));
        }

        @Test
        @DisplayName("Should create payment summary projection from payment event")
        void shouldCreatePaymentSummaryProjectionFromPaymentEvent() {
            // Given
            PaymentProcessedEvent event = createPaymentProcessedEvent();
            when(projectionRepository.findPaymentSummary(any())).thenReturn(Optional.empty());

            // When
            ProjectionResult result = paymentSummaryProjection.handle(event);

            // Then
            assertTrue(result.isSuccessful());
            assertEquals(ProjectionOperation.CREATE, result.getOperation());
            
            verify(projectionRepository, times(1)).savePaymentSummary(any(PaymentSummaryView.class));
        }
    }

    @Nested
    @DisplayName("Projection Update Tests")
    class ProjectionUpdateTests {

        @Test
        @DisplayName("Should update loan summary projection from approval event")
        void shouldUpdateLoanSummaryProjectionFromApprovalEvent() {
            // Given
            LoanApprovedEvent event = createLoanApprovedEvent();
            LoanSummaryView existingView = LoanSummaryView.builder()
                    .loanId(event.getLoanId().toString())
                    .customerId("CUST-123")
                    .requestedAmount(Money.of("USD", new BigDecimal("50000.00")))
                    .status("APPLIED")
                    .applicationDate(LocalDateTime.now().minusHours(1))
                    .lastUpdated(LocalDateTime.now().minusHours(1))
                    .version(1L)
                    .build();
            
            when(projectionRepository.findLoanSummary(event.getLoanId().toString()))
                    .thenReturn(Optional.of(existingView));

            // When
            ProjectionResult result = loanSummaryProjection.handle(event);

            // Then
            assertTrue(result.isSuccessful());
            assertEquals(ProjectionOperation.UPDATE, result.getOperation());
            
            verify(projectionRepository, times(1)).saveLoanSummary(argThat(view -> 
                "APPROVED".equals(view.getStatus()) &&
                event.getApprovedAmount().equals(view.getApprovedAmount()) &&
                event.getInterestRate().equals(view.getInterestRate())
            ));
        }

        @Test
        @DisplayName("Should update customer history projection with new loan")
        void shouldUpdateCustomerHistoryProjectionWithNewLoan() {
            // Given
            LoanApprovedEvent event = createLoanApprovedEvent();
            CustomerLoanHistoryView existingHistory = CustomerLoanHistoryView.builder()
                    .customerId("CUST-123")
                    .totalLoans(2)
                    .activeLoans(1)
                    .totalAmountBorrowed(Money.of("USD", new BigDecimal("80000.00")))
                    .lastLoanDate(LocalDateTime.now().minusMonths(1))
                    .build();
            
            when(projectionRepository.findCustomerHistory("CUST-123"))
                    .thenReturn(Optional.of(existingHistory));

            // When
            ProjectionResult result = customerHistoryProjection.handle(event);

            // Then
            assertTrue(result.isSuccessful());
            verify(projectionRepository, times(1)).saveCustomerHistory(argThat(history -> 
                history.getTotalLoans() == 3 &&
                history.getActiveLoans() == 2
            ));
        }

        @Test
        @DisplayName("Should update payment summary projection with new payment")
        void shouldUpdatePaymentSummaryProjectionWithNewPayment() {
            // Given
            PaymentProcessedEvent event = createPaymentProcessedEvent();
            PaymentSummaryView existingSummary = PaymentSummaryView.builder()
                    .loanId(event.getLoanId().toString())
                    .totalPayments(5)
                    .totalAmountPaid(Money.of("USD", new BigDecimal("5000.00")))
                    .lastPaymentDate(LocalDateTime.now().minusMonths(1))
                    .remainingBalance(Money.of("USD", new BigDecimal("45000.00")))
                    .build();
            
            when(projectionRepository.findPaymentSummary(event.getLoanId().toString()))
                    .thenReturn(Optional.of(existingSummary));

            // When
            ProjectionResult result = paymentSummaryProjection.handle(event);

            // Then
            assertTrue(result.isSuccessful());
            verify(projectionRepository, times(1)).savePaymentSummary(argThat(summary -> 
                summary.getTotalPayments() == 6 &&
                summary.getTotalAmountPaid().getAmount().equals(new BigDecimal("6000.00"))
            ));
        }
    }

    @Nested
    @DisplayName("Projection Rebuild Tests")
    class ProjectionRebuildTests {

        @Test
        @DisplayName("Should rebuild loan summary projection from event history")
        void shouldRebuildLoanSummaryProjectionFromEventHistory() {
            // Given
            String loanId = LoanId.generate().toString();
            List<DomainEvent> eventHistory = createLoanEventHistory(loanId);
            when(projectionEventStore.getEventsByAggregateId(loanId)).thenReturn(eventHistory);

            // When
            ProjectionRebuildResult result = loanSummaryProjection.rebuild(loanId);

            // Then
            assertTrue(result.isSuccessful());
            assertEquals(3, result.getProcessedEvents());
            assertEquals(loanId, result.getAggregateId());
            
            verify(projectionRepository, times(1)).saveLoanSummary(argThat(view -> 
                "DISBURSED".equals(view.getStatus())
            ));
        }

        @Test
        @DisplayName("Should rebuild customer history projection from all customer events")
        void shouldRebuildCustomerHistoryProjectionFromAllCustomerEvents() {
            // Given
            String customerId = "CUST-456";
            List<DomainEvent> customerEvents = createCustomerEventHistory(customerId);
            when(projectionEventStore.getEventsByCustomerId(customerId)).thenReturn(customerEvents);

            // When
            ProjectionRebuildResult result = customerHistoryProjection.rebuildForCustomer(customerId);

            // Then
            assertTrue(result.isSuccessful());
            assertEquals(6, result.getProcessedEvents()); // 2 loans × 3 events each
            
            verify(projectionRepository, times(1)).saveCustomerHistory(argThat(history -> 
                history.getTotalLoans() == 2 &&
                history.getActiveLoans() == 2
            ));
        }

        @Test
        @DisplayName("Should handle projection rebuild failures gracefully")
        void shouldHandleProjectionRebuildFailuresGracefully() {
            // Given
            String loanId = "INVALID-LOAN-ID";
            when(projectionEventStore.getEventsByAggregateId(loanId))
                    .thenThrow(new RuntimeException("Event store error"));

            // When
            ProjectionRebuildResult result = loanSummaryProjection.rebuild(loanId);

            // Then
            assertFalse(result.isSuccessful());
            assertNotNull(result.getErrorMessage());
            assertTrue(result.getErrorMessage().contains("Event store error"));
            assertEquals(0, result.getProcessedEvents());
        }
    }

    @Nested
    @DisplayName("Projection Query Tests")
    class ProjectionQueryTests {

        @Test
        @DisplayName("Should query loan summaries by customer")
        void shouldQueryLoanSummariesByCustomer() {
            // Given
            String customerId = "CUST-123";
            List<LoanSummaryView> expectedLoans = List.of(
                createLoanSummaryView("LOAN-1", customerId, "ACTIVE"),
                createLoanSummaryView("LOAN-2", customerId, "PAID_OFF")
            );
            when(projectionRepository.findLoanSummariesByCustomerId(customerId))
                    .thenReturn(expectedLoans);

            // When
            ProjectionQueryResult<List<LoanSummaryView>> result = 
                    loanSummaryProjection.findByCustomerId(customerId);

            // Then
            assertTrue(result.isSuccessful());
            assertNotNull(result.getData());
            assertEquals(2, result.getData().size());
            assertEquals(customerId, result.getData().get(0).getCustomerId());
        }

        @Test
        @DisplayName("Should query payment summaries with filters")
        void shouldQueryPaymentSummariesWithFilters() {
            // Given
            PaymentSummaryFilter filter = PaymentSummaryFilter.builder()
                    .dateFrom(LocalDateTime.now().minusMonths(3))
                    .dateTo(LocalDateTime.now())
                    .minimumAmount(Money.of("USD", new BigDecimal("500.00")))
                    .build();
            
            List<PaymentSummaryView> expectedPayments = List.of(
                createPaymentSummaryView("LOAN-1", 5),
                createPaymentSummaryView("LOAN-2", 3)
            );
            when(projectionRepository.findPaymentSummariesByFilter(filter))
                    .thenReturn(expectedPayments);

            // When
            ProjectionQueryResult<List<PaymentSummaryView>> result = 
                    paymentSummaryProjection.findByFilter(filter);

            // Then
            assertTrue(result.isSuccessful());
            assertEquals(2, result.getData().size());
        }

        @Test
        @DisplayName("Should query analytics projections for reporting")
        void shouldQueryAnalyticsProjectionsForReporting() {
            // Given
            AnalyticsQuery query = AnalyticsQuery.builder()
                    .timeRange(TimeRange.LAST_30_DAYS)
                    .aggregationType(AggregationType.MONTHLY)
                    .metrics(List.of("total_loans", "total_amount", "approval_rate"))
                    .build();
            
            LoanAnalyticsView expectedAnalytics = LoanAnalyticsView.builder()
                    .totalLoans(150)
                    .totalAmountDisbursed(Money.of("USD", new BigDecimal("15000000.00")))
                    .approvalRate(0.85)
                    .averageLoanAmount(Money.of("USD", new BigDecimal("100000.00")))
                    .build();
            when(projectionRepository.findAnalyticsByQuery(query))
                    .thenReturn(expectedAnalytics);

            // When
            ProjectionQueryResult<LoanAnalyticsView> result = 
                    analyticsProjection.queryAnalytics(query);

            // Then
            assertTrue(result.isSuccessful());
            assertNotNull(result.getData());
            assertEquals(150, result.getData().getTotalLoans());
            assertEquals(0.85, result.getData().getApprovalRate(), 0.01);
        }
    }

    @Nested
    @DisplayName("Projection Consistency Tests")
    class ProjectionConsistencyTests {

        @Test
        @DisplayName("Should maintain consistency between projections")
        void shouldMaintainConsistencyBetweenProjections() {
            // Given
            LoanApplicationSubmittedEvent event = createLoanApplicationEvent();
            
            // When
            ProjectionResult loanSummaryResult = loanSummaryProjection.handle(event);
            ProjectionResult customerHistoryResult = customerHistoryProjection.handle(event);

            // Then
            assertTrue(loanSummaryResult.isSuccessful());
            assertTrue(customerHistoryResult.isSuccessful());
            
            // Verify consistency
            ProjectionConsistencyCheck check = new ProjectionConsistencyCheck();
            ConsistencyCheckResult consistencyResult = check.validateConsistency(
                event.getLoanId().toString(), event.getCustomerId());
            
            assertTrue(consistencyResult.isConsistent());
        }

        @Test
        @DisplayName("Should detect projection inconsistencies")
        void shouldDetectProjectionInconsistencies() {
            // Given
            String loanId = "LOAN-123";
            String customerId = "CUST-456";
            
            // Simulate inconsistent state
            LoanSummaryView loanSummary = createLoanSummaryView(loanId, customerId, "ACTIVE");
            when(projectionRepository.findLoanSummary(loanId))
                    .thenReturn(Optional.of(loanSummary));
            
            CustomerLoanHistoryView customerHistory = CustomerLoanHistoryView.builder()
                    .customerId(customerId)
                    .activeLoans(0) // Inconsistent: loan is active but count is 0
                    .build();
            when(projectionRepository.findCustomerHistory(customerId))
                    .thenReturn(Optional.of(customerHistory));

            // When
            ProjectionConsistencyCheck check = new ProjectionConsistencyCheck();
            ConsistencyCheckResult result = check.validateConsistency(loanId, customerId);

            // Then
            assertFalse(result.isConsistent());
            assertNotNull(result.getInconsistencies());
            assertFalse(result.getInconsistencies().isEmpty());
        }

        @Test
        @DisplayName("Should repair projection inconsistencies")
        void shouldRepairProjectionInconsistencies() {
            // Given
            String loanId = "LOAN-789";
            String customerId = "CUST-789";
            
            ConsistencyCheckResult inconsistentResult = ConsistencyCheckResult.builder()
                    .consistent(false)
                    .inconsistencies(List.of("Active loan count mismatch"))
                    .build();

            ProjectionRepairService repairService = new ProjectionRepairService(
                    projectionRepository, projectionEventStore);

            // When
            ProjectionRepairResult repairResult = repairService.repairInconsistencies(
                    loanId, customerId, inconsistentResult);

            // Then
            assertTrue(repairResult.isSuccessful());
            assertEquals(1, repairResult.getRepairedInconsistencies());
            assertNotNull(repairResult.getRepairActions());
        }
    }

    @Nested
    @DisplayName("Projection Performance Tests")
    class ProjectionPerformanceTests {

        @Test
        @DisplayName("Should process projections within performance thresholds")
        void shouldProcessProjectionsWithinPerformanceThresholds() {
            // Given
            LoanApplicationSubmittedEvent event = createLoanApplicationEvent();
            PerformanceMonitor monitor = new PerformanceMonitor();

            // When
            long startTime = System.currentTimeMillis();
            ProjectionResult result = loanSummaryProjection.handle(event);
            long endTime = System.currentTimeMillis();

            // Then
            assertTrue(result.isSuccessful());
            long executionTime = endTime - startTime;
            assertTrue(executionTime < 100, "Projection should complete within 100ms");
            
            ProjectionPerformanceMetrics metrics = monitor.getMetrics();
            assertTrue(metrics.getAverageProcessingTime() < 50);
        }

        @Test
        @DisplayName("Should handle high-volume projection updates efficiently")
        void shouldHandleHighVolumeProjectionUpdatesEfficiently() {
            // Given
            List<DomainEvent> events = createBulkEvents(1000);
            BatchProjectionProcessor batchProcessor = new BatchProjectionProcessor(loanSummaryProjection);

            // When
            long startTime = System.currentTimeMillis();
            BatchProjectionResult result = batchProcessor.processBatch(events);
            long endTime = System.currentTimeMillis();

            // Then
            assertTrue(result.isSuccessful());
            assertEquals(1000, result.getProcessedEvents());
            
            long totalTime = endTime - startTime;
            double eventsPerSecond = 1000.0 / (totalTime / 1000.0);
            assertTrue(eventsPerSecond > 100, "Should process at least 100 events per second");
        }
    }

    // Helper methods

    private LoanApplicationSubmittedEvent createLoanApplicationEvent() {
        return LoanApplicationSubmittedEvent.builder()
                .eventId(EventId.generate())
                .aggregateId(LoanId.generate().toString())
                .loanId(LoanId.generate())
                .customerId("CUST-123")
                .loanAmount(Money.of("USD", new BigDecimal("50000.00")))
                .loanType("PERSONAL")
                .applicationDate(LocalDateTime.now())
                .occurredOn(LocalDateTime.now())
                .version(1L)
                .build();
    }

    private LoanApprovedEvent createLoanApprovedEvent() {
        return LoanApprovedEvent.builder()
                .eventId(EventId.generate())
                .aggregateId(LoanId.generate().toString())
                .loanId(LoanId.generate())
                .approvedAmount(Money.of("USD", new BigDecimal("45000.00")))
                .interestRate(new BigDecimal("5.5"))
                .approvalDate(LocalDateTime.now())
                .occurredOn(LocalDateTime.now())
                .version(2L)
                .build();
    }

    private PaymentProcessedEvent createPaymentProcessedEvent() {
        return PaymentProcessedEvent.builder()
                .eventId(EventId.generate())
                .aggregateId("PAYMENT-123")
                .paymentId("PAY-123")
                .loanId(LoanId.generate())
                .paymentAmount(Money.of("USD", new BigDecimal("1000.00")))
                .paymentDate(LocalDateTime.now())
                .occurredOn(LocalDateTime.now())
                .version(1L)
                .build();
    }

    private List<DomainEvent> createLoanEventHistory(String loanId) {
        return List.of(
            LoanApplicationSubmittedEvent.builder()
                .eventId(EventId.generate())
                .aggregateId(loanId)
                .loanId(LoanId.of(loanId))
                .customerId("CUST-123")
                .loanAmount(Money.of("USD", new BigDecimal("50000.00")))
                .version(1L)
                .occurredOn(LocalDateTime.now().minusHours(3))
                .build(),
            LoanApprovedEvent.builder()
                .eventId(EventId.generate())
                .aggregateId(loanId)
                .loanId(LoanId.of(loanId))
                .approvedAmount(Money.of("USD", new BigDecimal("45000.00")))
                .interestRate(new BigDecimal("5.5"))
                .version(2L)
                .occurredOn(LocalDateTime.now().minusHours(2))
                .build(),
            LoanDisbursedEvent.builder()
                .eventId(EventId.generate())
                .aggregateId(loanId)
                .loanId(LoanId.of(loanId))
                .disbursedAmount(Money.of("USD", new BigDecimal("45000.00")))
                .version(3L)
                .occurredOn(LocalDateTime.now().minusHours(1))
                .build()
        );
    }

    private List<DomainEvent> createCustomerEventHistory(String customerId) {
        // Create events for 2 loans for the customer
        String loan1Id = LoanId.generate().toString();
        String loan2Id = LoanId.generate().toString();
        
        return List.of(
            // Loan 1 events
            LoanApplicationSubmittedEvent.builder()
                .aggregateId(loan1Id).customerId(customerId).version(1L)
                .occurredOn(LocalDateTime.now().minusDays(10)).build(),
            LoanApprovedEvent.builder()
                .aggregateId(loan1Id).loanId(LoanId.of(loan1Id)).version(2L)
                .occurredOn(LocalDateTime.now().minusDays(9)).build(),
            LoanDisbursedEvent.builder()
                .aggregateId(loan1Id).loanId(LoanId.of(loan1Id)).version(3L)
                .occurredOn(LocalDateTime.now().minusDays(8)).build(),
            // Loan 2 events
            LoanApplicationSubmittedEvent.builder()
                .aggregateId(loan2Id).customerId(customerId).version(1L)
                .occurredOn(LocalDateTime.now().minusDays(5)).build(),
            LoanApprovedEvent.builder()
                .aggregateId(loan2Id).loanId(LoanId.of(loan2Id)).version(2L)
                .occurredOn(LocalDateTime.now().minusDays(4)).build(),
            LoanDisbursedEvent.builder()
                .aggregateId(loan2Id).loanId(LoanId.of(loan2Id)).version(3L)
                .occurredOn(LocalDateTime.now().minusDays(3)).build()
        );
    }

    private LoanSummaryView createLoanSummaryView(String loanId, String customerId, String status) {
        return LoanSummaryView.builder()
                .loanId(loanId)
                .customerId(customerId)
                .status(status)
                .requestedAmount(Money.of("USD", new BigDecimal("50000.00")))
                .approvedAmount(Money.of("USD", new BigDecimal("45000.00")))
                .interestRate(new BigDecimal("5.5"))
                .applicationDate(LocalDateTime.now().minusDays(7))
                .lastUpdated(LocalDateTime.now())
                .version(1L)
                .build();
    }

    private PaymentSummaryView createPaymentSummaryView(String loanId, int totalPayments) {
        return PaymentSummaryView.builder()
                .loanId(loanId)
                .totalPayments(totalPayments)
                .totalAmountPaid(Money.of("USD", new BigDecimal(String.valueOf(totalPayments * 1000))))
                .lastPaymentDate(LocalDateTime.now().minusDays(1))
                .remainingBalance(Money.of("USD", new BigDecimal("40000.00")))
                .build();
    }

    private List<DomainEvent> createBulkEvents(int count) {
        List<DomainEvent> events = new java.util.ArrayList<>();
        for (int i = 0; i < count; i++) {
            events.add(createLoanApplicationEvent());
        }
        return events;
    }
}