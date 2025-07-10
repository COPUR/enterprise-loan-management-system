package com.loanmanagement.payment.domain;

import com.loanmanagement.loan.domain.model.*;
import com.loanmanagement.payment.domain.model.*;
import com.loanmanagement.payment.domain.service.PaymentScheduleService;
import com.loanmanagement.payment.domain.service.PaymentReminderService;
import com.loanmanagement.payment.domain.service.PaymentReconciliationService;
import com.loanmanagement.shared.domain.Money;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for Payment Schedule Management
 * Comprehensive testing of payment scheduling and tracking
 */
@DisplayName("Payment Schedule Management Tests")
class PaymentScheduleManagementTest {

    private PaymentScheduleService paymentScheduleService;
    private PaymentReminderService paymentReminderService;
    private PaymentReconciliationService paymentReconciliationService;
    private LoanId testLoanId;
    private CustomerId testCustomerId;
    private Money monthlyPaymentAmount;

    @BeforeEach
    void setUp() {
        paymentScheduleService = new PaymentScheduleService();
        paymentReminderService = new PaymentReminderService();
        paymentReconciliationService = new PaymentReconciliationService();
        testLoanId = LoanId.generate();
        testCustomerId = CustomerId.generate();
        monthlyPaymentAmount = Money.of("USD", new BigDecimal("1200.00"));
    }

    @Nested
    @DisplayName("Payment Schedule Generation Tests")
    class PaymentScheduleGenerationTests {

        @Test
        @DisplayName("Should generate monthly payment schedule")
        void shouldGenerateMonthlyPaymentSchedule() {
            // Given
            PaymentScheduleRequest scheduleRequest = PaymentScheduleRequest.builder()
                    .loanId(testLoanId)
                    .customerId(testCustomerId)
                    .startDate(LocalDate.of(2024, 1, 15))
                    .endDate(LocalDate.of(2054, 1, 15)) // 30 years
                    .paymentAmount(monthlyPaymentAmount)
                    .frequency(PaymentFrequency.MONTHLY)
                    .dayOfMonth(15)
                    .build();

            // When
            PaymentSchedule schedule = paymentScheduleService.generatePaymentSchedule(scheduleRequest);

            // Then
            assertNotNull(schedule);
            assertEquals(360, schedule.getScheduledPayments().size()); // 30 years * 12 months
            assertEquals(testLoanId, schedule.getLoanId());
            assertEquals(PaymentFrequency.MONTHLY, schedule.getFrequency());
            
            // Verify first and last payment dates
            ScheduledPayment firstPayment = schedule.getScheduledPayments().get(0);
            ScheduledPayment lastPayment = schedule.getScheduledPayments().get(359);
            assertEquals(LocalDate.of(2024, 2, 15), firstPayment.getDueDate());
            assertEquals(LocalDate.of(2054, 1, 15), lastPayment.getDueDate());
        }

        @Test
        @DisplayName("Should generate bi-weekly payment schedule")
        void shouldGenerateBiWeeklyPaymentSchedule() {
            // Given
            PaymentScheduleRequest biWeeklyRequest = PaymentScheduleRequest.builder()
                    .loanId(testLoanId)
                    .customerId(testCustomerId)
                    .startDate(LocalDate.of(2024, 1, 1))
                    .endDate(LocalDate.of(2039, 1, 1)) // 15 years
                    .paymentAmount(Money.of("USD", new BigDecimal("600.00"))) // Half monthly
                    .frequency(PaymentFrequency.BI_WEEKLY)
                    .dayOfWeek(5) // Friday
                    .build();

            // When
            PaymentSchedule schedule = paymentScheduleService.generatePaymentSchedule(biWeeklyRequest);

            // Then
            assertNotNull(schedule);
            assertEquals(390, schedule.getScheduledPayments().size()); // 15 years * 26 payments/year
            assertEquals(PaymentFrequency.BI_WEEKLY, schedule.getFrequency());
            
            // Verify payment dates are bi-weekly and on Fridays
            ScheduledPayment firstPayment = schedule.getScheduledPayments().get(0);
            ScheduledPayment secondPayment = schedule.getScheduledPayments().get(1);
            assertEquals(5, firstPayment.getDueDate().getDayOfWeek().getValue()); // Friday
            assertEquals(14, secondPayment.getDueDate().toEpochDay() - firstPayment.getDueDate().toEpochDay());
        }

