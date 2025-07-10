package com.loanmanagement.loan.domain;

import com.loanmanagement.loan.domain.model.*;
import com.loanmanagement.loan.domain.service.LoanCalculationService;
import com.loanmanagement.loan.domain.service.LoanInstallmentService;
import com.loanmanagement.shared.domain.Money;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for Loan Installment Calculations
 * Comprehensive testing of installment calculation logic
 */
@DisplayName("Loan Installment Calculation Tests")
class LoanInstallmentCalculationTest {

    private LoanInstallmentService installmentService;
    private LoanCalculationService calculationService;
    private Money principalAmount;
    private LoanTerms standardTerms;

    @BeforeEach
    void setUp() {
        installmentService = new LoanInstallmentService();
        calculationService = new LoanCalculationService();
        principalAmount = Money.of("USD", new BigDecimal("100000.00"));
        standardTerms = LoanTerms.builder()
                .termInMonths(360)
                .interestRate(new BigDecimal("4.5"))
                .paymentFrequency(PaymentFrequency.MONTHLY)
                .allowsEarlyPayoff(true)
                .gracePeriodDays(15)
                .build();
    }

    @Nested
    @DisplayName("Basic Installment Calculation Tests")
    class BasicInstallmentCalculationTests {

        @Test
        @DisplayName("Should calculate correct installment amount for standard loan")
        void shouldCalculateCorrectInstallmentAmountForStandardLoan() {
            // When
            InstallmentPlan plan = installmentService.createInstallmentPlan(
                    principalAmount, standardTerms, LocalDate.now());

            // Then
            assertNotNull(plan);
            assertEquals(360, plan.getTotalInstallments());
            assertNotNull(plan.getInstallmentAmount());
            assertTrue(plan.getInstallmentAmount().getAmount().compareTo(BigDecimal.ZERO) > 0);
            
            // Verify total payments approximately equal principal + interest
            Money totalPayments = plan.getTotalPaymentAmount();
            assertTrue(totalPayments.getAmount().compareTo(principalAmount.getAmount()) > 0);
        }

        @Test
        @DisplayName("Should handle different payment frequencies correctly")
        void shouldHandleDifferentPaymentFrequenciesCorrectly() {
            // Given
            LoanTerms biWeeklyTerms = standardTerms.toBuilder()
                    .paymentFrequency(PaymentFrequency.BI_WEEKLY)
                    .build();

            // When
            InstallmentPlan monthlyPlan = installmentService.createInstallmentPlan(
                    principalAmount, standardTerms, LocalDate.now());
            InstallmentPlan biWeeklyPlan = installmentService.createInstallmentPlan(
                    principalAmount, biWeeklyTerms, LocalDate.now());

            // Then
            assertEquals(360, monthlyPlan.getTotalInstallments());
            assertEquals(780, biWeeklyPlan.getTotalInstallments()); // 30 years * 26 payments/year
            
            // Bi-weekly should result in lower total interest
            assertTrue(biWeeklyPlan.getTotalInterestAmount().getAmount()
                    .compareTo(monthlyPlan.getTotalInterestAmount().getAmount()) < 0);
        }

        @Test
        @DisplayName("Should calculate installments for zero interest rate")
        void shouldCalculateInstallmentsForZeroInterestRate() {
            // Given
            LoanTerms zeroRateTerms = standardTerms.toBuilder()
                    .interestRate(BigDecimal.ZERO)
                    .termInMonths(60)
                    .build();

            // When
            InstallmentPlan plan = installmentService.createInstallmentPlan(
                    principalAmount, zeroRateTerms, LocalDate.now());

            // Then
            assertNotNull(plan);
            assertEquals(60, plan.getTotalInstallments());
            
            Money expectedPayment = principalAmount.divide(new BigDecimal("60"));
            assertEquals(expectedPayment.getAmount(), plan.getInstallmentAmount().getAmount());
            assertEquals(Money.zero("USD").getAmount(), plan.getTotalInterestAmount().getAmount());
        }

        @Test
        @DisplayName("Should fail for invalid loan terms")
        void shouldFailForInvalidLoanTerms() {
            // Given
            LoanTerms invalidTerms = standardTerms.toBuilder()
                    .termInMonths(0)
                    .build();

            // When & Then
            assertThrows(IllegalArgumentException.class, () ->
                    installmentService.createInstallmentPlan(principalAmount, invalidTerms, LocalDate.now()));
        }
    }

    @Nested
    @DisplayName("Variable Rate Installment Tests")
    class VariableRateInstallmentTests {

