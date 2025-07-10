package com.loanmanagement.loan.domain.model;

import com.loanmanagement.shared.domain.Money;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.*;

/**
 * Comprehensive TDD Test Suite for Java 21 Enhanced Domain Models
 * 
 * Testing:
 * - Sealed interfaces and pattern matching
 * - Record patterns and validation
 * - Enhanced switch expressions
 * - Sequenced Collections behavior
 * - Type-safe financial calculations
 */
@DisplayName("Java 21 Enhanced Domain Models")
class DomainModelTest {
    
    private LoanAggregate loan;
    private CustomerId customerId;
    private Money principal;
    private RiskAssessment riskAssessment;
    
    @BeforeEach
    void setUp() {
        customerId = new CustomerId("CUST001");
        principal = new Money(BigDecimal.valueOf(100000), "USD");
        riskAssessment = new RiskAssessment(
            RiskLevel.MEDIUM,
            0.75,
            "Standard risk assessment",
            List.of("Good credit score", "Stable employment"),
            BigDecimal.valueOf(5.5)
        );
        
        loan = new LoanAggregate(
            LoanId.newLoanId(),
            customerId,
            principal,
            LoanPurpose.HOME_PURCHASE,
            riskAssessment
        );
    }
    
    @Nested
    @DisplayName("Sealed Interface Pattern Matching")
    class SealedInterfacePatternMatching {
        
        @Test
        @DisplayName("Should use pattern matching for loan state transitions")
        void shouldUsePatternMatchingForLoanStateTransitions() {
            // Given - Loan in pending state
            assertThat(loan.getCurrentState()).isInstanceOf(PendingState.class);
            
            // When - Approve loan
            var approveAction = new LoanAction.Approve(
                principal,
                List.of("Credit verification completed"),
                "Customer meets all criteria"
            );
            var officerId = new LoanOfficerId("OFFICER001");
            
            var result = loan.transitionTo(approveAction, "Loan approved", officerId);
            
            // Then - State should transition correctly
            assertThat(result.success()).isTrue();
            assertThat(result.newState()).isInstanceOf(ApprovedState.class);
            assertThat(loan.getCurrentState()).isInstanceOf(ApprovedState.class);
            
            // Verify approved state details using pattern matching
            switch (loan.getCurrentState()) {
                case ApprovedState(var timestamp, var reason, var officer, var conditions, var amount) -> {
                    assertThat(reason).isEqualTo("Loan approved");
                    assertThat(officer).isEqualTo(officerId);
                    assertThat(conditions).contains("Credit verification completed");
                    assertThat(amount).isEqualTo(principal);
                }
                default -> fail("Expected ApprovedState");
            }
        }
        
        @Test
        @DisplayName("Should prevent invalid state transitions using pattern matching")
        void shouldPreventInvalidStateTransitionsUsingPatternMatching() {
            // Given - Loan in pending state
            var paymentAction = new LoanAction.MakePayment(
                new Money(BigDecimal.valueOf(1000), "USD"),
                "BANK_TRANSFER",
                "REF123"
            );
            
            // When - Try to make payment on pending loan (invalid)
            var result = loan.transitionTo(paymentAction, "Payment attempt", null);
            
            // Then - Should fail
            assertThat(result.success()).isFalse();
            assertThat(result.message()).contains("Invalid state transition");
            assertThat(loan.getCurrentState()).isInstanceOf(PendingState.class);
        }
        
        @Test
        @DisplayName("Should handle terminal states correctly")
        void shouldHandleTerminalStatesCorrectly() {
            // Given - Reject the loan
            var rejectAction = new LoanAction.Reject("Insufficient income", "INC001");
            var officerId = new LoanOfficerId("OFFICER001");
            
            var result = loan.transitionTo(rejectAction, "Loan rejected", officerId);
            
            // Then - Should be in terminal state
            assertThat(result.success()).isTrue();
            assertThat(loan.getCurrentState().isTerminalState()).isTrue();
            
            // Try another action on terminal state
            var approveAction = new LoanAction.Approve(principal, List.of(), "Trying to approve");
            var secondResult = loan.transitionTo(approveAction, "Cannot approve", officerId);
            
            assertThat(secondResult.success()).isFalse();
        }
    }
    
    @Nested
    @DisplayName("Customer Profile Pattern Matching")
    class CustomerProfilePatternMatching {
        
        @Test
        @DisplayName("Should assess premium customer risk using pattern matching")
        void shouldAssessPremiumCustomerRiskUsingPatternMatching() {
            // Given - Premium customer
            var premiumCustomer = new PremiumCustomer(
                customerId,
                "John Premium",
                780,
                new Money(BigDecimal.valueOf(150000), "USD"),
                LocalDate.of(1980, 1, 1),
                8,
                new Money(BigDecimal.valueOf(500000), "USD")
            );
            
            // When - Calculate risk assessment
            var assessment = premiumCustomer.calculateRiskAssessment();
            
            // Then - Should have low risk
            assertThat(assessment.riskLevel()).isEqualTo(RiskLevel.LOW);
            assertThat(assessment.confidence()).isGreaterThan(0.9);
            assertThat(assessment.reasoning()).contains("Premium customer");
            assertThat(assessment.recommendedInterestRate()).isLessThan(BigDecimal.valueOf(4.0));
        }
        
