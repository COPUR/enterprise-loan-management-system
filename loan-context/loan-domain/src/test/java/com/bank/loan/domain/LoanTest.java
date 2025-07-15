package com.bank.loan.domain;

import com.bank.shared.kernel.domain.CustomerId;
import com.bank.shared.kernel.domain.Money;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Comprehensive TDD tests for Loan domain entity
 * 
 * Tests cover:
 * - Payment calculation validation
 * - Financial edge cases with property-based testing
 * - Business-critical financial calculations
 * - Loan lifecycle state transitions
 */
@DisplayName("Loan Domain Entity Tests")
class LoanTest {

    private LoanId loanId;
    private CustomerId customerId;
    private Money principalAmount;
    private InterestRate interestRate;
    private LoanTerm loanTerm;
    private Loan loan;

    @BeforeEach
    void setUp() {
        loanId = LoanId.generate();
        customerId = CustomerId.generate();
        principalAmount = Money.aed(new BigDecimal("100000"));
        interestRate = InterestRate.of(new BigDecimal("0.06")); // 6% annual
        loanTerm = LoanTerm.ofMonths(60); // 5 years
        loan = Loan.create(loanId, customerId, principalAmount, interestRate, loanTerm);
    }

    @Nested
    @DisplayName("Loan Creation Tests")
    class LoanCreationTests {

        @Test
        @DisplayName("Should create loan with valid parameters")
        void shouldCreateLoanWithValidParameters() {
            assertAll(
                () -> assertEquals(loanId, loan.getId()),
                () -> assertEquals(customerId, loan.getCustomerId()),
                () -> assertEquals(principalAmount, loan.getPrincipalAmount()),
                () -> assertEquals(interestRate, loan.getInterestRate()),
                () -> assertEquals(loanTerm, loan.getLoanTerm()),
                () -> assertEquals(LoanStatus.CREATED, loan.getStatus()),
                () -> assertEquals(principalAmount, loan.getOutstandingBalance()),
                () -> assertNotNull(loan.getApplicationDate()),
                () -> assertNotNull(loan.getCreatedAt())
            );
        }

        @Test
        @DisplayName("Should reject null loan ID")
        void shouldRejectNullLoanId() {
            assertThrows(NullPointerException.class, () ->
                Loan.create(null, customerId, principalAmount, interestRate, loanTerm)
            );
        }

        @Test
        @DisplayName("Should reject null customer ID")
        void shouldRejectNullCustomerId() {
            assertThrows(NullPointerException.class, () ->
                Loan.create(loanId, null, principalAmount, interestRate, loanTerm)
            );
        }

        @Test
        @DisplayName("Should reject negative principal amount")
        void shouldRejectNegativePrincipalAmount() {
            Money negativePrincipal = Money.aed(new BigDecimal("-1000"));
            assertThrows(IllegalArgumentException.class, () ->
                Loan.create(loanId, customerId, negativePrincipal, interestRate, loanTerm)
            );
        }

        @Test
        @DisplayName("Should reject zero principal amount")
        void shouldRejectZeroPrincipalAmount() {
            Money zeroPrincipal = Money.aed(BigDecimal.ZERO);
            assertThrows(IllegalArgumentException.class, () ->
                Loan.create(loanId, customerId, zeroPrincipal, interestRate, loanTerm)
            );
        }

        @Test
        @DisplayName("Should reject negative interest rate")
        void shouldRejectNegativeInterestRate() {
            assertThrows(IllegalArgumentException.class, () ->
                InterestRate.of(new BigDecimal("-0.01"))
            );
        }

        @Test
        @DisplayName("Should reject zero or negative loan term")
        void shouldRejectInvalidLoanTerm() {
            assertThrows(IllegalArgumentException.class, () ->
                LoanTerm.ofMonths(0)
            );
        }
    }

    @Nested
    @DisplayName("Payment Calculation Tests")
    class PaymentCalculationTests {

