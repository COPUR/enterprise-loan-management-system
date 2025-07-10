package com.loanmanagement.events.domain;

import com.loanmanagement.events.domain.model.*;
import com.loanmanagement.events.domain.service.EventStore;
import com.loanmanagement.loan.domain.model.LoanId;
import com.loanmanagement.shared.domain.Money;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for Event Sourcing Aggregate
 * Comprehensive testing of event-sourced aggregates, state reconstruction,
 * and aggregate lifecycle management
 */
@DisplayName("Event Sourcing Aggregate Tests")
class EventSourcingAggregateTest {

    private EventStore eventStore;
    private LoanAggregate loanAggregate;

    @BeforeEach
    void setUp() {
        eventStore = new EventStore();
        loanAggregate = new LoanAggregate();
    }

    @Nested
    @DisplayName("Aggregate Creation Tests")
    class AggregateCreationTests {

        @Test
        @DisplayName("Should create new aggregate with initial event")
        void shouldCreateNewAggregateWithInitialEvent() {
            // Given
            LoanId loanId = LoanId.generate();
            Money loanAmount = Money.of("USD", new BigDecimal("100000.00"));
            String customerId = "CUST-123";

            // When
            LoanAggregate aggregate = LoanAggregate.createNewLoan(loanId, customerId, loanAmount);

            // Then
            assertNotNull(aggregate);
            assertEquals(loanId, aggregate.getId());
            assertEquals(1, aggregate.getVersion());
            assertEquals(1, aggregate.getUncommittedEvents().size());
            assertTrue(aggregate.hasUncommittedEvents());
            
            DomainEvent firstEvent = aggregate.getUncommittedEvents().get(0);
            assertInstanceOf(LoanApplicationSubmittedEvent.class, firstEvent);
            assertEquals(loanId.toString(), firstEvent.getAggregateId());
            assertEquals(1L, firstEvent.getVersion());
        }

        @Test
        @DisplayName("Should fail to create aggregate with invalid parameters")
        void shouldFailToCreateAggregateWithInvalidParameters() {
            // Given
            LoanId nullLoanId = null;
            String nullCustomerId = null;
            Money nullAmount = null;

            // When & Then
            assertThrows(IllegalArgumentException.class, () -> 
                LoanAggregate.createNewLoan(nullLoanId, "CUST-123", Money.of("USD", BigDecimal.valueOf(10000))));
            
            assertThrows(IllegalArgumentException.class, () -> 
                LoanAggregate.createNewLoan(LoanId.generate(), nullCustomerId, Money.of("USD", BigDecimal.valueOf(10000))));
            
            assertThrows(IllegalArgumentException.class, () -> 
                LoanAggregate.createNewLoan(LoanId.generate(), "CUST-123", nullAmount));
        }

        @Test
        @DisplayName("Should create aggregate with proper event metadata")
        void shouldCreateAggregateWithProperEventMetadata() {
            // Given
            LoanId loanId = LoanId.generate();
            Money loanAmount = Money.of("USD", new BigDecimal("50000.00"));
            String customerId = "CUST-456";

            // When
            LoanAggregate aggregate = LoanAggregate.createNewLoan(loanId, customerId, loanAmount);

            // Then
            DomainEvent event = aggregate.getUncommittedEvents().get(0);
            assertNotNull(event.getEventId());
            assertNotNull(event.getOccurredOn());
            assertTrue(event.getOccurredOn().isBefore(LocalDateTime.now().plusSeconds(1)));
            assertTrue(event.getOccurredOn().isAfter(LocalDateTime.now().minusSeconds(1)));
        }
    }

    @Nested
    @DisplayName("Aggregate State Management Tests")
    class AggregateStateManagementTests {

        @Test
        @DisplayName("Should apply business operations and generate events")
        void shouldApplyBusinessOperationsAndGenerateEvents() {
            // Given
            LoanAggregate aggregate = createSampleLoanAggregate();

            // When
            aggregate.approveLoan(Money.of("USD", new BigDecimal("95000.00")), new BigDecimal("5.5"));
            aggregate.disburseLoan();

            // Then
            assertEquals(3, aggregate.getUncommittedEvents().size());
            assertEquals(3, aggregate.getVersion());
            
            List<DomainEvent> events = aggregate.getUncommittedEvents();
            assertInstanceOf(LoanApplicationSubmittedEvent.class, events.get(0));
            assertInstanceOf(LoanApprovedEvent.class, events.get(1));
            assertInstanceOf(LoanDisbursedEvent.class, events.get(2));
        }

