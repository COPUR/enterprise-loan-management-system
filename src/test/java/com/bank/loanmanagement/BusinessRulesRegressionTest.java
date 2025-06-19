package com.bank.loanmanagement;

import com.bank.loanmanagement.LoanManagementApplication;
import com.bank.loanmanagement.application.service.LoanService;
import com.bank.loanmanagement.domain.customer.CreditCustomer;
import com.bank.loanmanagement.domain.loan.CreditLoan;
import com.bank.loanmanagement.domain.loan.CreditLoanInstallment;
import com.bank.loanmanagement.infrastructure.repository.CreditCustomerRepository;
import com.bank.loanmanagement.infrastructure.repository.CreditLoanRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.*;

@SpringBootTest(classes = LoanManagementApplication.class)
@ActiveProfiles("test")
@Transactional
@DisplayName("Business Rules Regression Test Suite")
public class BusinessRulesRegressionTest {

    @Autowired
    private LoanService loanService;

    @Autowired
    private CreditCustomerRepository customerRepository;

    @Autowired
    private CreditLoanRepository loanRepository;

    private CreditCustomer testCustomer;

    @BeforeEach
    void setUp() {
        testCustomer = CreditCustomer.builder()
            .name("Test")
            .surname("Customer")
            .creditLimit(BigDecimal.valueOf(100000))
            .usedCreditLimit(BigDecimal.ZERO)
            .build();
        testCustomer = customerRepository.save(testCustomer);
    }

    @Test
    @DisplayName("BR-001: Total amount calculation must be exact (amount * (1 + interest rate))")
    void totalAmountCalculationMustBeExact() {
        // Test various combinations
        Object[][] testCases = {
            {10000, 0.1, 11000},   // 10000 * 1.1 = 11000
            {15000, 0.2, 18000},   // 15000 * 1.2 = 18000
            {25000, 0.35, 33750},  // 25000 * 1.35 = 33750
            {7500, 0.5, 11250},    // 7500 * 1.5 = 11250
            {1000, 0.15, 1150}     // 1000 * 1.15 = 1150
        };

        for (Object[] testCase : testCases) {
            BigDecimal amount = BigDecimal.valueOf((Integer) testCase[0]);
            BigDecimal interestRate = BigDecimal.valueOf((Double) testCase[1]);
            BigDecimal expectedTotal = BigDecimal.valueOf((Integer) testCase[2]);

            CreditLoan loan = loanService.createLoan(testCustomer.getId(), amount, interestRate, 12);
            
            assertThat(loan.getTotalAmount())
                .as("Total amount for loan amount %s with interest rate %s", amount, interestRate)
                .isEqualByComparingTo(expectedTotal);
        }
    }

    @Test
    @DisplayName("BR-002: All installments must have exactly equal amounts")
    void allInstallmentsMustHaveEqualAmounts() {
        // Test with different numbers of installments to ensure equal division
        int[] installmentCounts = {6, 9, 12, 24};
        
        for (int installments : installmentCounts) {
            CreditLoan loan = loanService.createLoan(
                testCustomer.getId(), 
                BigDecimal.valueOf(12000), 
                BigDecimal.valueOf(0.2), 
                installments
            );
            
            BigDecimal expectedInstallmentAmount = loan.getTotalAmount()
                .divide(BigDecimal.valueOf(installments), 2, RoundingMode.HALF_UP);
            
            List<CreditLoanInstallment> installmentList = loan.getInstallments();
            
            for (int i = 0; i < installmentList.size(); i++) {
                assertThat(installmentList.get(i).getAmount())
                    .as("Installment %d amount for %d installments", i + 1, installments)
                    .isEqualByComparingTo(expectedInstallmentAmount);
            }
        }
    }

    @Test
    @DisplayName("BR-003: Due dates must be first day of consecutive months starting next month")
    void dueDatesMustBeFirstDayOfConsecutiveMonths() {
        CreditLoan loan = loanService.createLoan(
            testCustomer.getId(), 
            BigDecimal.valueOf(12000), 
            BigDecimal.valueOf(0.2), 
            6
        );
        
        LocalDate expectedFirstDueDate = LocalDate.now().plusMonths(1).withDayOfMonth(1);
        List<CreditLoanInstallment> installments = loan.getInstallments();
        
        for (int i = 0; i < installments.size(); i++) {
            LocalDate expectedDueDate = expectedFirstDueDate.plusMonths(i);
            LocalDate actualDueDate = installments.get(i).getDueDate();
            
            assertThat(actualDueDate)
                .as("Installment %d due date", i + 1)
                .isEqualTo(expectedDueDate);
            
            assertThat(actualDueDate.getDayOfMonth())
                .as("Installment %d due date day of month", i + 1)
                .isEqualTo(1);
        }
    }