        @Test
        @DisplayName("Should handle payment schedule with grace periods")
        void shouldHandlePaymentScheduleWithGracePeriods() {
            // Given
            PaymentScheduleRequest graceRequest = PaymentScheduleRequest.builder()
                    .loanId(testLoanId)
                    .customerId(testCustomerId)
                    .startDate(LocalDate.of(2024, 1, 15))
                    .endDate(LocalDate.of(2034, 1, 15))
                    .paymentAmount(monthlyPaymentAmount)
                    .frequency(PaymentFrequency.MONTHLY)
                    .gracePeriodDays(10)
                    .build();

            // When
            PaymentSchedule schedule = paymentScheduleService.generatePaymentSchedule(graceRequest);

            // Then
            assertNotNull(schedule);
            schedule.getScheduledPayments().forEach(payment -> {
                assertEquals(10, payment.getGracePeriodDays());
                assertNotNull(payment.getLateFeeDate());
                assertEquals(payment.getDueDate().plusDays(10), payment.getLateFeeDate());
            });
        }

        @Test
        @DisplayName("Should adjust payment schedule for holidays")
        void shouldAdjustPaymentScheduleForHolidays() {
            // Given
            List<LocalDate> holidays = List.of(
                    LocalDate.of(2024, 7, 4), // Independence Day
                    LocalDate.of(2024, 12, 25) // Christmas
            );

            PaymentScheduleRequest holidayRequest = PaymentScheduleRequest.builder()
                    .loanId(testLoanId)
                    .customerId(testCustomerId)
                    .startDate(LocalDate.of(2024, 1, 1))
                    .endDate(LocalDate.of(2025, 1, 1))
                    .paymentAmount(monthlyPaymentAmount)
                    .frequency(PaymentFrequency.MONTHLY)
                    .holidays(holidays)
                    .holidayAdjustmentRule(HolidayAdjustmentRule.NEXT_BUSINESS_DAY)
                    .build();

            // When
            PaymentSchedule schedule = paymentScheduleService.generatePaymentSchedule(holidayRequest);

            // Then
            assertNotNull(schedule);
            
            // Verify holiday adjustments
            ScheduledPayment julyPayment = schedule.getScheduledPayments().stream()
                    .filter(p -> p.getDueDate().getMonth().getValue() == 7)
                    .findFirst().orElseThrow();
            
            // If payment falls on July 4th (holiday), it should be moved to next business day
            if (julyPayment.getOriginalDueDate().equals(LocalDate.of(2024, 7, 4))) {
                assertTrue(julyPayment.getDueDate().isAfter(LocalDate.of(2024, 7, 4)));
                assertTrue(julyPayment.isHolidayAdjusted());
            }
        }
    }

    @Nested
    @DisplayName("Payment Schedule Modification Tests")
    class PaymentScheduleModificationTests {

        @Test
        @DisplayName("Should modify payment schedule for amount change")
        void shouldModifyPaymentScheduleForAmountChange() {
            // Given
            PaymentSchedule originalSchedule = createSamplePaymentSchedule();
            Money newPaymentAmount = Money.of("USD", new BigDecimal("1400.00"));

            PaymentScheduleModification modification = PaymentScheduleModification.builder()
                    .scheduleId(originalSchedule.getScheduleId())
                    .modificationType(PaymentScheduleModificationType.AMOUNT_CHANGE)
                    .newPaymentAmount(newPaymentAmount)
                    .effectiveDate(LocalDate.now().plusMonths(1))
                    .reason("Customer requested payment increase")
                    .build();

            // When
            PaymentSchedule modifiedSchedule = paymentScheduleService
                    .modifyPaymentSchedule(originalSchedule, modification);

            // Then
            assertNotNull(modifiedSchedule);
            assertEquals(1, modifiedSchedule.getModifications().size());
            
            // Verify payments after effective date have new amount
            modifiedSchedule.getScheduledPayments().stream()
                    .filter(p -> p.getDueDate().isAfter(modification.getEffectiveDate()))
                    .forEach(p -> assertEquals(newPaymentAmount, p.getPaymentAmount()));
        }