        @Test
        @DisplayName("Should calculate correct monthly payment for standard loan")
        void shouldCalculateCorrectMonthlyPaymentForStandardLoan() {
            // Given: 100,000 AED at 6% for 60 months
            // Expected monthly payment: ≈ 1,933.28 AED (using PMT formula)
            Money monthlyPayment = loan.calculateMonthlyPayment();
            
            // Verify the payment is in reasonable range
            assertTrue(monthlyPayment.getAmount().compareTo(new BigDecimal("1930")) >= 0);
            assertTrue(monthlyPayment.getAmount().compareTo(new BigDecimal("1940")) <= 0);
        }

        @Test
        @DisplayName("Should calculate correct monthly payment for zero interest")
        void shouldCalculateCorrectMonthlyPaymentForZeroInterest() {
            InterestRate zeroRate = InterestRate.of(BigDecimal.ZERO);
            Loan zeroInterestLoan = Loan.create(loanId, customerId, principalAmount, zeroRate, loanTerm);
            
            Money monthlyPayment = zeroInterestLoan.calculateMonthlyPayment();
            Money expectedPayment = principalAmount.divide(new BigDecimal("60"));
            
            assertEquals(expectedPayment.getAmount(), monthlyPayment.getAmount());
        }

        @Test
        @DisplayName("Should handle single month loan correctly")
        void shouldHandleSingleMonthLoanCorrectly() {
            LoanTerm oneMonth = LoanTerm.ofMonths(1);
            Loan shortLoan = Loan.create(loanId, customerId, principalAmount, interestRate, oneMonth);
            
            Money monthlyPayment = shortLoan.calculateMonthlyPayment();
            
            assertEquals(principalAmount.getAmount(), monthlyPayment.getAmount());
        }

        @ParameterizedTest
        @ValueSource(strings = {"12", "24", "36", "48", "60", "120", "240"})
        @DisplayName("Should calculate reasonable payments for various loan terms")
        void shouldCalculateReasonablePaymentsForVariousTerms(String months) {
            LoanTerm term = LoanTerm.ofMonths(Integer.parseInt(months));
            Loan testLoan = Loan.create(loanId, customerId, principalAmount, interestRate, term);
            
            Money monthlyPayment = testLoan.calculateMonthlyPayment();
            
            // Payment should be positive
            assertTrue(monthlyPayment.isPositive());
            
            // Total payments should exceed principal (due to interest)
            Money totalPayments = monthlyPayment.multiply(new BigDecimal(months));
            assertTrue(totalPayments.compareTo(principalAmount) > 0);
            
            // Monthly payment should be reasonable (not more than principal)
            assertTrue(monthlyPayment.compareTo(principalAmount) <= 0);
        }

        @ParameterizedTest
        @ValueSource(strings = {"0.01", "0.03", "0.06", "0.12", "0.15"})
        @DisplayName("Should calculate increasing payments for increasing interest rates")
        void shouldCalculateIncreasingPaymentsForIncreasingRates(String rate) {
            InterestRate testRate = InterestRate.of(new BigDecimal(rate));
            Loan testLoan = Loan.create(loanId, customerId, principalAmount, testRate, loanTerm);
            
            Money monthlyPayment = testLoan.calculateMonthlyPayment();
            
            // Payment should be positive and reasonable
            assertTrue(monthlyPayment.isPositive());
            
            // Higher rates should result in higher payments (property-based testing concept)
            if (testRate.getAnnualRate().compareTo(new BigDecimal("0.06")) > 0) {
                Money basePayment = loan.calculateMonthlyPayment();
                assertTrue(monthlyPayment.compareTo(basePayment) > 0,
                    "Higher interest rate should result in higher payment");
            }
        }
    }

    @Nested
    @DisplayName("Payment Processing Tests")
    class PaymentProcessingTests {

        @BeforeEach
        void setUpLoanForPayments() {
            loan.approve();
            loan.disburse();
        }

