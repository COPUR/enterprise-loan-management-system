package com.bank.infrastructure.financial;

import com.bank.infrastructure.financial.LoanCalculationService.PaymentDistribution;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Nested;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.*;

/**
 * TDD Test Suite for Loan Calculation Service
 * 
 * Tests the critical business logic preserved from archive code review:
 * - Standard loan payment calculations
 * - Credit score based interest rates
 * - Payment distributions
 * - Late payment penalties
 * - Outstanding balance calculations
 */
@DisplayName("Loan Calculation Service TDD Tests")
class LoanCalculationServiceTest {
    
    private LoanCalculationService loanCalculationService;
    
    @BeforeEach
    void setUp() {
        loanCalculationService = new LoanCalculationService();
    }
    
    @Nested
    @DisplayName("Monthly Payment Calculation Tests")
    class MonthlyPaymentCalculationTests {
        
        @Test
        @DisplayName("Should calculate monthly payment correctly for standard loan")
        void shouldCalculateMonthlyPaymentForStandardLoan() {
            // Given
            BigDecimal loanAmount = new BigDecimal("100000.00");
            BigDecimal annualInterestRate = new BigDecimal("0.12"); // 12% annual
            int numberOfInstallments = 36; // 3 years
            
            // When
            BigDecimal monthlyPayment = loanCalculationService.calculateMonthlyPayment(
                loanAmount, annualInterestRate, numberOfInstallments);
            
            // Then
            assertThat(monthlyPayment).isNotNull();
            assertThat(monthlyPayment).isEqualByComparingTo(new BigDecimal("3321.43"));
        }
        
        @Test
        @DisplayName("Should handle minimum interest rate correctly")
        void shouldHandleMinimumInterestRate() {
            // Given
            BigDecimal loanAmount = new BigDecimal("12000.00");
            BigDecimal annualInterestRate = new BigDecimal("0.05"); // 5% minimum
            int numberOfInstallments = 12;
            
            // When
            BigDecimal monthlyPayment = loanCalculationService.calculateMonthlyPayment(
                loanAmount, annualInterestRate, numberOfInstallments);
            
            // Then
            assertThat(monthlyPayment).isNotNull();
            assertThat(monthlyPayment.compareTo(BigDecimal.ZERO)).isGreaterThan(0);
        }
        
        @Test
        @DisplayName("Should validate loan amount within business rules")
        void shouldValidateLoanAmountWithinBusinessRules() {
            // Given
            BigDecimal invalidLowAmount = new BigDecimal("500.00"); // Below $1,000 minimum
            BigDecimal invalidHighAmount = new BigDecimal("600000.00"); // Above $500,000 maximum
            BigDecimal validRate = new BigDecimal("0.12");
            int validInstallments = 24;
            
            // When & Then
            assertThatThrownBy(() -> loanCalculationService.calculateMonthlyPayment(
                invalidLowAmount, validRate, validInstallments))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Loan amount must be between $1,000 and $500,000");
            
            assertThatThrownBy(() -> loanCalculationService.calculateMonthlyPayment(
                invalidHighAmount, validRate, validInstallments))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Loan amount must be between $1,000 and $500,000");
        }
        
        @Test
        @DisplayName("Should validate interest rate within business rules")
        void shouldValidateInterestRateWithinBusinessRules() {
            // Given
            BigDecimal validAmount = new BigDecimal("50000.00");
            BigDecimal invalidLowRate = new BigDecimal("0.02"); // Below 5% minimum
            BigDecimal invalidHighRate = new BigDecimal("0.35"); // Above 30% maximum
            int validInstallments = 24;
            
            // When & Then
            assertThatThrownBy(() -> loanCalculationService.calculateMonthlyPayment(
                validAmount, invalidLowRate, validInstallments))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Interest rate must be between 5% and 30%");
            
            assertThatThrownBy(() -> loanCalculationService.calculateMonthlyPayment(
                validAmount, invalidHighRate, validInstallments))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Interest rate must be between 5% and 30%");
        }
        
        @Test
        @DisplayName("Should validate installments within business rules")
        void shouldValidateInstallmentsWithinBusinessRules() {
            // Given
            BigDecimal validAmount = new BigDecimal("50000.00");
            BigDecimal validRate = new BigDecimal("0.12");
            int invalidLowInstallments = 3; // Below 6 months minimum
            int invalidHighInstallments = 72; // Above 60 months maximum
            
            // When & Then
            assertThatThrownBy(() -> loanCalculationService.calculateMonthlyPayment(
                validAmount, validRate, invalidLowInstallments))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Number of installments must be between 6 and 60 months");
            
            assertThatThrownBy(() -> loanCalculationService.calculateMonthlyPayment(
                validAmount, validRate, invalidHighInstallments))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Number of installments must be between 6 and 60 months");
        }
    }
    