        @Test
        @DisplayName("Should assess young professional eligibility")
        void shouldAssessYoungProfessionalEligibility() {
            // Given - Young professional
            var youngProfessional = new YoungProfessional(
                customerId,
                "Jane Engineer",
                720,
                new Money(BigDecimal.valueOf(85000), "USD"),
                LocalDate.of(1995, 6, 15),
                EducationLevel.GRADUATE,
                Profession.ENGINEER
            );
            
            var requestedAmount = new Money(BigDecimal.valueOf(300000), "USD");
            
            // When - Check loan eligibility
            var eligibility = youngProfessional.checkLoanEligibility(requestedAmount, LoanPurpose.HOME_PURCHASE);
            
            // Then - Should be eligible with special terms
            assertThat(eligibility.eligible()).isTrue();
            assertThat(eligibility.reason()).contains("young professional");
            assertThat(eligibility.interestRate()).isLessThan(BigDecimal.valueOf(7.0));
        }
        
        @Test
        @DisplayName("Should handle business customer risk assessment")
        void shouldHandleBusinessCustomerRiskAssessment() {
            // Given - Technology business customer
            var businessCustomer = new BusinessCustomer(
                customerId,
                "Tech Startup Inc",
                700,
                new Money(BigDecimal.valueOf(25000), "USD"),
                LocalDate.of(1985, 3, 20),
                BusinessType.TECHNOLOGY,
                5,
                new Money(BigDecimal.valueOf(1200000), "USD")
            );
            
            // When - Calculate risk assessment
            var assessment = businessCustomer.calculateRiskAssessment();
            
            // Then - Should reflect technology business advantages
            assertThat(assessment.riskLevel()).isEqualTo(RiskLevel.LOW);
            assertThat(assessment.reasoning()).contains("Established business");
            assertThat(assessment.recommendedInterestRate()).isLessThan(BigDecimal.valueOf(7.0));
        }
    }
    
    @Nested
    @DisplayName("Sequenced Collections Usage")
    class SequencedCollectionsUsage {
        
        @Test
        @DisplayName("Should maintain payment order in loan history")
        void shouldMaintainPaymentOrderInLoanHistory() {
            // Given - Approved and disbursed loan
            approveLoan();
            disburseLoan();
            
            // When - Make multiple payments
            makePayment(BigDecimal.valueOf(1000), "PAYMENT001");
            makePayment(BigDecimal.valueOf(1500), "PAYMENT002");
            makePayment(BigDecimal.valueOf(1200), "PAYMENT003");
            
            // Then - Payment history should maintain order
            var paymentHistory = loan.getPaymentHistory();
            assertThat(paymentHistory).hasSize(3);
            
            // Check first and last payments using Sequenced Collections
            var firstPayment = paymentHistory.getFirst();
            var lastPayment = paymentHistory.getLast();
            
            assertThat(firstPayment.amount().getAmount()).isEqualTo(BigDecimal.valueOf(1000));
            assertThat(lastPayment.amount().getAmount()).isEqualTo(BigDecimal.valueOf(1200));
            
            // Verify chronological order
            var payments = paymentHistory.stream().toList();
            for (int i = 1; i < payments.size(); i++) {
                assertThat(payments.get(i).timestamp())
                    .isAfterOrEqualTo(payments.get(i-1).timestamp());
            }
        }
        
        @Test
        @DisplayName("Should track state transitions in order")
        void shouldTrackStateTransitionsInOrder() {
            // Given - New loan
            // When - Perform multiple state transitions
            approveLoan();
            disburseLoan();
            
            // Then - State transitions should be ordered
            var transitions = loan.getStateTransitions();
            assertThat(transitions).hasSize(2);
            
            var transitionList = transitions.stream().toList();
            
            // First transition: Pending -> Approved
            assertThat(transitionList.get(0).fromState()).isInstanceOf(PendingState.class);
            assertThat(transitionList.get(0).toState()).isInstanceOf(ApprovedState.class);
            
            // Second transition: Approved -> Disbursed
            assertThat(transitionList.get(1).fromState()).isInstanceOf(ApprovedState.class);
            assertThat(transitionList.get(1).toState()).isInstanceOf(DisbursedState.class);
        }
    }
    
    @Nested
    @DisplayName("Enhanced Switch Expressions")
    class EnhancedSwitchExpressions {
        
        @Test
        @DisplayName("Should calculate authorization levels using enhanced switch")
        void shouldCalculateAuthorizationLevelsUsingEnhancedSwitch() {
            // Given - Different loan actions
            var smallApproval = new LoanAction.Approve(
                new Money(BigDecimal.valueOf(50000), "USD"),
                List.of(),
                "Small loan approval"
            );
            
            var largeApproval = new LoanAction.Approve(
                new Money(BigDecimal.valueOf(150000), "USD"),
                List.of(),
                "Large loan approval"
            );
            
            var restructure = new LoanAction.Restructure(
                new LoanTerms(/* simplified */),
                "Financial hardship",
                BigDecimal.valueOf(4.5)
            );
            
            // When/Then - Check authorization levels
            assertThat(smallApproval.getRequiredAuthorizationLevel())
                .isEqualTo(AuthorizationLevel.LOAN_OFFICER);
                
            assertThat(largeApproval.getRequiredAuthorizationLevel())
                .isEqualTo(AuthorizationLevel.SENIOR_MANAGER);
                
            assertThat(restructure.getRequiredAuthorizationLevel())
                .isEqualTo(AuthorizationLevel.MANAGER);
        }
        
