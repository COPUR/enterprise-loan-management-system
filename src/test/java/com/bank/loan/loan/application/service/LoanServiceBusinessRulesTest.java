package com.bank.loanmanagement.loan.application.service;

import com.bank.loanmanagement.loan.domain.customer.CreditCustomer;
import com.bank.loanmanagement.loan.domain.loan.CreditLoan;
import com.bank.loanmanagement.loan.domain.loan.CreditLoanInstallment;
import com.bank.loanmanagement.loan.infrastructure.repository.CreditCustomerRepository;
import com.bank.loanmanagement.loan.infrastructure.repository.CreditLoanRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Loan Service Business Rules Tests")
class LoanServiceBusinessRulesTest {

    @Mock
    private CreditCustomerRepository customerRepository;
    
    @Mock
    private CreditLoanRepository loanRepository;
    
    @InjectMocks
    private LoanService loanService;
    
    private CreditCustomer testCustomer;
    
    @BeforeEach
    void setUp() {
        testCustomer = CreditCustomer.builder()
            .id(1L)
            .name("John")
            .surname("Doe")
            .creditLimit(BigDecimal.valueOf(100000))
            .usedCreditLimit(BigDecimal.ZERO)
            .build();
    }

    @Nested
    @DisplayName("Create Loan Business Rules")
    class CreateLoanBusinessRules {

        @Test
        @DisplayName("Should create loan successfully with valid parameters")
        void shouldCreateLoanSuccessfully() {
            // Given
            Long customerId = 1L;
            BigDecimal amount = BigDecimal.valueOf(10000);
            BigDecimal interestRate = BigDecimal.valueOf(0.2);
            Integer numberOfInstallments = 12;
            
            when(customerRepository.findById(customerId)).thenReturn(Optional.of(testCustomer));
            when(customerRepository.save(any(CreditCustomer.class))).thenReturn(testCustomer);
            when(loanRepository.save(any(CreditLoan.class))).thenAnswer(invocation -> {
                CreditLoan loan = invocation.getArgument(0);
                loan.setId(1L);
                return loan;
            });

            // When
            CreditLoan result = loanService.createLoan(customerId, amount, interestRate, numberOfInstallments);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getCustomerId()).isEqualTo(customerId);
            assertThat(result.getLoanAmount()).isEqualTo(amount);
            assertThat(result.getInterestRate()).isEqualTo(interestRate);
            assertThat(result.getNumberOfInstallments()).isEqualTo(numberOfInstallments);
            assertThat(result.getTotalAmount()).isEqualTo(amount.multiply(BigDecimal.ONE.add(interestRate)));
            assertThat(result.getInstallments()).hasSize(numberOfInstallments);
            
            // Verify customer credit limit is allocated
            verify(customerRepository).save(argThat(customer -> 
                customer.getUsedCreditLimit().equals(amount)));
        }

        @Test
        @DisplayName("Should reject loan when customer has insufficient credit limit")
        void shouldRejectLoanWhenInsufficientCreditLimit() {
            // Given
            testCustomer.setUsedCreditLimit(BigDecimal.valueOf(95000)); // Only 5000 available
            Long customerId = 1L;
            BigDecimal amount = BigDecimal.valueOf(10000); // Requesting more than available
            BigDecimal interestRate = BigDecimal.valueOf(0.2);
            Integer numberOfInstallments = 12;
            
            when(customerRepository.findById(customerId)).thenReturn(Optional.of(testCustomer));

            // When & Then
            assertThatThrownBy(() -> loanService.createLoan(customerId, amount, interestRate, numberOfInstallments))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Customer does not have enough credit limit");
        }

        @Test
        @DisplayName("Should reject loan with invalid number of installments")
        void shouldRejectLoanWithInvalidInstallments() {
            // Given
            Long customerId = 1L;
            BigDecimal amount = BigDecimal.valueOf(10000);
            BigDecimal interestRate = BigDecimal.valueOf(0.2);
            
            // Test each invalid installment number
            int[] invalidInstallments = {1, 3, 5, 7, 8, 10, 11, 13, 18, 36, 48};
            
            for (int installments : invalidInstallments) {
                // When & Then
                assertThatThrownBy(() -> loanService.createLoan(customerId, amount, interestRate, installments))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("Number of installments can only be 6, 9, 12, or 24");
            }
        }