    @Test
    @DisplayName("BR-004: Payment must pay installments wholly or not at all")
    void paymentMustPayInstallmentsWhollyOrNotAtAll() {
        CreditLoan loan = loanService.createLoan(
            testCustomer.getId(), 
            BigDecimal.valueOf(12000), 
            BigDecimal.valueOf(0.2), 
            12
        );
        
        BigDecimal installmentAmount = loan.getInstallmentAmount(); // 1200
        
        // Test case 1: Exact amount for 2 installments
        LoanService.PaymentResult result1 = loanService.payLoan(
            loan.getId(), 
            installmentAmount.multiply(BigDecimal.valueOf(2))
        );
        assertThat(result1.getInstallmentsPaid()).isEqualTo(2);
        
        // Reset loan
        loan = loanService.createLoan(testCustomer.getId(), BigDecimal.valueOf(12000), BigDecimal.valueOf(0.2), 12);
        
        // Test case 2: Amount for 1.7 installments - should only pay 1
        LoanService.PaymentResult result2 = loanService.payLoan(
            loan.getId(), 
            installmentAmount.multiply(BigDecimal.valueOf(1.7))
        );
        assertThat(result2.getInstallmentsPaid()).isEqualTo(1);
        
        // Reset loan
        loan = loanService.createLoan(testCustomer.getId(), BigDecimal.valueOf(12000), BigDecimal.valueOf(0.2), 12);
        
        // Test case 3: Amount for 0.9 installments - should pay 0
        LoanService.PaymentResult result3 = loanService.payLoan(
            loan.getId(), 
            installmentAmount.multiply(BigDecimal.valueOf(0.9))
        );
        assertThat(result3.getInstallmentsPaid()).isEqualTo(0);
    }

    @Test
    @DisplayName("BR-005: Earliest installments must be paid first")
    void earliestInstallmentsMustBePaidFirst() {
        CreditLoan loan = loanService.createLoan(
            testCustomer.getId(), 
            BigDecimal.valueOf(12000), 
            BigDecimal.valueOf(0.2), 
            12
        );
        
        BigDecimal installmentAmount = loan.getInstallmentAmount();
        
        // Pay for 4 installments
        loanService.payLoan(loan.getId(), installmentAmount.multiply(BigDecimal.valueOf(4)));
        
        // Reload loan to get updated state
        loan = loanRepository.findById(loan.getId()).orElseThrow();
        List<CreditLoanInstallment> installments = loan.getInstallments();
        
        // First 4 installments should be paid
        for (int i = 0; i < 4; i++) {
            assertThat(installments.get(i).getIsPaid())
                .as("Installment %d should be paid", i + 1)
                .isTrue();
        }
        
        // Remaining installments should not be paid
        for (int i = 4; i < 12; i++) {
            assertThat(installments.get(i).getIsPaid())
                .as("Installment %d should not be paid", i + 1)
                .isFalse();
        }
    }

    @Test
    @DisplayName("BR-006: Cannot pay installments due more than 3 months in future")
    void cannotPayInstallmentsDueMoreThan3MonthsInFuture() {
        // Create loan with 24 installments (2 years)
        CreditLoan loan = loanService.createLoan(
            testCustomer.getId(), 
            BigDecimal.valueOf(24000), 
            BigDecimal.valueOf(0.2), 
            24
        );
        
        BigDecimal totalLoanAmount = loan.getTotalAmount();
        
        // Try to pay entire loan amount
        LoanService.PaymentResult result = loanService.payLoan(loan.getId(), totalLoanAmount);
        
        // Should only pay installments within 3 months (at most 3 installments)
        assertThat(result.getInstallmentsPaid())
            .as("Should only pay installments within 3 months")
            .isLessThanOrEqualTo(3);
    }

    @Test
    @DisplayName("BR-007: Customer credit limit must be allocated/released correctly")
    void customerCreditLimitMustBeAllocatedReleasedCorrectly() {
        BigDecimal initialUsedCredit = testCustomer.getUsedCreditLimit();
        BigDecimal loanAmount = BigDecimal.valueOf(10000);
        
        // Create loan - should allocate credit
        CreditLoan loan = loanService.createLoan(
            testCustomer.getId(), 
            loanAmount, 
            BigDecimal.valueOf(0.2), 
            12
        );
        
        // Reload customer to verify credit allocation
        testCustomer = customerRepository.findById(testCustomer.getId()).orElseThrow();
        assertThat(testCustomer.getUsedCreditLimit())
            .as("Credit should be allocated for loan amount")
            .isEqualByComparingTo(initialUsedCredit.add(loanAmount));
        
        // Pay some installments - should release proportional credit
        BigDecimal installmentAmount = loan.getInstallmentAmount();
        loanService.payLoan(loan.getId(), installmentAmount.multiply(BigDecimal.valueOf(3)));
        
        // Reload customer to verify credit release
        testCustomer = customerRepository.findById(testCustomer.getId()).orElseThrow();
        BigDecimal expectedReleasedCredit = installmentAmount.multiply(BigDecimal.valueOf(3));
        
        assertThat(testCustomer.getUsedCreditLimit())
            .as("Credit should be released for paid installments")
            .isEqualByComparingTo(loanAmount.subtract(expectedReleasedCredit));
    }

