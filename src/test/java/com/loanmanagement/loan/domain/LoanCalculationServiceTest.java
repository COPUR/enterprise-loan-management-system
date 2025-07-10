package com.loanmanagement.loan.domain;

import com.loanmanagement.loan.domain.model.*;
import com.loanmanagement.loan.domain.service.LoanCalculationService;
import com.loanmanagement.shared.domain.Money;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for Loan Calculation Service
 * Tests loan payment calculations, amortization schedules, and interest calculations
 */
@DisplayName("Loan Calculation Service Tests")
class LoanCalculationServiceTest {

    private LoanCalculationService calculationService;
    private Money principalAmount;
    private LoanTerms standardTerms;

    @BeforeEach
    void setUp() {
        calculationService = new LoanCalculationService();
        principalAmount = Money.of("USD", new BigDecimal("100000.00"));
        standardTerms = LoanTerms.builder()
                .termInMonths(360) // 30 years
                .interestRate(new BigDecimal("5.0"))
                .paymentFrequency(PaymentFrequency.MONTHLY)
                .compoundingFrequency(CompoundingFrequency.MONTHLY)
                .build();
    }

    @Nested
    @DisplayName("Monthly Payment Calculation Tests")
    class MonthlyPaymentCalculationTests {

        @Test
        @DisplayName("Should calculate correct monthly payment for standard mortgage")
        void shouldCalculateCorrectMonthlyPaymentForStandardMortgage() {
            // When
            Money monthlyPayment = calculationService.calculateMonthlyPayment(
                    principalAmount, standardTerms);

            // Then
            // Expected payment for $100,000 at 5% for 30 years is approximately $536.82
            BigDecimal expected = new BigDecimal("536.82");
            assertEquals(0, expected.compareTo(monthlyPayment.getAmount().setScale(2, RoundingMode.HALF_UP)));
        }

        @Test
        @DisplayName("Should calculate higher payment for shorter term")
        void shouldCalculateHigherPaymentForShorterTerm() {
            // Given
            LoanTerms shorterTerms = LoanTerms.builder()
                    .termInMonths(180) // 15 years
                    .interestRate(new BigDecimal("5.0"))
                    .paymentFrequency(PaymentFrequency.MONTHLY)
                    .compoundingFrequency(CompoundingFrequency.MONTHLY)
                    .build();

            // When
            Money standardPayment = calculationService.calculateMonthlyPayment(
                    principalAmount, standardTerms);
            Money shorterTermPayment = calculationService.calculateMonthlyPayment(
                    principalAmount, shorterTerms);

            // Then
            assertTrue(shorterTermPayment.getAmount().compareTo(standardPayment.getAmount()) > 0);
        }

        @Test
        @DisplayName("Should calculate higher payment for higher interest rate")
        void shouldCalculateHigherPaymentForHigherInterestRate() {
            // Given
            LoanTerms higherRateTerms = LoanTerms.builder()
                    .termInMonths(360)
                    .interestRate(new BigDecimal("7.0"))
                    .paymentFrequency(PaymentFrequency.MONTHLY)
                    .compoundingFrequency(CompoundingFrequency.MONTHLY)
                    .build();

            // When
            Money standardPayment = calculationService.calculateMonthlyPayment(
                    principalAmount, standardTerms);
            Money higherRatePayment = calculationService.calculateMonthlyPayment(
                    principalAmount, higherRateTerms);

            // Then
            assertTrue(higherRatePayment.getAmount().compareTo(standardPayment.getAmount()) > 0);
        }

        @Test
        @DisplayName("Should handle zero interest rate")
        void shouldHandleZeroInterestRate() {
            // Given
            LoanTerms zeroInterestTerms = LoanTerms.builder()
                    .termInMonths(60)
                    .interestRate(BigDecimal.ZERO)
                    .paymentFrequency(PaymentFrequency.MONTHLY)
                    .compoundingFrequency(CompoundingFrequency.MONTHLY)
                    .build();

            // When
            Money monthlyPayment = calculationService.calculateMonthlyPayment(
                    principalAmount, zeroInterestTerms);

            // Then
            // For zero interest, payment should be principal / term
            BigDecimal expected = principalAmount.getAmount().divide(
                    new BigDecimal("60"), 2, RoundingMode.HALF_UP);
            assertEquals(0, expected.compareTo(monthlyPayment.getAmount().setScale(2, RoundingMode.HALF_UP)));
        }
    }