        @Test
        @DisplayName("Should modify payment schedule for frequency change")
        void shouldModifyPaymentScheduleForFrequencyChange() {
            // Given
            PaymentSchedule originalSchedule = createSamplePaymentSchedule();

            PaymentScheduleModification modification = PaymentScheduleModification.builder()
                    .scheduleId(originalSchedule.getScheduleId())
                    .modificationType(PaymentScheduleModificationType.FREQUENCY_CHANGE)
                    .newFrequency(PaymentFrequency.BI_WEEKLY)
                    .newPaymentAmount(Money.of("USD", new BigDecimal("600.00")))
                    .effectiveDate(LocalDate.now().plusMonths(1))
                    .reason("Customer wants to pay bi-weekly")
                    .build();

            // When
            PaymentSchedule modifiedSchedule = paymentScheduleService
                    .modifyPaymentSchedule(originalSchedule, modification);

            // Then
            assertNotNull(modifiedSchedule);
            assertEquals(PaymentFrequency.BI_WEEKLY, modifiedSchedule.getFrequency());
            
            // Verify more frequent payments after effective date
            long paymentsAfterChange = modifiedSchedule.getScheduledPayments().stream()
                    .filter(p -> p.getDueDate().isAfter(modification.getEffectiveDate()))
                    .count();
            
            assertTrue(paymentsAfterChange > 0);
        }

        @Test
        @DisplayName("Should handle payment schedule suspension")
        void shouldHandlePaymentScheduleSuspension() {
            // Given
            PaymentSchedule activeSchedule = createSamplePaymentSchedule();

            PaymentScheduleSuspension suspension = PaymentScheduleSuspension.builder()
                    .scheduleId(activeSchedule.getScheduleId())
                    .suspensionStartDate(LocalDate.now().plusDays(5))
                    .suspensionEndDate(LocalDate.now().plusMonths(3))
                    .suspensionReason(PaymentSuspensionReason.FINANCIAL_HARDSHIP)
                    .approvedBy("underwriter-123")
                    .build();

            // When
            PaymentSchedule suspendedSchedule = paymentScheduleService
                    .suspendPaymentSchedule(activeSchedule, suspension);

            // Then
            assertNotNull(suspendedSchedule);
            assertEquals(PaymentScheduleStatus.SUSPENDED, suspendedSchedule.getStatus());
            assertEquals(1, suspendedSchedule.getSuspensions().size());
            
            // Verify payments during suspension period are marked
            suspendedSchedule.getScheduledPayments().stream()
                    .filter(p -> p.getDueDate().isAfter(suspension.getSuspensionStartDate()) &&
                            p.getDueDate().isBefore(suspension.getSuspensionEndDate()))
                    .forEach(p -> assertEquals(PaymentStatus.SUSPENDED, p.getStatus()));
        }
    }

    @Nested
    @DisplayName("Payment Reminder Tests")
    class PaymentReminderTests {

        @Test
        @DisplayName("Should generate payment reminders before due date")
        void shouldGeneratePaymentRemindersBeforeDueDate() {
            // Given
            PaymentSchedule schedule = createSamplePaymentSchedule();
            PaymentReminderConfig reminderConfig = PaymentReminderConfig.builder()
                    .reminderDaysBefore(List.of(7, 3, 1))
                    .reminderChannels(List.of(ReminderChannel.EMAIL, ReminderChannel.SMS))
                    .customerId(testCustomerId)
                    .build();

            // When
            List<PaymentReminder> reminders = paymentReminderService
                    .generatePaymentReminders(schedule, reminderConfig);

            // Then
            assertNotNull(reminders);
            assertFalse(reminders.isEmpty());
            
            // Verify reminders are generated for upcoming payments
            PaymentReminder firstReminder = reminders.get(0);
            assertNotNull(firstReminder.getReminderId());
            assertEquals(testCustomerId, firstReminder.getCustomerId());
            assertNotNull(firstReminder.getScheduledDate());
            assertEquals(ReminderStatus.SCHEDULED, firstReminder.getStatus());
        }

        @Test
        @DisplayName("Should handle late payment notifications")
        void shouldHandleLatePaymentNotifications() {
            // Given
            ScheduledPayment overduePayment = ScheduledPayment.builder()
                    .paymentId(PaymentId.generate())
                    .loanId(testLoanId)
                    .dueDate(LocalDate.now().minusDays(5)) // 5 days overdue
                    .paymentAmount(monthlyPaymentAmount)
                    .status(PaymentStatus.OVERDUE)
                    .build();

            // When
            LatePaymentNotification notification = paymentReminderService
                    .generateLatePaymentNotification(overduePayment, testCustomerId);

            // Then
            assertNotNull(notification);
            assertEquals(testCustomerId, notification.getCustomerId());
            assertEquals(testLoanId, notification.getLoanId());
            assertEquals(5, notification.getDaysOverdue());
            assertTrue(notification.getLateFeeAmount().getAmount().compareTo(BigDecimal.ZERO) > 0);
            assertEquals(NotificationPriority.HIGH, notification.getPriority());
        }