        @Test
        @DisplayName("Should calculate installments for variable rate loan")
        void shouldCalculateInstallmentsForVariableRateLoan() {
            // Given
            List<RateAdjustment> rateAdjustments = List.of(
                    RateAdjustment.create(LocalDate.now().plusYears(1), new BigDecimal("5.0")),
                    RateAdjustment.create(LocalDate.now().plusYears(3), new BigDecimal("4.0"))
            );

            // When
            VariableRateInstallmentPlan plan = installmentService.createVariableRateInstallmentPlan(
                    principalAmount, standardTerms, rateAdjustments, LocalDate.now());

            // Then
            assertNotNull(plan);
            assertEquals(3, plan.getRatePeriods().size());
            
            // Verify rate adjustments are applied correctly
            List<RatePeriod> periods = plan.getRatePeriods();
            assertEquals(new BigDecimal("4.5"), periods.get(0).getInterestRate());
            assertEquals(new BigDecimal("5.0"), periods.get(1).getInterestRate());
            assertEquals(new BigDecimal("4.0"), periods.get(2).getInterestRate());
        }

        @Test
        @DisplayName("Should recalculate payments after rate adjustment")
        void shouldRecalculatePaymentsAfterRateAdjustment() {
            // Given
            List<RateAdjustment> rateAdjustments = List.of(
                    RateAdjustment.create(LocalDate.now().plusMonths(12), new BigDecimal("6.0"))
            );

            // When
            VariableRateInstallmentPlan plan = installmentService.createVariableRateInstallmentPlan(
                    principalAmount, standardTerms, rateAdjustments, LocalDate.now());

            // Then
            List<RatePeriod> periods = plan.getRatePeriods();
            assertEquals(2, periods.size());
            
            // First period should have 12 payments at 4.5%
            assertEquals(12, periods.get(0).getPaymentCount());
            assertEquals(new BigDecimal("4.5"), periods.get(0).getInterestRate());
            
            // Second period should have remaining payments at 6.0%
            assertEquals(348, periods.get(1).getPaymentCount());
            assertEquals(new BigDecimal("6.0"), periods.get(1).getInterestRate());
            
            // Second period payment should be higher due to rate increase
            assertTrue(periods.get(1).getPaymentAmount().getAmount()
                    .compareTo(periods.get(0).getPaymentAmount().getAmount()) > 0);
        }
    }

    @Nested
    @DisplayName("Payment Modification Tests")
    class PaymentModificationTests {

        @Test
        @DisplayName("Should handle payment amount modification")
        void shouldHandlePaymentAmountModification() {
            // Given
            InstallmentPlan originalPlan = installmentService.createInstallmentPlan(
                    principalAmount, standardTerms, LocalDate.now());
            
            Money newPaymentAmount = originalPlan.getInstallmentAmount()
                    .multiply(new BigDecimal("1.2")); // 20% increase

            // When
            InstallmentModification modification = InstallmentModification.paymentAmountChange(
                    12, newPaymentAmount, "Customer requested payment increase");
            
            ModifiedInstallmentPlan modifiedPlan = installmentService.modifyInstallmentPlan(
                    originalPlan, modification);

            // Then
            assertNotNull(modifiedPlan);
            assertEquals(1, modifiedPlan.getModifications().size());
            
            // Should result in shorter loan term
            assertTrue(modifiedPlan.getEffectiveTermMonths() < originalPlan.getTermInMonths());
            
            // Should result in lower total interest
            assertTrue(modifiedPlan.getTotalInterestAmount().getAmount()
                    .compareTo(originalPlan.getTotalInterestAmount().getAmount()) < 0);
        }

        @Test
        @DisplayName("Should handle payment deferral")
        void shouldHandlePaymentDeferral() {
            // Given
            InstallmentPlan originalPlan = installmentService.createInstallmentPlan(
                    principalAmount, standardTerms, LocalDate.now());

            // When
            InstallmentModification deferral = InstallmentModification.paymentDeferral(
                    6, 3, "COVID-19 hardship deferral");
            
            ModifiedInstallmentPlan modifiedPlan = installmentService.modifyInstallmentPlan(
                    originalPlan, deferral);

            // Then
            assertNotNull(modifiedPlan);
            assertTrue(modifiedPlan.getEffectiveTermMonths() > originalPlan.getTermInMonths());
            
            // Should have deferred payments
            List<DeferredPayment> deferredPayments = modifiedPlan.getDeferredPayments();
            assertEquals(3, deferredPayments.size());
        }

        @Test
        @DisplayName("Should handle term extension")
        void shouldHandleTermExtension() {
            // Given
            InstallmentPlan originalPlan = installmentService.createInstallmentPlan(
                    principalAmount, standardTerms, LocalDate.now());

            // When
            InstallmentModification extension = InstallmentModification.termExtension(
                    12, 60, "Payment reduction for financial hardship");
            
            ModifiedInstallmentPlan modifiedPlan = installmentService.modifyInstallmentPlan(
                    originalPlan, extension);

            // Then
            assertNotNull(modifiedPlan);
            assertEquals(originalPlan.getTermInMonths() + 60, modifiedPlan.getEffectiveTermMonths());
            
            // Extended term should result in lower payment amount
            assertTrue(modifiedPlan.getModifiedPaymentAmount().getAmount()
                    .compareTo(originalPlan.getInstallmentAmount().getAmount()) < 0);
        }
    }