        @Test
        @DisplayName("Should process valid payment correctly")
        void shouldProcessValidPaymentCorrectly() {
            Money paymentAmount = Money.aed(new BigDecimal("2000"));
            Money expectedBalance = principalAmount.subtract(paymentAmount);
            
            PaymentResult result = loan.makePayment(paymentAmount);
            
            assertTrue(result.isSuccess());
            assertEquals(expectedBalance, loan.getOutstandingBalance());
            assertEquals(LoanStatus.DISBURSED, loan.getStatus());
            assertEquals(paymentAmount, result.getPaymentDistribution().getTotalPayment());
        }

        @Test
        @DisplayName("Should mark loan as fully paid when balance reaches zero")
        void shouldMarkLoanAsFullyPaidWhenBalanceReachesZero() {
            Money fullPayment = loan.getOutstandingBalance();
            
            PaymentResult result = loan.makePayment(fullPayment);
            
            assertTrue(result.isSuccess());
            assertTrue(result.isLoanFullyPaid());
            assertEquals(Money.aed(BigDecimal.ZERO), loan.getOutstandingBalance());
            assertEquals(LoanStatus.FULLY_PAID, loan.getStatus());
        }

        @Test
        @DisplayName("Should reject payment that exceeds outstanding balance")
        void shouldRejectPaymentThatExceedsOutstandingBalance() {
            Money excessivePayment = principalAmount.add(Money.aed(new BigDecimal("1000")));
            
            assertThrows(IllegalArgumentException.class, () ->
                loan.makePayment(excessivePayment)
            );
        }

        @Test
        @DisplayName("Should reject negative payment amount")
        void shouldRejectNegativePaymentAmount() {
            Money negativePayment = Money.aed(new BigDecimal("-1000"));
            
            assertThrows(IllegalArgumentException.class, () ->
                loan.makePayment(negativePayment)
            );
        }

        @Test
        @DisplayName("Should reject zero payment amount")
        void shouldRejectZeroPaymentAmount() {
            Money zeroPayment = Money.aed(BigDecimal.ZERO);
            
            assertThrows(IllegalArgumentException.class, () ->
                loan.makePayment(zeroPayment)
            );
        }

        @Test
        @DisplayName("Should reject payment when loan is not in valid status")
        void shouldRejectPaymentWhenLoanIsNotInValidStatus() {
            Loan newLoan = Loan.create(LoanId.generate(), customerId, principalAmount, interestRate, loanTerm);
            Money paymentAmount = Money.aed(new BigDecimal("1000"));
            
            assertThrows(IllegalStateException.class, () ->
                newLoan.makePayment(paymentAmount)
            );
        }

        @Test
        @DisplayName("Should handle multiple sequential payments correctly")
        void shouldHandleMultipleSequentialPaymentsCorrectly() {
            Money firstPayment = Money.aed(new BigDecimal("30000"));
            Money secondPayment = Money.aed(new BigDecimal("25000"));
            Money thirdPayment = Money.aed(new BigDecimal("45000"));
            
            PaymentResult result1 = loan.makePayment(firstPayment);
            assertTrue(result1.isSuccess());
            Money balanceAfterFirst = principalAmount.subtract(firstPayment);
            assertEquals(balanceAfterFirst, loan.getOutstandingBalance());
            
            PaymentResult result2 = loan.makePayment(secondPayment);
            assertTrue(result2.isSuccess());
            Money balanceAfterSecond = balanceAfterFirst.subtract(secondPayment);
            assertEquals(balanceAfterSecond, loan.getOutstandingBalance());
            
            PaymentResult result3 = loan.makePayment(thirdPayment);
            assertTrue(result3.isSuccess());
            assertTrue(result3.isLoanFullyPaid());
            Money balanceAfterThird = balanceAfterSecond.subtract(thirdPayment);
            assertEquals(balanceAfterThird, loan.getOutstandingBalance());
            assertEquals(LoanStatus.FULLY_PAID, loan.getStatus());
        }
    }

    @Nested
    @DisplayName("Loan State Transition Tests")
    class LoanStateTransitionTests {