    @Nested
    @DisplayName("Personalized Interest Rate Tests")
    class PersonalizedInterestRateTests {
        
        @Test
        @DisplayName("Should calculate 8% rate for excellent credit score (750+)")
        void shouldCalculateExcellentCreditRate() {
            // Given
            int excellentCreditScore = 780;
            
            // When
            BigDecimal rate = loanCalculationService.calculatePersonalizedRate(excellentCreditScore);
            
            // Then
            assertThat(rate).isEqualByComparingTo(new BigDecimal("0.08"));
        }
        
        @Test
        @DisplayName("Should calculate 12% rate for very good credit score (700-749)")
        void shouldCalculateVeryGoodCreditRate() {
            // Given
            int veryGoodCreditScore = 725;
            
            // When
            BigDecimal rate = loanCalculationService.calculatePersonalizedRate(veryGoodCreditScore);
            
            // Then
            assertThat(rate).isEqualByComparingTo(new BigDecimal("0.12"));
        }
        
        @Test
        @DisplayName("Should calculate 16% rate for good credit score (650-699)")
        void shouldCalculateGoodCreditRate() {
            // Given
            int goodCreditScore = 675;
            
            // When
            BigDecimal rate = loanCalculationService.calculatePersonalizedRate(goodCreditScore);
            
            // Then
            assertThat(rate).isEqualByComparingTo(new BigDecimal("0.16"));
        }
        
        @Test
        @DisplayName("Should calculate 20% rate for fair credit score (<650)")
        void shouldCalculateFairCreditRate() {
            // Given
            int fairCreditScore = 600;
            
            // When
            BigDecimal rate = loanCalculationService.calculatePersonalizedRate(fairCreditScore);
            
            // Then
            assertThat(rate).isEqualByComparingTo(new BigDecimal("0.20"));
        }
        
        @Test
        @DisplayName("Should validate credit score range")
        void shouldValidateCreditScoreRange() {
            // Given
            int invalidLowScore = 250;
            int invalidHighScore = 900;
            
            // When & Then
            assertThatThrownBy(() -> loanCalculationService.calculatePersonalizedRate(invalidLowScore))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Credit score must be between 300 and 850");
            
            assertThatThrownBy(() -> loanCalculationService.calculatePersonalizedRate(invalidHighScore))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Credit score must be between 300 and 850");
        }
    }
    
    @Nested
    @DisplayName("Payment Distribution Tests")
    class PaymentDistributionTests {
        
        @Test
        @DisplayName("Should calculate payment distribution correctly")
        void shouldCalculatePaymentDistribution() {
            // Given
            BigDecimal outstandingBalance = new BigDecimal("50000.00");
            BigDecimal monthlyPayment = new BigDecimal("2000.00");
            BigDecimal monthlyInterestRate = new BigDecimal("0.01"); // 1% monthly
            
            // When
            PaymentDistribution distribution = loanCalculationService.calculatePaymentDistribution(
                outstandingBalance, monthlyPayment, monthlyInterestRate);
            
            // Then
            assertThat(distribution.getInterestAmount()).isEqualByComparingTo(new BigDecimal("500.00"));
            assertThat(distribution.getPrincipalAmount()).isEqualByComparingTo(new BigDecimal("1500.00"));
        }
        
        @Test
        @DisplayName("Should limit principal to outstanding balance")
        void shouldLimitPrincipalToOutstandingBalance() {
            // Given
            BigDecimal outstandingBalance = new BigDecimal("1000.00");
            BigDecimal monthlyPayment = new BigDecimal("2000.00");
            BigDecimal monthlyInterestRate = new BigDecimal("0.01");
            
            // When
            PaymentDistribution distribution = loanCalculationService.calculatePaymentDistribution(
                outstandingBalance, monthlyPayment, monthlyInterestRate);
            
            // Then
            assertThat(distribution.getInterestAmount()).isEqualByComparingTo(new BigDecimal("10.00"));
            assertThat(distribution.getPrincipalAmount()).isEqualByComparingTo(new BigDecimal("1000.00"));
        }
    }
    