        @Test
        @DisplayName("Should maintain event version sequence")
        void shouldMaintainEventVersionSequence() {
            // Given
            LoanAggregate aggregate = createSampleLoanAggregate();

            // When
            aggregate.approveLoan(Money.of("USD", new BigDecimal("90000.00")), new BigDecimal("6.0"));
            aggregate.disburseLoan();
            aggregate.makePayment(Money.of("USD", new BigDecimal("1000.00")));

            // Then
            List<DomainEvent> events = aggregate.getUncommittedEvents();
            for (int i = 0; i < events.size(); i++) {
                assertEquals(i + 1, events.get(i).getVersion());
            }
        }

        @Test
        @DisplayName("Should validate business rules before applying operations")
        void shouldValidateBusinessRulesBeforeApplyingOperations() {
            // Given
            LoanAggregate aggregate = createSampleLoanAggregate();

            // When & Then - Try to disburse before approval
            assertThrows(IllegalStateException.class, () -> aggregate.disburseLoan());

            // When & Then - Try to make payment before disbursement
            assertThrows(IllegalStateException.class, () -> 
                aggregate.makePayment(Money.of("USD", new BigDecimal("1000.00"))));
        }

        @Test
        @DisplayName("Should track aggregate state changes")
        void shouldTrackAggregateStateChanges() {
            // Given
            LoanAggregate aggregate = createSampleLoanAggregate();

            // When
            aggregate.approveLoan(Money.of("USD", new BigDecimal("80000.00")), new BigDecimal("4.5"));

            // Then
            assertEquals(LoanStatus.APPROVED, aggregate.getStatus());
            assertEquals(Money.of("USD", new BigDecimal("80000.00")), aggregate.getApprovedAmount());
            assertEquals(new BigDecimal("4.5"), aggregate.getInterestRate());
        }
    }

    @Nested
    @DisplayName("Event Sourcing Reconstruction Tests")
    class EventSourcingReconstructionTests {

        @Test
        @DisplayName("Should reconstruct aggregate from stored events")
        void shouldReconstructAggregateFromStoredEvents() {
            // Given
            LoanId loanId = LoanId.generate();
            List<DomainEvent> historicalEvents = createHistoricalEvents(loanId);
            historicalEvents.forEach(eventStore::store);

            // When
            LoanAggregate reconstructedAggregate = LoanAggregate.fromHistory(loanId, historicalEvents);

            // Then
            assertNotNull(reconstructedAggregate);
            assertEquals(loanId, reconstructedAggregate.getId());
            assertEquals(historicalEvents.size(), reconstructedAggregate.getVersion());
            assertEquals(LoanStatus.DISBURSED, reconstructedAggregate.getStatus());
            assertFalse(reconstructedAggregate.hasUncommittedEvents());
        }

        @Test
        @DisplayName("Should reconstruct aggregate state accurately")
        void shouldReconstructAggregateStateAccurately() {
            // Given
            LoanId loanId = LoanId.generate();
            Money originalAmount = Money.of("USD", new BigDecimal("75000.00"));
            Money approvedAmount = Money.of("USD", new BigDecimal("70000.00"));
            BigDecimal interestRate = new BigDecimal("5.2");

            List<DomainEvent> events = List.of(
                LoanApplicationSubmittedEvent.builder()
                    .eventId(EventId.generate())
                    .aggregateId(loanId.toString())
                    .loanId(loanId)
                    .customerId("CUST-789")
                    .loanAmount(originalAmount)
                    .loanType("PERSONAL")
                    .occurredOn(LocalDateTime.now().minusHours(2))
                    .version(1L)
                    .build(),
                LoanApprovedEvent.builder()
                    .eventId(EventId.generate())
                    .aggregateId(loanId.toString())
                    .loanId(loanId)
                    .approvedAmount(approvedAmount)
                    .interestRate(interestRate)
                    .occurredOn(LocalDateTime.now().minusHours(1))
                    .version(2L)
                    .build()
            );

            // When
            LoanAggregate aggregate = LoanAggregate.fromHistory(loanId, events);

            // Then
            assertEquals(originalAmount, aggregate.getRequestedAmount());
            assertEquals(approvedAmount, aggregate.getApprovedAmount());
            assertEquals(interestRate, aggregate.getInterestRate());
            assertEquals(LoanStatus.APPROVED, aggregate.getStatus());
        }

        @Test
        @DisplayName("Should handle empty event history")
        void shouldHandleEmptyEventHistory() {
            // Given
            LoanId loanId = LoanId.generate();
            List<DomainEvent> emptyEvents = List.of();

            // When & Then
            assertThrows(IllegalArgumentException.class, () -> 
                LoanAggregate.fromHistory(loanId, emptyEvents));
        }