        @Test
        @DisplayName("Should accept loan with valid number of installments")
        void shouldAcceptLoanWithValidInstallments() {
            // Given
            Long customerId = 1L;
            BigDecimal amount = BigDecimal.valueOf(10000);
            BigDecimal interestRate = BigDecimal.valueOf(0.2);
            int[] validInstallments = {6, 9, 12, 24};
            
            when(customerRepository.findById(customerId)).thenReturn(Optional.of(testCustomer));
            when(customerRepository.save(any(CreditCustomer.class))).thenReturn(testCustomer);
            when(loanRepository.save(any(CreditLoan.class))).thenAnswer(invocation -> {
                CreditLoan loan = invocation.getArgument(0);
                loan.setId(1L);
                return loan;
            });
            
            for (int installments : validInstallments) {
                // When
                CreditLoan result = loanService.createLoan(customerId, amount, interestRate, installments);
                
                // Then
                assertThat(result.getNumberOfInstallments()).isEqualTo(installments);
            }
        }

        @Test
        @DisplayName("Should reject loan with interest rate below minimum (0.1)")
        void shouldRejectLoanWithInterestRateBelowMinimum() {
            // Given
            Long customerId = 1L;
            BigDecimal amount = BigDecimal.valueOf(10000);
            BigDecimal interestRate = BigDecimal.valueOf(0.05); // Below minimum
            Integer numberOfInstallments = 12;

            // When & Then
            assertThatThrownBy(() -> loanService.createLoan(customerId, amount, interestRate, numberOfInstallments))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Interest rate must be between 0.1 and 0.5");
        }

        @Test
        @DisplayName("Should reject loan with interest rate above maximum (0.5)")
        void shouldRejectLoanWithInterestRateAboveMaximum() {
            // Given
            Long customerId = 1L;
            BigDecimal amount = BigDecimal.valueOf(10000);
            BigDecimal interestRate = BigDecimal.valueOf(0.6); // Above maximum
            Integer numberOfInstallments = 12;

            // When & Then
            assertThatThrownBy(() -> loanService.createLoan(customerId, amount, interestRate, numberOfInstallments))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Interest rate must be between 0.1 and 0.5");
        }

        @Test
        @DisplayName("Should accept loan with interest rate at boundaries")
        void shouldAcceptLoanWithInterestRateAtBoundaries() {
            // Given
            Long customerId = 1L;
            BigDecimal amount = BigDecimal.valueOf(10000);
            Integer numberOfInstallments = 12;
            
            when(customerRepository.findById(customerId)).thenReturn(Optional.of(testCustomer));
            when(customerRepository.save(any(CreditCustomer.class))).thenReturn(testCustomer);
            when(loanRepository.save(any(CreditLoan.class))).thenAnswer(invocation -> {
                CreditLoan loan = invocation.getArgument(0);
                loan.setId(1L);
                return loan;
            });

            // Test minimum rate
            CreditLoan result1 = loanService.createLoan(customerId, amount, BigDecimal.valueOf(0.1), numberOfInstallments);
            assertThat(result1.getInterestRate()).isEqualTo(BigDecimal.valueOf(0.1));

            // Reset customer for second test
            testCustomer.setUsedCreditLimit(BigDecimal.ZERO);
            
            // Test maximum rate
            CreditLoan result2 = loanService.createLoan(customerId, amount, BigDecimal.valueOf(0.5), numberOfInstallments);
            assertThat(result2.getInterestRate()).isEqualTo(BigDecimal.valueOf(0.5));
        }