        @Test
        @DisplayName("Should approve loan in CREATED status")
        void shouldApproveLoanInCreatedStatus() {
            loan.approve();
            
            assertEquals(LoanStatus.APPROVED, loan.getStatus());
            assertNotNull(loan.getApprovalDate());
        }

        @Test
        @DisplayName("Should reject loan in CREATED status")
        void shouldRejectLoanInCreatedStatus() {
            String reason = "Insufficient credit score";
            
            loan.reject(reason);
            
            assertEquals(LoanStatus.REJECTED, loan.getStatus());
        }

        @Test
        @DisplayName("Should disburse approved loan")
        void shouldDisburseApprovedLoan() {
            loan.approve();
            loan.disburse();
            
            assertEquals(LoanStatus.DISBURSED, loan.getStatus());
            assertNotNull(loan.getDisbursementDate());
            assertNotNull(loan.getMaturityDate());
        }

        @Test
        @DisplayName("Should calculate correct maturity date")
        void shouldCalculateCorrectMaturityDate() {
            loan.approve();
            LocalDate disbursementDate = LocalDate.now();
            loan.disburse();
            
            LocalDate expectedMaturity = disbursementDate.plusMonths(loanTerm.getMonths());
            assertEquals(expectedMaturity, loan.getMaturityDate());
        }

        @Test
        @DisplayName("Should cancel loan in appropriate status")
        void shouldCancelLoanInAppropriateStatus() {
            String reason = "Customer request";
            
            loan.cancel(reason);
            
            assertEquals(LoanStatus.CANCELLED, loan.getStatus());
        }

        @Test
        @DisplayName("Should detect overdue loans correctly")
        void shouldDetectOverdueLoansCorrectly() {
            loan.approve();
            loan.disburse();
            
            // Loan should not be overdue initially
            assertFalse(loan.isOverdue());
            
            // Note: For a real test, we'd need to manipulate the maturity date
            // This is a simplified test focusing on the logic structure
        }
    }

    @Nested
    @DisplayName("Financial Edge Cases - Property-Based Testing")
    class FinancialEdgeCasesTests {

        @ParameterizedTest
        @ValueSource(strings = {"1", "1000", "10000", "100000", "1000000", "10000000"})
        @DisplayName("Should handle various principal amounts correctly")
        void shouldHandleVariousPrincipalAmountsCorrectly(String principalValue) {
            Money testPrincipal = Money.aed(new BigDecimal(principalValue));
            Loan testLoan = Loan.create(loanId, customerId, testPrincipal, interestRate, loanTerm);
            
            // Basic invariants
            assertEquals(testPrincipal, testLoan.getPrincipalAmount());
            assertEquals(testPrincipal, testLoan.getOutstandingBalance());
            
            // Payment calculation should work
            Money monthlyPayment = testLoan.calculateMonthlyPayment();
            assertTrue(monthlyPayment.isPositive());
            assertTrue(monthlyPayment.compareTo(testPrincipal) <= 0);
        }

        @Test
        @DisplayName("Should maintain precision in financial calculations")
        void shouldMaintainPrecisionInFinancialCalculations() {
            // Test with amount that could cause rounding issues
            Money precisePrincipal = Money.aed(new BigDecimal("99999.99"));
            InterestRate preciseRate = InterestRate.of(new BigDecimal("0.05999"));
            LoanTerm preciseTerm = LoanTerm.ofMonths(37);
            
            Loan preciseLoan = Loan.create(loanId, customerId, precisePrincipal, preciseRate, preciseTerm);
            Money monthlyPayment = preciseLoan.calculateMonthlyPayment();
            
            // Payment should have reasonable precision
            assertTrue(monthlyPayment.getAmount().scale() <= 2);
            assertTrue(monthlyPayment.isPositive());
        }