        @Test
        @DisplayName("Should validate event sequence during reconstruction")
        void shouldValidateEventSequenceDuringReconstruction() {
            // Given
            LoanId loanId = LoanId.generate();
            List<DomainEvent> outOfOrderEvents = List.of(
                LoanApprovedEvent.builder()
                    .eventId(EventId.generate())
                    .aggregateId(loanId.toString())
                    .version(2L)
                    .occurredOn(LocalDateTime.now())
                    .build(),
                LoanApplicationSubmittedEvent.builder()
                    .eventId(EventId.generate())
                    .aggregateId(loanId.toString())
                    .version(1L)
                    .occurredOn(LocalDateTime.now().minusHours(1))
                    .build()
            );

            // When & Then
            assertThrows(IllegalStateException.class, () -> 
                LoanAggregate.fromHistory(loanId, outOfOrderEvents));
        }
    }

    @Nested
    @DisplayName("Aggregate Snapshot Tests")
    class AggregateSnapshotTests {

        @Test
        @DisplayName("Should create aggregate snapshot")
        void shouldCreateAggregateSnapshot() {
            // Given
            LoanAggregate aggregate = createComplexLoanAggregate();

            // When
            AggregateSnapshot snapshot = aggregate.createSnapshot();

            // Then
            assertNotNull(snapshot);
            assertEquals(aggregate.getId().toString(), snapshot.getAggregateId());
            assertEquals(aggregate.getVersion(), snapshot.getVersion());
            assertNotNull(snapshot.getSnapshotData());
            assertNotNull(snapshot.getCreatedAt());
        }

        @Test
        @DisplayName("Should restore aggregate from snapshot")
        void shouldRestoreAggregateFromSnapshot() {
            // Given
            LoanAggregate originalAggregate = createComplexLoanAggregate();
            AggregateSnapshot snapshot = originalAggregate.createSnapshot();

            // When
            LoanAggregate restoredAggregate = LoanAggregate.fromSnapshot(snapshot);

            // Then
            assertEquals(originalAggregate.getId(), restoredAggregate.getId());
            assertEquals(originalAggregate.getVersion(), restoredAggregate.getVersion());
            assertEquals(originalAggregate.getStatus(), restoredAggregate.getStatus());
            assertEquals(originalAggregate.getApprovedAmount(), restoredAggregate.getApprovedAmount());
        }

        @Test
        @DisplayName("Should apply events after snapshot restoration")
        void shouldApplyEventsAfterSnapshotRestoration() {
            // Given
            LoanAggregate originalAggregate = createComplexLoanAggregate();
            AggregateSnapshot snapshot = originalAggregate.createSnapshot();
            LoanAggregate restoredAggregate = LoanAggregate.fromSnapshot(snapshot);

            // When
            restoredAggregate.makePayment(Money.of("USD", new BigDecimal("2000.00")));

            // Then
            assertEquals(originalAggregate.getVersion() + 1, restoredAggregate.getVersion());
            assertTrue(restoredAggregate.hasUncommittedEvents());
            assertEquals(1, restoredAggregate.getUncommittedEvents().size());
        }
    }

    @Nested
    @DisplayName("Aggregate Event Commit Tests")
    class AggregateEventCommitTests {

        @Test
        @DisplayName("Should commit uncommitted events")
        void shouldCommitUncommittedEvents() {
            // Given
            LoanAggregate aggregate = createSampleLoanAggregate();
            aggregate.approveLoan(Money.of("USD", new BigDecimal("85000.00")), new BigDecimal("5.8"));

            // When
            aggregate.markEventsAsCommitted();

            // Then
            assertFalse(aggregate.hasUncommittedEvents());
            assertEquals(0, aggregate.getUncommittedEvents().size());
        }

        @Test
        @DisplayName("Should maintain version after commit")
        void shouldMaintainVersionAfterCommit() {
            // Given
            LoanAggregate aggregate = createSampleLoanAggregate();
            aggregate.approveLoan(Money.of("USD", new BigDecimal("85000.00")), new BigDecimal("5.8"));
            long versionBeforeCommit = aggregate.getVersion();

            // When
            aggregate.markEventsAsCommitted();

            // Then
            assertEquals(versionBeforeCommit, aggregate.getVersion());
        }