        @Test
        @DisplayName("Should calculate total amount correctly (amount * (1 + interest rate))")
        void shouldCalculateTotalAmountCorrectly() {
            // Given
            Long customerId = 1L;
            BigDecimal amount = BigDecimal.valueOf(10000);
            BigDecimal interestRate = BigDecimal.valueOf(0.2); // 20%
            Integer numberOfInstallments = 12;
            
            when(customerRepository.findById(customerId)).thenReturn(Optional.of(testCustomer));
            when(customerRepository.save(any(CreditCustomer.class))).thenReturn(testCustomer);
            when(loanRepository.save(any(CreditLoan.class))).thenAnswer(invocation -> {
                CreditLoan loan = invocation.getArgument(0);
                loan.setId(1L);
                return loan;
            });

            // When
            CreditLoan result = loanService.createLoan(customerId, amount, interestRate, numberOfInstallments);

            // Then
            BigDecimal expectedTotal = amount.multiply(BigDecimal.ONE.add(interestRate)); // 10000 * 1.2 = 12000
            assertThat(result.getTotalAmount()).isEqualTo(expectedTotal);
        }

        @Test
        @DisplayName("Should create installments with equal amounts")
        void shouldCreateInstallmentsWithEqualAmounts() {
            // Given
            Long customerId = 1L;
            BigDecimal amount = BigDecimal.valueOf(12000);
            BigDecimal interestRate = BigDecimal.valueOf(0.2);
            Integer numberOfInstallments = 12;
            
            when(customerRepository.findById(customerId)).thenReturn(Optional.of(testCustomer));
            when(customerRepository.save(any(CreditCustomer.class))).thenReturn(testCustomer);
            when(loanRepository.save(any(CreditLoan.class))).thenAnswer(invocation -> {
                CreditLoan loan = invocation.getArgument(0);
                loan.setId(1L);
                return loan;
            });

            // When
            CreditLoan result = loanService.createLoan(customerId, amount, interestRate, numberOfInstallments);

            // Then
            BigDecimal expectedInstallmentAmount = result.getTotalAmount().divide(BigDecimal.valueOf(numberOfInstallments), 2, BigDecimal.ROUND_HALF_UP);
            
            assertThat(result.getInstallments()).hasSize(numberOfInstallments);
            result.getInstallments().forEach(installment -> {
                assertThat(installment.getAmount()).isEqualTo(expectedInstallmentAmount);
                assertThat(installment.getIsPaid()).isFalse();
                assertThat(installment.getPaidAmount()).isEqualTo(BigDecimal.ZERO);
            });
        }