        @Test
        @DisplayName("Should validate payments using pattern matching")
        void shouldValidatePaymentsUsingPatternMatching() {
            // Given - Approved and disbursed loan
            approveLoan();
            disburseLoan();
            
            // When - Try different payment scenarios
            var validPayment = new LoanAction.MakePayment(
                new Money(BigDecimal.valueOf(1000), "USD"),
                "BANK_TRANSFER",
                "REF001"
            );
            
            var negativePayment = new LoanAction.MakePayment(
                new Money(BigDecimal.valueOf(-100), "USD"),
                "BANK_TRANSFER",
                "REF002"
            );
            
            var excessivePayment = new LoanAction.MakePayment(
                new Money(BigDecimal.valueOf(200000), "USD"),
                "BANK_TRANSFER",
                "REF003"
            );
            
            // Then - Validation should work correctly
            var validResult = loan.transitionTo(validPayment, "Valid payment", null);
            assertThat(validResult.success()).isTrue();
            
            var negativeResult = loan.transitionTo(negativePayment, "Negative payment", null);
            assertThat(negativeResult.success()).isFalse();
            assertThat(negativeResult.message()).contains("positive");
            
            var excessiveResult = loan.transitionTo(excessivePayment, "Excessive payment", null);
            assertThat(excessiveResult.success()).isFalse();
            assertThat(excessiveResult.message()).contains("exceeds");
        }
    }
    
    @Nested
    @DisplayName("Financial Calculations with Records")
    class FinancialCalculationsWithRecords {
        
        @Test
        @DisplayName("Should calculate amortization schedule correctly")
        void shouldCalculateAmortizationScheduleCorrectly() {
            // Given - Disbursed loan
            approveLoan();
            disburseLoan();
            
            // When - Calculate amortization schedule
            var schedule = loan.calculateAmortizationSchedule(12); // 12 months
            
            // Then - Should have correct structure
            assertThat(schedule.payments()).hasSize(12);
            
            var firstPayment = schedule.getFirstPayment();
            var lastPayment = schedule.getLastPayment();
            
            assertThat(firstPayment.paymentNumber()).isEqualTo(1);
            assertThat(lastPayment.paymentNumber()).isEqualTo(12);
            
            // Verify total interest calculation
            var totalInterest = schedule.getTotalInterest();
            assertThat(totalInterest).isGreaterThan(BigDecimal.ZERO);
            
            // Verify final balance is zero
            assertThat(lastPayment.remainingBalance().getAmount())
                .isCloseTo(BigDecimal.ZERO, within(BigDecimal.valueOf(1.0)));
        }
        
        @Test
        @DisplayName("Should generate comprehensive loan analytics")
        void shouldGenerateComprehensiveLoanAnalytics() {
            // Given - Loan with payment history
            approveLoan();
            disburseLoan();
            makePayment(BigDecimal.valueOf(1000), "PAYMENT001");
            makePayment(BigDecimal.valueOf(1000), "PAYMENT002");
            makePayment(BigDecimal.valueOf(1100), "PAYMENT003");
            
            // When - Get loan analytics
            var analytics = loan.getLoanAnalytics();
            
            // Then - Should have comprehensive data
            assertThat(analytics.totalPayments()).isEqualTo(3);
            assertThat(analytics.totalAmountPaid()).isEqualTo(BigDecimal.valueOf(3100));
            assertThat(analytics.averagePaymentAmount()).isCloseTo(
                BigDecimal.valueOf(1033.33), 
                within(BigDecimal.valueOf(0.01))
            );
            assertThat(analytics.paymentConsistencyScore()).isGreaterThan(0.5);
            assertThat(analytics.currentRiskLevel()).isNotNull();
        }
    }
    
    // Helper methods
    private void approveLoan() {
        var approveAction = new LoanAction.Approve(
            principal,
            List.of("Credit verification completed"),
            "Customer meets all criteria"
        );
        var officerId = new LoanOfficerId("OFFICER001");
        loan.transitionTo(approveAction, "Loan approved", officerId);
    }
    
    private void disburseLoan() {
        var disburseAction = new LoanAction.Disburse(
            principal,
            "BANK_TRANSFER",
            "ACC123456"
        );
        loan.transitionTo(disburseAction, "Loan disbursed", null);
    }
    
    private void makePayment(BigDecimal amount, String reference) {
        var paymentAction = new LoanAction.MakePayment(
            new Money(amount, "USD"),
            "BANK_TRANSFER",
            reference
        );
        loan.transitionTo(paymentAction, "Payment made", null);
    }
}