        @Test
        @DisplayName("Should continue generating events after commit")
        void shouldContinueGeneratingEventsAfterCommit() {
            // Given
            LoanAggregate aggregate = createSampleLoanAggregate();
            aggregate.approveLoan(Money.of("USD", new BigDecimal("85000.00")), new BigDecimal("5.8"));
            aggregate.markEventsAsCommitted();

            // When
            aggregate.disburseLoan();

            // Then
            assertTrue(aggregate.hasUncommittedEvents());
            assertEquals(1, aggregate.getUncommittedEvents().size());
            assertEquals(3, aggregate.getVersion()); // Original + approved + disbursed
        }
    }

    @Nested
    @DisplayName("Aggregate Concurrency Tests")
    class AggregateConcurrencyTests {

        @Test
        @DisplayName("Should detect concurrent modification conflicts")
        void shouldDetectConcurrentModificationConflicts() {
            // Given
            LoanId loanId = LoanId.generate();
            LoanAggregate aggregate1 = createSampleLoanAggregateWithId(loanId);
            LoanAggregate aggregate2 = createSampleLoanAggregateWithId(loanId);

            // When
            aggregate1.approveLoan(Money.of("USD", new BigDecimal("80000.00")), new BigDecimal("5.0"));
            aggregate2.rejectLoan("Insufficient income");

            // Then
            ConcurrencyConflictResult conflict = aggregate1.detectConcurrencyConflict(aggregate2);
            assertTrue(conflict.hasConflict());
            assertNotNull(conflict.getConflictDescription());
        }

        @Test
        @DisplayName("Should handle optimistic locking")
        void shouldHandleOptimisticLocking() {
            // Given
            LoanAggregate aggregate = createSampleLoanAggregate();
            long expectedVersion = aggregate.getVersion();

            // When
            OptimisticLockResult lockResult = aggregate.checkOptimisticLock(expectedVersion);

            // Then
            assertTrue(lockResult.isLockValid());
            assertNull(lockResult.getConflictReason());
        }

        @Test
        @DisplayName("Should fail optimistic lock check with wrong version")
        void shouldFailOptimisticLockCheckWithWrongVersion() {
            // Given
            LoanAggregate aggregate = createSampleLoanAggregate();
            aggregate.approveLoan(Money.of("USD", new BigDecimal("75000.00")), new BigDecimal("6.0"));
            long outdatedVersion = 1L; // Original version before approval

            // When
            OptimisticLockResult lockResult = aggregate.checkOptimisticLock(outdatedVersion);

            // Then
            assertFalse(lockResult.isLockValid());
            assertNotNull(lockResult.getConflictReason());
        }
    }

    // Helper methods

    private LoanAggregate createSampleLoanAggregate() {
        return LoanAggregate.createNewLoan(
            LoanId.generate(),
            "CUST-123",
            Money.of("USD", new BigDecimal("100000.00"))
        );
    }

    private LoanAggregate createSampleLoanAggregateWithId(LoanId loanId) {
        return LoanAggregate.createNewLoan(
            loanId,
            "CUST-123",
            Money.of("USD", new BigDecimal("100000.00"))
        );
    }

    private LoanAggregate createComplexLoanAggregate() {
        LoanAggregate aggregate = createSampleLoanAggregate();
        aggregate.approveLoan(Money.of("USD", new BigDecimal("90000.00")), new BigDecimal("5.5"));
        aggregate.disburseLoan();
        return aggregate;
    }

    private List<DomainEvent> createHistoricalEvents(LoanId loanId) {
        return List.of(
            LoanApplicationSubmittedEvent.builder()
                .eventId(EventId.generate())
                .aggregateId(loanId.toString())
                .loanId(loanId)
                .customerId("CUST-456")
                .loanAmount(Money.of("USD", new BigDecimal("80000.00")))
                .loanType("PERSONAL")
                .occurredOn(LocalDateTime.now().minusHours(3))
                .version(1L)
                .build(),
            LoanApprovedEvent.builder()
                .eventId(EventId.generate())
                .aggregateId(loanId.toString())
                .loanId(loanId)
                .approvedAmount(Money.of("USD", new BigDecimal("75000.00")))
                .interestRate(new BigDecimal("5.0"))
                .occurredOn(LocalDateTime.now().minusHours(2))
                .version(2L)
                .build(),
            LoanDisbursedEvent.builder()
                .eventId(EventId.generate())
                .aggregateId(loanId.toString())
                .loanId(loanId)
                .disbursedAmount(Money.of("USD", new BigDecimal("75000.00")))
                .disbursementDate(LocalDateTime.now().minusHours(1))
                .occurredOn(LocalDateTime.now().minusHours(1))
                .version(3L)
                .build()
        );
    }
}