        @Test
        @DisplayName("Should customize reminder preferences per customer")
        void shouldCustomizeReminderPreferencesPerCustomer() {
            // Given
            CustomerReminderPreferences preferences = CustomerReminderPreferences.builder()
                    .customerId(testCustomerId)
                    .preferredChannels(List.of(ReminderChannel.EMAIL))
                    .reminderDaysBefore(List.of(10, 5))
                    .optOut(false)
                    .preferredTime("09:00")
                    .timeZone("EST")
                    .build();

            PaymentSchedule schedule = createSamplePaymentSchedule();

            // When
            List<PaymentReminder> customizedReminders = paymentReminderService
                    .generateCustomizedReminders(schedule, preferences);

            // Then
            assertNotNull(customizedReminders);
            
            // Verify reminders follow customer preferences
            customizedReminders.forEach(reminder -> {
                assertTrue(reminder.getChannels().containsAll(preferences.getPreferredChannels()));
                assertEquals(preferences.getTimeZone(), reminder.getTimeZone());
            });
        }
    }

    @Nested
    @DisplayName("Payment Reconciliation Tests")
    class PaymentReconciliationTests {

        @Test
        @DisplayName("Should reconcile scheduled vs actual payments")
        void shouldReconcileScheduledVsActualPayments() {
            // Given
            PaymentSchedule schedule = createSamplePaymentSchedule();
            List<Payment> actualPayments = createSampleActualPayments();

            // When
            PaymentReconciliationReport reconciliationReport = paymentReconciliationService
                    .reconcilePayments(schedule, actualPayments, LocalDate.now());

            // Then
            assertNotNull(reconciliationReport);
            assertTrue(reconciliationReport.getTotalScheduledPayments() > 0);
            assertTrue(reconciliationReport.getTotalActualPayments() > 0);
            assertNotNull(reconciliationReport.getVariances());
            
            // Verify reconciliation accuracy
            assertTrue(reconciliationReport.getReconciliationAccuracy()
                    .compareTo(new BigDecimal("90")) >= 0); // At least 90% accuracy
        }

        @Test
        @DisplayName("Should identify payment discrepancies")
        void shouldIdentifyPaymentDiscrepancies() {
            // Given
            PaymentSchedule schedule = createSamplePaymentSchedule();
            List<Payment> paymentsWithDiscrepancies = createPaymentsWithDiscrepancies();

            // When
            List<PaymentDiscrepancy> discrepancies = paymentReconciliationService
                    .identifyDiscrepancies(schedule, paymentsWithDiscrepancies);

            // Then
            assertNotNull(discrepancies);
            assertFalse(discrepancies.isEmpty());
            
            // Verify different types of discrepancies are identified
            boolean hasAmountDiscrepancy = discrepancies.stream()
                    .anyMatch(d -> d.getDiscrepancyType() == PaymentDiscrepancyType.AMOUNT_MISMATCH);
            boolean hasDateDiscrepancy = discrepancies.stream()
                    .anyMatch(d -> d.getDiscrepancyType() == PaymentDiscrepancyType.DATE_MISMATCH);
            
            assertTrue(hasAmountDiscrepancy || hasDateDiscrepancy);
        }

        @Test
        @DisplayName("Should automatically resolve minor discrepancies")
        void shouldAutomaticallyResolveMinorDiscrepancies() {
            // Given
            PaymentSchedule schedule = createSamplePaymentSchedule();
            List<Payment> paymentsWithMinorIssues = createPaymentsWithMinorIssues();

            PaymentReconciliationConfig reconciliationConfig = PaymentReconciliationConfig.builder()
                    .autoResolveThreshold(Money.of("USD", new BigDecimal("5.00")))
                    .autoResolveDateVarianceDays(2)
                    .enableAutoResolution(true)
                    .build();

            // When
            PaymentReconciliationResult result = paymentReconciliationService
                    .reconcileWithAutoResolution(schedule, paymentsWithMinorIssues, reconciliationConfig);

            // Then
            assertNotNull(result);
            assertTrue(result.getAutoResolvedDiscrepancies() > 0);
            assertTrue(result.getManualReviewRequired().size() < result.getTotalDiscrepancies());
        }