    @Nested
    @DisplayName("Advanced Installment Features Tests")
    class AdvancedInstallmentFeaturesTests {

        @Test
        @DisplayName("Should calculate balloon payment installments")
        void shouldCalculateBalloonPaymentInstallments() {
            // Given
            Money balloonAmount = Money.of("USD", new BigDecimal("20000.00"));
            BalloonPaymentTerms balloonTerms = BalloonPaymentTerms.builder()
                    .balloonAmount(balloonAmount)
                    .balloonPaymentNumber(360)
                    .regularPaymentBasis(principalAmount.subtract(balloonAmount))
                    .build();

            // When
            BalloonInstallmentPlan plan = installmentService.createBalloonInstallmentPlan(
                    principalAmount, standardTerms, balloonTerms, LocalDate.now());

            // Then
            assertNotNull(plan);
            assertEquals(balloonAmount, plan.getBalloonPayment().getPaymentAmount());
            assertEquals(360, plan.getBalloonPayment().getPaymentNumber());
            
            // Regular payments should be lower than standard loan
            InstallmentPlan standardPlan = installmentService.createInstallmentPlan(
                    principalAmount, standardTerms, LocalDate.now());
            assertTrue(plan.getRegularPaymentAmount().getAmount()
                    .compareTo(standardPlan.getInstallmentAmount().getAmount()) < 0);
        }

        @Test
        @DisplayName("Should calculate graduated payment installments")
        void shouldCalculateGraduatedPaymentInstallments() {
            // Given
            GraduatedPaymentSchedule gradSchedule = GraduatedPaymentSchedule.builder()
                    .initialPaymentPercentage(new BigDecimal("80"))
                    .annualIncrease(new BigDecimal("5"))
                    .graduationPeriodYears(5)
                    .build();

            // When
            GraduatedInstallmentPlan plan = installmentService.createGraduatedInstallmentPlan(
                    principalAmount, standardTerms, gradSchedule, LocalDate.now());

            // Then
            assertNotNull(plan);
            List<PaymentPeriod> periods = plan.getPaymentPeriods();
            assertEquals(6, periods.size()); // 5 graduation years + final period
            
            // Each period should have higher payment than previous
            for (int i = 1; i < periods.size() - 1; i++) {
                assertTrue(periods.get(i).getPaymentAmount().getAmount()
                        .compareTo(periods.get(i-1).getPaymentAmount().getAmount()) > 0);
            }
        }

        @Test
        @DisplayName("Should calculate interest-only period installments")
        void shouldCalculateInterestOnlyPeriodInstallments() {
            // Given
            InterestOnlyPeriod ioTerms = InterestOnlyPeriod.builder()
                    .interestOnlyMonths(60)
                    .principalAmortizationStart(61)
                    .build();

            // When
            InterestOnlyInstallmentPlan plan = installmentService.createInterestOnlyInstallmentPlan(
                    principalAmount, standardTerms, ioTerms, LocalDate.now());

            // Then
            assertNotNull(plan);
            assertEquals(60, plan.getInterestOnlyPayments().size());
            assertEquals(300, plan.getPrincipalAndInterestPayments().size());
            
            Money ioPayment = plan.getInterestOnlyPayments().get(0).getPaymentAmount();
            Money piPayment = plan.getPrincipalAndInterestPayments().get(0).getPaymentAmount();
            
            // P&I payment should be higher than interest-only payment
            assertTrue(piPayment.getAmount().compareTo(ioPayment.getAmount()) > 0);
        }
    }

    @Nested
    @DisplayName("Payment Schedule Generation Tests")
    class PaymentScheduleGenerationTests {

        @Test
        @DisplayName("Should generate accurate payment schedule with dates")
        void shouldGenerateAccuratePaymentScheduleWithDates() {
            // Given
            LocalDate startDate = LocalDate.of(2024, 1, 15);

            // When
            InstallmentSchedule schedule = installmentService.generatePaymentSchedule(
                    principalAmount, standardTerms, startDate);

            // Then
            assertNotNull(schedule);
            assertEquals(360, schedule.getScheduledPayments().size());
            
            // Verify dates are correctly calculated
            ScheduledInstallment firstPayment = schedule.getScheduledPayments().get(0);
            ScheduledInstallment secondPayment = schedule.getScheduledPayments().get(1);
            
            assertEquals(LocalDate.of(2024, 2, 15), firstPayment.getDueDate());
            assertEquals(LocalDate.of(2024, 3, 15), secondPayment.getDueDate());
            
            // Verify payment numbers are sequential
            assertEquals(1, firstPayment.getPaymentNumber());
            assertEquals(2, secondPayment.getPaymentNumber());
        }