    @Nested
    @DisplayName("Late Penalty Tests")
    class LatePenaltyTests {
        
        @Test
        @DisplayName("Should calculate late penalty correctly")
        void shouldCalculateLatePenalty() {
            // Given
            BigDecimal scheduledAmount = new BigDecimal("1000.00");
            
            // When
            BigDecimal penalty = loanCalculationService.calculateLatePenalty(scheduledAmount);
            
            // Then
            assertThat(penalty).isEqualByComparingTo(new BigDecimal("50.00")); // 5% of 1000
        }
        
        @Test
        @DisplayName("Should return zero penalty for null or zero scheduled amount")
        void shouldReturnZeroPenaltyForNullOrZeroAmount() {
            // Given
            BigDecimal nullAmount = null;
            BigDecimal zeroAmount = BigDecimal.ZERO;
            
            // When
            BigDecimal penaltyForNull = loanCalculationService.calculateLatePenalty(nullAmount);
            BigDecimal penaltyForZero = loanCalculationService.calculateLatePenalty(zeroAmount);
            
            // Then
            assertThat(penaltyForNull).isEqualByComparingTo(BigDecimal.ZERO);
            assertThat(penaltyForZero).isEqualByComparingTo(BigDecimal.ZERO);
        }
    }
    
    @Nested
    @DisplayName("Outstanding Balance Tests")
    class OutstandingBalanceTests {
        
        @Test
        @DisplayName("Should calculate outstanding balance correctly")
        void shouldCalculateOutstandingBalance() {
            // Given
            BigDecimal currentBalance = new BigDecimal("50000.00");
            BigDecimal principalPayment = new BigDecimal("1500.00");
            
            // When
            BigDecimal outstandingBalance = loanCalculationService.calculateOutstandingBalance(
                currentBalance, principalPayment);
            
            // Then
            assertThat(outstandingBalance).isEqualByComparingTo(new BigDecimal("48500.00"));
        }
        
        @Test
        @DisplayName("Should not allow negative outstanding balance")
        void shouldNotAllowNegativeOutstandingBalance() {
            // Given
            BigDecimal currentBalance = new BigDecimal("1000.00");
            BigDecimal principalPayment = new BigDecimal("1500.00");
            
            // When
            BigDecimal outstandingBalance = loanCalculationService.calculateOutstandingBalance(
                currentBalance, principalPayment);
            
            // Then
            assertThat(outstandingBalance).isEqualByComparingTo(BigDecimal.ZERO);
        }
    }
    
    @Nested
    @DisplayName("Total Interest Tests")
    class TotalInterestTests {
        
        @Test
        @DisplayName("Should calculate total interest correctly")
        void shouldCalculateTotalInterest() {
            // Given
            BigDecimal monthlyPayment = new BigDecimal("2000.00");
            int numberOfInstallments = 24;
            BigDecimal loanAmount = new BigDecimal("40000.00");
            
            // When
            BigDecimal totalInterest = loanCalculationService.calculateTotalInterest(
                monthlyPayment, numberOfInstallments, loanAmount);
            
            // Then
            assertThat(totalInterest).isEqualByComparingTo(new BigDecimal("8000.00"));
        }
    }
    
    @Nested
    @DisplayName("Integration Tests")
    class IntegrationTests {
        
        @Test
        @DisplayName("Should calculate complete loan scenario correctly")
        void shouldCalculateCompleteLoanScenario() {
            // Given - Customer with good credit score applying for car loan
            int creditScore = 720;
            BigDecimal loanAmount = new BigDecimal("25000.00");
            int numberOfInstallments = 48; // 4 years
            
            // When
            BigDecimal personalizedRate = loanCalculationService.calculatePersonalizedRate(creditScore);
            BigDecimal monthlyPayment = loanCalculationService.calculateMonthlyPayment(
                loanAmount, personalizedRate, numberOfInstallments);
            BigDecimal totalInterest = loanCalculationService.calculateTotalInterest(
                monthlyPayment, numberOfInstallments, loanAmount);
            
            // Then
            assertThat(personalizedRate).isEqualByComparingTo(new BigDecimal("0.12")); // 12% for 720 score
            assertThat(monthlyPayment).isNotNull();
            assertThat(monthlyPayment.compareTo(BigDecimal.ZERO)).isGreaterThan(0);
            assertThat(totalInterest).isNotNull();
            assertThat(totalInterest.compareTo(BigDecimal.ZERO)).isGreaterThan(0);
        }
    }
}