        @Test
        @DisplayName("Should generate payment variance analysis")
        void shouldGeneratePaymentVarianceAnalysis() {
            // Given
            PaymentSchedule schedule = createSamplePaymentSchedule();
            List<Payment> actualPayments = createSampleActualPayments();
            LocalDate analysisStartDate = LocalDate.now().minusMonths(6);
            LocalDate analysisEndDate = LocalDate.now();

            // When
            PaymentVarianceAnalysis analysis = paymentReconciliationService
                    .analyzePaymentVariances(schedule, actualPayments, analysisStartDate, analysisEndDate);

            // Then
            assertNotNull(analysis);
            assertNotNull(analysis.getAverageVariance());
            assertNotNull(analysis.getMaxVariance());
            assertNotNull(analysis.getMinVariance());
            assertTrue(analysis.getVariancesByMonth().size() <= 6);
            assertNotNull(analysis.getVarianceTrend());
        }
    }

    // Helper methods
    private PaymentSchedule createSamplePaymentSchedule() {
        return PaymentSchedule.builder()
                .scheduleId(PaymentScheduleId.generate())
                .loanId(testLoanId)
                .customerId(testCustomerId)
                .frequency(PaymentFrequency.MONTHLY)
                .paymentAmount(monthlyPaymentAmount)
                .startDate(LocalDate.now())
                .endDate(LocalDate.now().plusYears(5))
                .status(PaymentScheduleStatus.ACTIVE)
                .scheduledPayments(generateSampleScheduledPayments())
                .build();
    }

    private List<ScheduledPayment> generateSampleScheduledPayments() {
        return List.of(
                ScheduledPayment.builder()
                        .paymentId(PaymentId.generate())
                        .loanId(testLoanId)
                        .dueDate(LocalDate.now().plusDays(30))
                        .paymentAmount(monthlyPaymentAmount)
                        .status(PaymentStatus.SCHEDULED)
                        .build(),
                ScheduledPayment.builder()
                        .paymentId(PaymentId.generate())
                        .loanId(testLoanId)
                        .dueDate(LocalDate.now().plusDays(60))
                        .paymentAmount(monthlyPaymentAmount)
                        .status(PaymentStatus.SCHEDULED)
                        .build()
        );
    }

    private List<Payment> createSampleActualPayments() {
        return List.of(
                Payment.builder()
                        .paymentId(PaymentId.generate())
                        .loanId(testLoanId)
                        .paymentAmount(monthlyPaymentAmount)
                        .paymentDate(LocalDateTime.now().minusMonths(1))
                        .paymentStatus(PaymentStatus.PROCESSED)
                        .build(),
                Payment.builder()
                        .paymentId(PaymentId.generate())
                        .loanId(testLoanId)
                        .paymentAmount(Money.of("USD", new BigDecimal("1190.00"))) // Slight variance
                        .paymentDate(LocalDateTime.now().minusDays(28))
                        .paymentStatus(PaymentStatus.PROCESSED)
                        .build()
        );
    }

    private List<Payment> createPaymentsWithDiscrepancies() {
        return List.of(
                Payment.builder()
                        .paymentId(PaymentId.generate())
                        .loanId(testLoanId)
                        .paymentAmount(Money.of("USD", new BigDecimal("1100.00"))) // $100 short
                        .paymentDate(LocalDateTime.now().minusDays(30))
                        .paymentStatus(PaymentStatus.PROCESSED)
                        .build(),
                Payment.builder()
                        .paymentId(PaymentId.generate())
                        .loanId(testLoanId)
                        .paymentAmount(monthlyPaymentAmount)
                        .paymentDate(LocalDateTime.now().minusDays(5)) // Wrong date
                        .paymentStatus(PaymentStatus.PROCESSED)
                        .build()
        );
    }

    private List<Payment> createPaymentsWithMinorIssues() {
        return List.of(
                Payment.builder()
                        .paymentId(PaymentId.generate())
                        .loanId(testLoanId)
                        .paymentAmount(Money.of("USD", new BigDecimal("1202.50"))) // $2.50 over - minor
                        .paymentDate(LocalDateTime.now().minusDays(30))
                        .paymentStatus(PaymentStatus.PROCESSED)
                        .build(),
                Payment.builder()
                        .paymentId(PaymentId.generate())
                        .loanId(testLoanId)
                        .paymentAmount(monthlyPaymentAmount)
                        .paymentDate(LocalDateTime.now().minusDays(29)) // 1 day early - minor
                        .paymentStatus(PaymentStatus.PROCESSED)
                        .build()
        );
    }
}