    @Nested
    @DisplayName("Payment Allocation Tests")
    class PaymentAllocationTests {

        @Test
        @DisplayName("Should allocate payment correctly between principal and interest")
        void shouldAllocatePaymentCorrectlyBetweenPrincipalAndInterest() {
            // Given
            Money currentBalance = principalAmount;
            Money paymentAmount = calculationService.calculateMonthlyPayment(
                    principalAmount, standardTerms);

            // When
            PaymentAllocation allocation = calculationService.allocatePayment(
                    paymentAmount, currentBalance, standardTerms);

            // Then
            assertNotNull(allocation);
            assertTrue(allocation.getInterestAmount().getAmount().compareTo(BigDecimal.ZERO) > 0);
            assertTrue(allocation.getPrincipalAmount().getAmount().compareTo(BigDecimal.ZERO) > 0);
            
            // Total allocation should equal payment amount
            Money totalAllocated = allocation.getPrincipalAmount()
                    .add(allocation.getInterestAmount())
                    .add(allocation.getFeesAmount());
            assertEquals(0, paymentAmount.getAmount().compareTo(totalAllocated.getAmount()));
        }

        @Test
        @DisplayName("Should calculate interest based on current balance")
        void shouldCalculateInterestBasedOnCurrentBalance() {
            // Given
            Money currentBalance = Money.of("USD", new BigDecimal("50000.00"));
            Money paymentAmount = Money.of("USD", new BigDecimal("500.00"));

            // When
            PaymentAllocation allocation = calculationService.allocatePayment(
                    paymentAmount, currentBalance, standardTerms);

            // Then
            // Interest for one month at 5% annual on $50,000 should be approximately $208.33
            BigDecimal expectedMonthlyInterest = currentBalance.getAmount()
                    .multiply(standardTerms.getInterestRate())
                    .divide(new BigDecimal("100"), 10, RoundingMode.HALF_UP)
                    .divide(new BigDecimal("12"), 2, RoundingMode.HALF_UP);
            
            assertEquals(0, expectedMonthlyInterest.compareTo(
                    allocation.getInterestAmount().getAmount().setScale(2, RoundingMode.HALF_UP)));
        }

        @Test
        @DisplayName("Should handle overpayment correctly")
        void shouldHandleOverpaymentCorrectly() {
            // Given
            Money currentBalance = Money.of("USD", new BigDecimal("1000.00"));
            Money largePayment = Money.of("USD", new BigDecimal("2000.00"));

            // When
            PaymentAllocation allocation = calculationService.allocatePayment(
                    largePayment, currentBalance, standardTerms);

            // Then
            // Principal allocation should not exceed current balance
            assertTrue(allocation.getPrincipalAmount().getAmount().compareTo(currentBalance.getAmount()) <= 0);
            
            // Any excess should be tracked
            Money totalDue = currentBalance.add(allocation.getInterestAmount());
            if (largePayment.getAmount().compareTo(totalDue.getAmount()) > 0) {
                assertNotNull(allocation.getExcessAmount());
                assertTrue(allocation.getExcessAmount().getAmount().compareTo(BigDecimal.ZERO) > 0);
            }
        }
    }

    @Nested
    @DisplayName("Amortization Schedule Tests")
    class AmortizationScheduleTests {

        @Test
        @DisplayName("Should generate complete amortization schedule")
        void shouldGenerateCompleteAmortizationSchedule() {
            // When
            AmortizationSchedule schedule = calculationService.generateAmortizationSchedule(
                    principalAmount, standardTerms);

            // Then
            assertNotNull(schedule);
            assertEquals(360, schedule.getPayments().size());
            
            // First payment should have higher interest portion
            ScheduledPayment firstPayment = schedule.getPayments().get(0);
            assertTrue(firstPayment.getInterestAmount().getAmount().compareTo(
                    firstPayment.getPrincipalAmount().getAmount()) > 0);
            
            // Last payment should have higher principal portion
            ScheduledPayment lastPayment = schedule.getPayments().get(359);
            assertTrue(lastPayment.getPrincipalAmount().getAmount().compareTo(
                    lastPayment.getInterestAmount().getAmount()) > 0);
        }