        @Test
        @DisplayName("Should handle different start dates correctly")
        void shouldHandleDifferentStartDatesCorrectly() {
            // Given
            LocalDate endOfMonthStart = LocalDate.of(2024, 1, 31);

            // When
            InstallmentSchedule schedule = installmentService.generatePaymentSchedule(
                    principalAmount, standardTerms, endOfMonthStart);

            // Then
            assertNotNull(schedule);
            
            // Should handle month-end dates properly
            ScheduledInstallment firstPayment = schedule.getScheduledPayments().get(0);
            ScheduledInstallment februaryPayment = schedule.getScheduledPayments().get(1);
            
            assertEquals(LocalDate.of(2024, 2, 29), firstPayment.getDueDate()); // Leap year
            assertEquals(LocalDate.of(2024, 3, 31), februaryPayment.getDueDate());
        }

        @Test
        @DisplayName("Should calculate payment schedule with holidays adjustment")
        void shouldCalculatePaymentScheduleWithHolidaysAdjustment() {
            // Given
            LocalDate startDate = LocalDate.of(2024, 1, 1); // New Year's Day
            HolidayAdjustmentRule holidayRule = HolidayAdjustmentRule.NEXT_BUSINESS_DAY;

            // When
            InstallmentSchedule schedule = installmentService.generatePaymentScheduleWithHolidays(
                    principalAmount, standardTerms, startDate, holidayRule);

            // Then
            assertNotNull(schedule);
            
            // Verify holiday adjustments are applied
            ScheduledInstallment firstPayment = schedule.getScheduledPayments().get(0);
            assertTrue(firstPayment.getDueDate().getDayOfWeek().getValue() <= 5); // Weekday
        }
    }

    @Nested
    @DisplayName("Installment Analytics Tests")
    class InstallmentAnalyticsTests {

        @Test
        @DisplayName("Should calculate amortization analytics")
        void shouldCalculateAmortizationAnalytics() {
            // Given
            InstallmentPlan plan = installmentService.createInstallmentPlan(
                    principalAmount, standardTerms, LocalDate.now());

            // When
            AmortizationAnalytics analytics = installmentService.calculateAmortizationAnalytics(plan);

            // Then
            assertNotNull(analytics);
            assertTrue(analytics.getPrincipalPercentageAtYear(5).compareTo(BigDecimal.ZERO) > 0);
            assertTrue(analytics.getInterestPercentageAtYear(5).compareTo(BigDecimal.ZERO) > 0);
            assertEquals(new BigDecimal("100"), 
                    analytics.getPrincipalPercentageAtYear(5).add(analytics.getInterestPercentageAtYear(5)));
        }

        @Test
        @DisplayName("Should calculate early payoff savings")
        void shouldCalculateEarlyPayoffSavings() {
            // Given
            InstallmentPlan plan = installmentService.createInstallmentPlan(
                    principalAmount, standardTerms, LocalDate.now());

            // When
            EarlyPayoffAnalysis analysis = installmentService.calculateEarlyPayoffSavings(
                    plan, 60, LocalDate.now().plusYears(5));

            // Then
            assertNotNull(analysis);
            assertTrue(analysis.getInterestSavings().getAmount().compareTo(BigDecimal.ZERO) > 0);
            assertTrue(analysis.getTimeSavingsMonths() > 0);
            assertNotNull(analysis.getBreakEvenPoint());
        }

        @Test
        @DisplayName("Should compare different installment options")
        void shouldCompareDifferentInstallmentOptions() {
            // Given
            LoanTerms option1 = standardTerms;
            LoanTerms option2 = standardTerms.toBuilder()
                    .termInMonths(180)
                    .interestRate(new BigDecimal("4.25"))
                    .build();

            // When
            InstallmentComparison comparison = installmentService.compareInstallmentOptions(
                    principalAmount, List.of(option1, option2), LocalDate.now());

            // Then
            assertNotNull(comparison);
            assertEquals(2, comparison.getOptions().size());
            assertNotNull(comparison.getRecommendation());
            
            InstallmentOption longerTerm = comparison.getOptions().get(0);
            InstallmentOption shorterTerm = comparison.getOptions().get(1);
            
            // Shorter term should have higher payment but lower total interest
            assertTrue(shorterTerm.getMonthlyPayment().getAmount()
                    .compareTo(longerTerm.getMonthlyPayment().getAmount()) > 0);
            assertTrue(shorterTerm.getTotalInterest().getAmount()
                    .compareTo(longerTerm.getTotalInterest().getAmount()) < 0);
        }
    }
}