    @Test
    @DisplayName("BR-008: Loan must be marked as paid when all installments are paid")
    void loanMustBeMarkedAsPaidWhenAllInstallmentsArePaid() {
        CreditLoan loan = loanService.createLoan(
            testCustomer.getId(), 
            BigDecimal.valueOf(6000), 
            BigDecimal.valueOf(0.2), 
            6
        );
        
        assertThat(loan.getIsPaid()).isFalse();
        
        // Pay entire loan
        BigDecimal totalAmount = loan.getTotalAmount();
        LoanService.PaymentResult result = loanService.payLoan(loan.getId(), totalAmount);
        
        assertThat(result.isLoanFullyPaid()).isTrue();
        
        // Reload loan to verify status
        loan = loanRepository.findById(loan.getId()).orElseThrow();
        assertThat(loan.getIsPaid()).isTrue();
    }

    @Test
    @DisplayName("BR-009: Interest rate boundaries must be strictly enforced")
    void interestRateBoundariesMustBeStrictlyEnforced() {
        // Test exact boundaries
        assertThatNoException().isThrownBy(() -> 
            loanService.createLoan(testCustomer.getId(), BigDecimal.valueOf(10000), BigDecimal.valueOf(0.1), 12)
        );
        
        assertThatNoException().isThrownBy(() -> 
            loanService.createLoan(testCustomer.getId(), BigDecimal.valueOf(10000), BigDecimal.valueOf(0.5), 12)
        );
        
        // Test just outside boundaries
        assertThatThrownBy(() -> 
            loanService.createLoan(testCustomer.getId(), BigDecimal.valueOf(10000), BigDecimal.valueOf(0.099), 12)
        ).hasMessage("Interest rate must be between 0.1 and 0.5");
        
        assertThatThrownBy(() -> 
            loanService.createLoan(testCustomer.getId(), BigDecimal.valueOf(10000), BigDecimal.valueOf(0.501), 12)
        ).hasMessage("Interest rate must be between 0.1 and 0.5");
    }

    @Test
    @DisplayName("BR-010: Installment number validation must be exact")
    void installmentNumberValidationMustBeExact() {
        int[] validNumbers = {6, 9, 12, 24};
        int[] invalidNumbers = {1, 2, 3, 4, 5, 7, 8, 10, 11, 13, 15, 18, 20, 25, 30, 36, 48};
        
        // Valid numbers should work
        for (int valid : validNumbers) {
            assertThatNoException()
                .as("Valid installment number %d should be accepted", valid)
                .isThrownBy(() -> 
                    loanService.createLoan(testCustomer.getId(), BigDecimal.valueOf(6000), BigDecimal.valueOf(0.2), valid)
                );
        }
        
        // Invalid numbers should be rejected
        for (int invalid : invalidNumbers) {
            assertThatThrownBy(() -> 
                loanService.createLoan(testCustomer.getId(), BigDecimal.valueOf(10000), BigDecimal.valueOf(0.2), invalid)
            )
            .as("Invalid installment number %d should be rejected", invalid)
            .hasMessage("Number of installments can only be 6, 9, 12, or 24");
        }
    }

    @Test
    @DisplayName("BR-011: Bonus Feature - Early payment discount calculation")
    void earlyPaymentDiscountCalculationMustBeAccurate() {
        LocalDate dueDate = LocalDate.of(2024, 3, 1);
        LocalDate paymentDate = LocalDate.of(2024, 2, 20); // 10 days early
        BigDecimal installmentAmount = BigDecimal.valueOf(1000);
        
        CreditLoanInstallment installment = CreditLoanInstallment.builder()
            .amount(installmentAmount)
            .dueDate(dueDate)
            .isPaid(false)
            .build();
        
        BigDecimal discount = installment.calculateRewardOrPenalty(paymentDate);
        BigDecimal effectiveAmount = installment.getEffectiveAmount(paymentDate);
        
        // Expected discount: 1000 * 0.001 * 10 = 10
        assertThat(discount).isEqualByComparingTo(BigDecimal.valueOf(10));
        // Effective amount: 1000 - 10 = 990
        assertThat(effectiveAmount).isEqualByComparingTo(BigDecimal.valueOf(990));
    }

    @Test
    @DisplayName("BR-012: Bonus Feature - Late payment penalty calculation")
    void latePaymentPenaltyCalculationMustBeAccurate() {
        LocalDate dueDate = LocalDate.of(2024, 3, 1);
        LocalDate paymentDate = LocalDate.of(2024, 3, 15); // 14 days late
        BigDecimal installmentAmount = BigDecimal.valueOf(1500);
        
        CreditLoanInstallment installment = CreditLoanInstallment.builder()
            .amount(installmentAmount)
            .dueDate(dueDate)
            .isPaid(false)
            .build();
        
        BigDecimal penalty = installment.calculateRewardOrPenalty(paymentDate);
        BigDecimal effectiveAmount = installment.getEffectiveAmount(paymentDate);
        
        // Expected penalty: -(1500 * 0.001 * 14) = -21
        assertThat(penalty).isEqualByComparingTo(BigDecimal.valueOf(-21));
        // Effective amount: 1500 - (-21) = 1521
        assertThat(effectiveAmount).isEqualByComparingTo(BigDecimal.valueOf(1521));
    }
}