        @Test
        @DisplayName("Should show decreasing balance over time")
        void shouldShowDecreasingBalanceOverTime() {
            // When
            AmortizationSchedule schedule = calculationService.generateAmortizationSchedule(
                    principalAmount, standardTerms);

            // Then
            List<ScheduledPayment> payments = schedule.getPayments();
            
            for (int i = 1; i < payments.size(); i++) {
                Money previousBalance = payments.get(i - 1).getRemainingBalance();
                Money currentBalance = payments.get(i).getRemainingBalance();
                
                assertTrue(currentBalance.getAmount().compareTo(previousBalance.getAmount()) < 0,
                        "Balance should decrease with each payment");
            }
            
            // Final balance should be zero (or very close to zero due to rounding)
            Money finalBalance = payments.get(payments.size() - 1).getRemainingBalance();
            assertTrue(finalBalance.getAmount().abs().compareTo(new BigDecimal("0.01")) < 0);
        }

        @Test
        @DisplayName("Should calculate total interest paid")
        void shouldCalculateTotalInterestPaid() {
            // When
            AmortizationSchedule schedule = calculationService.generateAmortizationSchedule(
                    principalAmount, standardTerms);

            // Then
            Money totalInterest = schedule.getTotalInterest();
            Money totalPayments = schedule.getTotalPayments();
            
            assertTrue(totalInterest.getAmount().compareTo(BigDecimal.ZERO) > 0);
            assertEquals(0, totalPayments.getAmount().compareTo(
                    principalAmount.getAmount().add(totalInterest.getAmount())));
        }
    }

    @Nested
    @DisplayName("Early Payoff Calculation Tests")
    class EarlyPayoffCalculationTests {

        @Test
        @DisplayName("Should calculate payoff amount correctly")
        void shouldCalculatePayoffAmountCorrectly() {
            // Given
            Money currentBalance = Money.of("USD", new BigDecimal("75000.00"));
            LocalDate payoffDate = LocalDate.now().plusDays(15);
            LocalDate lastPaymentDate = LocalDate.now().minusDays(15);

            // When
            PayoffCalculation payoff = calculationService.calculatePayoffAmount(
                    currentBalance, standardTerms, payoffDate, lastPaymentDate);

            // Then
            assertNotNull(payoff);
            assertTrue(payoff.getPayoffAmount().getAmount().compareTo(currentBalance.getAmount()) >= 0);
            assertTrue(payoff.getAccruedInterest().getAmount().compareTo(BigDecimal.ZERO) >= 0);
        }

        @Test
        @DisplayName("Should calculate interest savings from early payoff")
        void shouldCalculateInterestSavingsFromEarlyPayoff() {
            // Given
            Money currentBalance = Money.of("USD", new BigDecimal("50000.00"));
            LocalDate payoffDate = LocalDate.now();
            LocalDate lastPaymentDate = LocalDate.now().minusMonths(1);

            // When
            PayoffCalculation payoff = calculationService.calculatePayoffAmount(
                    currentBalance, standardTerms, payoffDate, lastPaymentDate);
            
            // Calculate remaining payments if loan continues
            int remainingPayments = calculationService.calculateRemainingPayments(
                    currentBalance, standardTerms);
            Money monthlyPayment = calculationService.calculateMonthlyPayment(
                    principalAmount, standardTerms);
            Money totalRemainingPayments = Money.of("USD", 
                    monthlyPayment.getAmount().multiply(new BigDecimal(remainingPayments)));

            // Then
            Money interestSavings = totalRemainingPayments.subtract(payoff.getPayoffAmount());
            assertTrue(interestSavings.getAmount().compareTo(BigDecimal.ZERO) > 0);
            assertEquals(interestSavings, payoff.getInterestSavings());
        }
    }

    @Nested
    @DisplayName("Payment Frequency Tests")
    class PaymentFrequencyTests {