        @Test
        @DisplayName("Should set due dates to first day of each month starting next month")
        void shouldSetDueDatesCorrectly() {
            // Given
            Long customerId = 1L;
            BigDecimal amount = BigDecimal.valueOf(10000);
            BigDecimal interestRate = BigDecimal.valueOf(0.2);
            Integer numberOfInstallments = 6;
            
            when(customerRepository.findById(customerId)).thenReturn(Optional.of(testCustomer));
            when(customerRepository.save(any(CreditCustomer.class))).thenReturn(testCustomer);
            when(loanRepository.save(any(CreditLoan.class))).thenAnswer(invocation -> {
                CreditLoan loan = invocation.getArgument(0);
                loan.setId(1L);
                return loan;
            });

            // When
            CreditLoan result = loanService.createLoan(customerId, amount, interestRate, numberOfInstallments);

            // Then
            LocalDate expectedFirstDueDate = LocalDate.now().plusMonths(1).withDayOfMonth(1);
            List<CreditLoanInstallment> installments = result.getInstallments();
            
            for (int i = 0; i < numberOfInstallments; i++) {
                LocalDate expectedDueDate = expectedFirstDueDate.plusMonths(i);
                assertThat(installments.get(i).getDueDate()).isEqualTo(expectedDueDate);
                assertThat(installments.get(i).getDueDate().getDayOfMonth()).isEqualTo(1);
            }
        }
    }

    @Nested
    @DisplayName("Payment Business Rules")
    class PaymentBusinessRules {

        private CreditLoan createTestLoan() {
            CreditLoan loan = CreditLoan.builder()
                .id(1L)
                .customerId(1L)
                .loanAmount(BigDecimal.valueOf(12000))
                .numberOfInstallments(12)
                .interestRate(BigDecimal.valueOf(0.2))
                .createDate(LocalDate.now())
                .isPaid(false)
                .build();
            loan.generateInstallments();
            return loan;
        }

        @Test
        @DisplayName("Should pay installments wholly or not at all")
        void shouldPayInstallmentsWhollyOrNotAtAll() {
            // Given
            CreditLoan testLoan = createTestLoan();
            BigDecimal installmentAmount = testLoan.getInstallmentAmount(); // Should be 1200
            
            when(loanRepository.findById(1L)).thenReturn(Optional.of(testLoan));
            when(customerRepository.findById(1L)).thenReturn(Optional.of(testCustomer));
            when(loanRepository.save(any(CreditLoan.class))).thenReturn(testLoan);
            when(customerRepository.save(any(CreditCustomer.class))).thenReturn(testCustomer);

            // Test 1: Payment amount = 2 * installment amount -> should pay 2 installments
            LoanService.PaymentResult result1 = loanService.payLoan(1L, installmentAmount.multiply(BigDecimal.valueOf(2)));
            assertThat(result1.getInstallmentsPaid()).isEqualTo(2);

            // Reset loan for next test
            testLoan = createTestLoan();
            when(loanRepository.findById(1L)).thenReturn(Optional.of(testLoan));

            // Test 2: Payment amount = 1.5 * installment amount -> should pay only 1 installment
            LoanService.PaymentResult result2 = loanService.payLoan(1L, installmentAmount.multiply(BigDecimal.valueOf(1.5)));
            assertThat(result2.getInstallmentsPaid()).isEqualTo(1);

            // Reset loan for next test
            testLoan = createTestLoan();
            when(loanRepository.findById(1L)).thenReturn(Optional.of(testLoan));

            // Test 3: Payment amount = 0.5 * installment amount -> should pay 0 installments
            LoanService.PaymentResult result3 = loanService.payLoan(1L, installmentAmount.multiply(BigDecimal.valueOf(0.5)));
            assertThat(result3.getInstallmentsPaid()).isEqualTo(0);
        }

        @Test
        @DisplayName("Should pay earliest installments first")
        void shouldPayEarliestInstallmentsFirst() {
            // Given
            CreditLoan testLoan = createTestLoan();
            BigDecimal installmentAmount = testLoan.getInstallmentAmount();
            
            when(loanRepository.findById(1L)).thenReturn(Optional.of(testLoan));
            when(customerRepository.findById(1L)).thenReturn(Optional.of(testCustomer));
            when(loanRepository.save(any(CreditLoan.class))).thenReturn(testLoan);
            when(customerRepository.save(any(CreditCustomer.class))).thenReturn(testCustomer);

            // When - Pay for 3 installments
            LoanService.PaymentResult result = loanService.payLoan(1L, installmentAmount.multiply(BigDecimal.valueOf(3)));

            // Then - First 3 installments should be paid
            assertThat(result.getInstallmentsPaid()).isEqualTo(3);
            
            List<CreditLoanInstallment> installments = testLoan.getInstallments();
            assertThat(installments.get(0).getIsPaid()).isTrue();
            assertThat(installments.get(1).getIsPaid()).isTrue();
            assertThat(installments.get(2).getIsPaid()).isTrue();
            assertThat(installments.get(3).getIsPaid()).isFalse();
        }

        @Test
        @DisplayName("Should not allow payment of installments due more than 3 months in future")
        void shouldNotAllowPaymentOfInstallmentsDueMoreThan3MonthsInFuture() {
            // Given - Create loan with installments starting from today + 1 month
            LocalDate today = LocalDate.now();
            CreditLoan testLoan = CreditLoan.builder()
                .id(1L)
                .customerId(1L)
                .loanAmount(BigDecimal.valueOf(12000))
                .numberOfInstallments(12)
                .interestRate(BigDecimal.valueOf(0.2))
                .createDate(today) // Created today
                .isPaid(false)
                .build();
            testLoan.generateInstallments();
            
            // The installments will be due on the 1st of next month, next+1 month, etc.
            // Only the first 3 should be payable (within 3 months from now)
            
            when(loanRepository.findById(1L)).thenReturn(Optional.of(testLoan));
            when(customerRepository.findById(1L)).thenReturn(Optional.of(testCustomer));
            when(loanRepository.save(any(CreditLoan.class))).thenReturn(testLoan);
            when(customerRepository.save(any(CreditCustomer.class))).thenReturn(testCustomer);

            // When - Try to pay all 12 installments (but should only pay first 3)
            BigDecimal paymentAmount = testLoan.getTotalAmount(); // Pay entire loan amount
            LoanService.PaymentResult result = loanService.payLoan(1L, paymentAmount);

            // Then - Should only pay installments within 3 months
            assertThat(result.getInstallmentsPaid()).isLessThanOrEqualTo(3);
        }

        @Test
        @DisplayName("Should return correct payment result information")
        void shouldReturnCorrectPaymentResultInformation() {
            // Given
            CreditLoan testLoan = createTestLoan();
            BigDecimal installmentAmount = testLoan.getInstallmentAmount();
            
            when(loanRepository.findById(1L)).thenReturn(Optional.of(testLoan));
            when(customerRepository.findById(1L)).thenReturn(Optional.of(testCustomer));
            when(loanRepository.save(any(CreditLoan.class))).thenReturn(testLoan);
            when(customerRepository.save(any(CreditCustomer.class))).thenReturn(testCustomer);

            // When - Pay for 2 installments
            LoanService.PaymentResult result = loanService.payLoan(1L, installmentAmount.multiply(BigDecimal.valueOf(2)));

            // Then
            assertThat(result.getInstallmentsPaid()).isEqualTo(2);
            // The total amount spent should be the effective amount (may include rewards/penalties)
            assertThat(result.getTotalAmountSpent()).isGreaterThan(BigDecimal.ZERO);
            assertThat(result.isLoanFullyPaid()).isFalse();
        }

        @Test
        @DisplayName("Should mark loan as fully paid when all installments are paid")
        void shouldMarkLoanAsFullyPaidWhenAllInstallmentsArePaid() {
            // Given - Create a loan with only 3 installments so all can be paid at once
            CreditLoan testLoan = CreditLoan.builder()
                .id(1L)
                .customerId(1L)
                .loanAmount(BigDecimal.valueOf(3000)) // Smaller loan
                .numberOfInstallments(3) // Only 3 installments - all within 3 months
                .interestRate(BigDecimal.valueOf(0.2))
                .createDate(LocalDate.now())
                .isPaid(false)
                .build();
            testLoan.generateInstallments();
            BigDecimal totalAmount = testLoan.getTotalAmount();
            
            when(loanRepository.findById(1L)).thenReturn(Optional.of(testLoan));
            when(customerRepository.findById(1L)).thenReturn(Optional.of(testCustomer));
            when(loanRepository.save(any(CreditLoan.class))).thenReturn(testLoan);
            when(customerRepository.save(any(CreditCustomer.class))).thenReturn(testCustomer);

            // When - Pay entire loan
            LoanService.PaymentResult result = loanService.payLoan(1L, totalAmount);

            // Then - All installments should be paid, so loan should be fully paid
            assertThat(result.getInstallmentsPaid()).isEqualTo(3);
            assertThat(result.isLoanFullyPaid()).isTrue();
            assertThat(testLoan.getIsPaid()).isTrue();
        }

        @Test
        @DisplayName("Should update customer credit limit when installments are paid")
        void shouldUpdateCustomerCreditLimitWhenInstallmentsArePaid() {
            // Given
            testCustomer.setUsedCreditLimit(BigDecimal.valueOf(12000)); // Full loan amount used
            CreditLoan testLoan = createTestLoan();
            BigDecimal installmentAmount = testLoan.getInstallmentAmount();
            
            when(loanRepository.findById(1L)).thenReturn(Optional.of(testLoan));
            when(customerRepository.findById(1L)).thenReturn(Optional.of(testCustomer));
            when(loanRepository.save(any(CreditLoan.class))).thenReturn(testLoan);
            when(customerRepository.save(any(CreditCustomer.class))).thenReturn(testCustomer);

            // When - Pay 1 installment
            loanService.payLoan(1L, installmentAmount);

            // Then - Credit should be released for the paid installment
            verify(customerRepository).save(argThat(customer -> 
                customer.getUsedCreditLimit().compareTo(BigDecimal.valueOf(11000)) == 0)); // 12000 - 1000 (installment principal)
        }
    }

    @Nested
    @DisplayName("Bonus Features - Reward and Penalty Tests")
    class RewardAndPenaltyTests {

        @Test
        @DisplayName("Should apply discount for early payment")
        void shouldApplyDiscountForEarlyPayment() {
            // Given
            LocalDate dueDate = LocalDate.of(2024, 2, 1);
            LocalDate earlyPaymentDate = LocalDate.of(2024, 1, 25); // 7 days early
            BigDecimal installmentAmount = BigDecimal.valueOf(1000);
            
            CreditLoanInstallment installment = CreditLoanInstallment.builder()
                .amount(installmentAmount)
                .dueDate(dueDate)
                .isPaid(false)
                .build();

            // When
            BigDecimal rewardOrPenalty = installment.calculateRewardOrPenalty(earlyPaymentDate);
            BigDecimal effectiveAmount = installment.getEffectiveAmount(earlyPaymentDate);

            // Then
            BigDecimal expectedReward = installmentAmount.multiply(BigDecimal.valueOf(0.001)).multiply(BigDecimal.valueOf(7)); // 1000 * 0.001 * 7 = 7
            assertThat(rewardOrPenalty).isEqualTo(expectedReward);
            assertThat(effectiveAmount).isEqualTo(installmentAmount.subtract(expectedReward)); // 1000 - 7 = 993
        }

        @Test
        @DisplayName("Should apply penalty for late payment")
        void shouldApplyPenaltyForLatePayment() {
            // Given
            LocalDate dueDate = LocalDate.of(2024, 2, 1);
            LocalDate latePaymentDate = LocalDate.of(2024, 2, 8); // 7 days late
            BigDecimal installmentAmount = BigDecimal.valueOf(1000);
            
            CreditLoanInstallment installment = CreditLoanInstallment.builder()
                .amount(installmentAmount)
                .dueDate(dueDate)
                .isPaid(false)
                .build();

            // When
            BigDecimal rewardOrPenalty = installment.calculateRewardOrPenalty(latePaymentDate);
            BigDecimal effectiveAmount = installment.getEffectiveAmount(latePaymentDate);

            // Then
            BigDecimal expectedPenalty = installmentAmount.multiply(BigDecimal.valueOf(0.001)).multiply(BigDecimal.valueOf(7)).negate(); // -(1000 * 0.001 * 7) = -7
            assertThat(rewardOrPenalty).isEqualTo(expectedPenalty);
            assertThat(effectiveAmount).isEqualTo(installmentAmount.subtract(expectedPenalty)); // 1000 - (-7) = 1007
        }

        @Test
        @DisplayName("Should not apply reward or penalty for payment on due date")
        void shouldNotApplyRewardOrPenaltyForPaymentOnDueDate() {
            // Given
            LocalDate dueDate = LocalDate.of(2024, 2, 1);
            BigDecimal installmentAmount = BigDecimal.valueOf(1000);
            
            CreditLoanInstallment installment = CreditLoanInstallment.builder()
                .amount(installmentAmount)
                .dueDate(dueDate)
                .isPaid(false)
                .build();

            // When
            BigDecimal rewardOrPenalty = installment.calculateRewardOrPenalty(dueDate);
            BigDecimal effectiveAmount = installment.getEffectiveAmount(dueDate);

            // Then
            assertThat(rewardOrPenalty).isEqualTo(BigDecimal.ZERO);
            assertThat(effectiveAmount).isEqualTo(installmentAmount);
        }
    }
}