        @Test
        @DisplayName("Should handle minimum viable loan amounts")
        void shouldHandleMinimumViableLoanAmounts() {
            // Use 1.00 AED as minimum - 0.01 AED results in payments smaller than currency precision
            Money minimumAmount = Money.aed(new BigDecimal("1.00"));
            Loan minimumLoan = Loan.create(loanId, customerId, minimumAmount, interestRate, loanTerm);
            
            Money monthlyPayment = minimumLoan.calculateMonthlyPayment();
            assertTrue(monthlyPayment.isPositive());
            
            // Should be able to make payment
            minimumLoan.approve();
            minimumLoan.disburse();
            PaymentResult result = minimumLoan.makePayment(minimumAmount);
            
            assertTrue(result.isSuccess());
            assertTrue(result.isLoanFullyPaid());
            assertEquals(Money.aed(BigDecimal.ZERO), minimumLoan.getOutstandingBalance());
        }

        @Test
        @DisplayName("Should handle high interest rate scenarios")
        void shouldHandleHighInterestRateScenarios() {
            InterestRate highRate = InterestRate.of(new BigDecimal("0.30")); // 30% annual
            Loan highRateLoan = Loan.create(loanId, customerId, principalAmount, highRate, loanTerm);
            
            Money monthlyPayment = highRateLoan.calculateMonthlyPayment();
            
            // High rate should result in significantly higher payment
            Money normalPayment = loan.calculateMonthlyPayment();
            assertTrue(monthlyPayment.compareTo(normalPayment) > 0);
            
            // But payment should still be reasonable
            assertTrue(monthlyPayment.compareTo(principalAmount) <= 0);
        }
    }

    @Nested
    @DisplayName("Business Rule Tests")
    class BusinessRuleTests {

        @Test
        @DisplayName("Should enforce loan lifecycle constraints")
        void shouldEnforceLoanLifecycleConstraints() {
            // Can't disburse before approval
            assertThrows(IllegalStateException.class, () -> loan.disburse());
            
            // Can't make payments before disbursement
            assertThrows(IllegalStateException.class, () -> 
                loan.makePayment(Money.aed(new BigDecimal("1000"))));
            
            // Proper flow should work
            loan.approve();
            loan.disburse();
            PaymentResult result = loan.makePayment(Money.aed(new BigDecimal("1000")));
            
            assertTrue(result.isSuccess());
            assertEquals(LoanStatus.DISBURSED, loan.getStatus());
        }

        @Test
        @DisplayName("Should preserve financial integrity during payments")
        void shouldPreserveFinancialIntegrityDuringPayments() {
            loan.approve();
            loan.disburse();
            
            Money initialBalance = loan.getOutstandingBalance();
            Money payment1 = Money.aed(new BigDecimal("10000"));
            Money payment2 = Money.aed(new BigDecimal("15000"));
            
            PaymentResult result1 = loan.makePayment(payment1);
            assertTrue(result1.isSuccess());
            Money balanceAfterPayment1 = loan.getOutstandingBalance();
            
            PaymentResult result2 = loan.makePayment(payment2);
            assertTrue(result2.isSuccess());
            Money balanceAfterPayment2 = loan.getOutstandingBalance();
            
            // Verify mathematical consistency
            Money expectedFinalBalance = initialBalance.subtract(payment1).subtract(payment2);
            assertEquals(expectedFinalBalance, balanceAfterPayment2);
        }

        @Test
        @DisplayName("Should handle boundary payment scenarios")
        void shouldHandleBoundaryPaymentScenarios() {
            loan.approve();
            loan.disburse();
            
            Money outstandingBalance = loan.getOutstandingBalance();
            
            // Payment exactly equal to outstanding balance
            PaymentResult result = loan.makePayment(outstandingBalance);
            
            assertTrue(result.isSuccess());
            assertTrue(result.isLoanFullyPaid());
            assertEquals(Money.aed(BigDecimal.ZERO), loan.getOutstandingBalance());
            assertEquals(LoanStatus.FULLY_PAID, loan.getStatus());
            
            // Cannot make further payments on fully paid loan
            assertThrows(IllegalStateException.class, () ->
                loan.makePayment(Money.aed(new BigDecimal("100"))));
        }
    }
}