        @Test
        @DisplayName("Should calculate bi-weekly payments correctly")
        void shouldCalculateBiWeeklyPaymentsCorrectly() {
            // Given
            LoanTerms biWeeklyTerms = LoanTerms.builder()
                    .termInMonths(360)
                    .interestRate(new BigDecimal("5.0"))
                    .paymentFrequency(PaymentFrequency.BI_WEEKLY)
                    .compoundingFrequency(CompoundingFrequency.MONTHLY)
                    .build();

            // When
            Money biWeeklyPayment = calculationService.calculatePaymentAmount(
                    principalAmount, biWeeklyTerms);
            Money monthlyPayment = calculationService.calculateMonthlyPayment(
                    principalAmount, standardTerms);

            // Then
            // Bi-weekly payment should be approximately half of monthly payment
            BigDecimal expectedBiWeekly = monthlyPayment.getAmount().divide(
                    new BigDecimal("2"), 2, RoundingMode.HALF_UP);
            BigDecimal actualBiWeekly = biWeeklyPayment.getAmount().setScale(2, RoundingMode.HALF_UP);
            
            // Allow for small variance due to different calculation methods
            BigDecimal difference = expectedBiWeekly.subtract(actualBiWeekly).abs();
            assertTrue(difference.compareTo(new BigDecimal("10.00")) < 0);
        }

        @Test
        @DisplayName("Should handle quarterly payments")
        void shouldHandleQuarterlyPayments() {
            // Given
            LoanTerms quarterlyTerms = LoanTerms.builder()
                    .termInMonths(120) // 10 years
                    .interestRate(new BigDecimal("6.0"))
                    .paymentFrequency(PaymentFrequency.QUARTERLY)
                    .compoundingFrequency(CompoundingFrequency.QUARTERLY)
                    .build();

            // When
            Money quarterlyPayment = calculationService.calculatePaymentAmount(
                    principalAmount, quarterlyTerms);

            // Then
            assertNotNull(quarterlyPayment);
            assertTrue(quarterlyPayment.getAmount().compareTo(BigDecimal.ZERO) > 0);
            
            // Quarterly payment should be higher than monthly payment
            Money monthlyPayment = calculationService.calculateMonthlyPayment(
                    principalAmount, standardTerms);
            assertTrue(quarterlyPayment.getAmount().compareTo(monthlyPayment.getAmount()) > 0);
        }
    }

    @Nested
    @DisplayName("Late Fee Calculation Tests")
    class LateFeeCalculationTests {

        @Test
        @DisplayName("Should calculate late fees correctly")
        void shouldCalculateLateFeeCorrectly() {
            // Given
            Money missedPayment = Money.of("USD", new BigDecimal("1000.00"));
            int daysLate = 15;
            LateFeeStructure feeStructure = LateFeeStructure.builder()
                    .flatFee(Money.of("USD", new BigDecimal("25.00")))
                    .percentageFee(new BigDecimal("5.0"))
                    .gracePeriodDays(10)
                    .maxFee(Money.of("USD", new BigDecimal("100.00")))
                    .build();

            // When
            Money lateFee = calculationService.calculateLateFee(
                    missedPayment, daysLate, feeStructure);

            // Then
            assertNotNull(lateFee);
            assertTrue(lateFee.getAmount().compareTo(BigDecimal.ZERO) > 0);
            // Should be either flat fee or percentage fee, whichever is higher
            Money percentageFee = Money.of("USD", 
                    missedPayment.getAmount().multiply(feeStructure.getPercentageFee())
                            .divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP));
            Money expectedFee = feeStructure.getFlatFee().getAmount().compareTo(percentageFee.getAmount()) > 0 
                    ? feeStructure.getFlatFee() : percentageFee;
            assertEquals(0, expectedFee.getAmount().compareTo(lateFee.getAmount()));
        }

        @Test
        @DisplayName("Should not charge late fee within grace period")
        void shouldNotChargeLateFeeWithinGracePeriod() {
            // Given
            Money missedPayment = Money.of("USD", new BigDecimal("1000.00"));
            int daysLate = 5;
            LateFeeStructure feeStructure = LateFeeStructure.builder()
                    .flatFee(Money.of("USD", new BigDecimal("25.00")))
                    .percentageFee(new BigDecimal("5.0"))
                    .gracePeriodDays(10)
                    .maxFee(Money.of("USD", new BigDecimal("100.00")))
                    .build();

            // When
            Money lateFee = calculationService.calculateLateFee(
                    missedPayment, daysLate, feeStructure);

            // Then
            assertEquals(0, BigDecimal.ZERO.compareTo(lateFee.getAmount()));
        }
    }
}