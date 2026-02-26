package com.bank.loan.domain.service;

import com.bank.loan.domain.Customer;
import com.bank.loan.domain.LoanEligibilityResult;
import com.bank.shared.kernel.domain.CustomerId;
import com.bank.shared.kernel.domain.Money;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

class LoanEligibilityServiceTest {

    private final LoanEligibilityService service = new LoanEligibilityService();

    @Test
    void assessEligibilityShouldApproveQualifiedCustomer() {
        Customer customer = stubCustomer(
            true,
            720,
            LocalDate.now().minusYears(35),
            new BigDecimal("20000"),
            new BigDecimal("2000")
        );

        LoanEligibilityResult result = service.assessEligibility(customer, Money.aed(new BigDecimal("100000")));

        assertThat(result.isApproved()).isTrue();
        assertThat(result.hasPassedAllChecks()).isTrue();
        assertThat(result.getPassedCheckCount()).isGreaterThan(0);
    }

    @Test
    void assessEligibilityShouldRejectInvalidInputs() {
        LoanEligibilityResult nullCustomer = service.assessEligibility(null, Money.aed(new BigDecimal("10000")));
        LoanEligibilityResult invalidAmount = service.assessEligibility(
            stubCustomer(true, 700, LocalDate.now().minusYears(30), new BigDecimal("10000"), new BigDecimal("1000")),
            Money.aed(BigDecimal.ZERO)
        );

        assertThat(nullCustomer.isRejected()).isTrue();
        assertThat(nullCustomer.getPrimaryReason()).contains("cannot be null");
        assertThat(invalidAmount.isRejected()).isTrue();
        assertThat(invalidAmount.getPrimaryReason()).contains("must be positive");
    }

    @Test
    void assessEligibilityShouldRejectHighPriorityFailures() {
        Customer lowScore = stubCustomer(
            true,
            550,
            LocalDate.now().minusYears(30),
            new BigDecimal("15000"),
            new BigDecimal("1000")
        );

        LoanEligibilityResult result = service.assessEligibility(lowScore, Money.aed(new BigDecimal("50000")));

        assertThat(result.isRejected()).isTrue();
        assertThat(result.getPrimaryReason()).contains("Credit score");
    }

    @Test
    void assessEligibilityShouldRequireReviewForModeratePriorityFailures() {
        Customer employmentGap = stubCustomer(
            true,
            730,
            LocalDate.now().minusYears(33),
            new BigDecimal("4900"),
            BigDecimal.ZERO
        );

        LoanEligibilityResult result = service.assessEligibility(employmentGap, Money.aed(new BigDecimal("10000")));

        assertThat(result.requiresReview()).isTrue();
        assertThat(result.getPrimaryReason()).contains("Manual review required");
    }

    @Test
    void calculateMaximumLoanAmountShouldReturnZeroWhenBasicEligibilityFails() {
        Customer ineligible = stubCustomer(
            false,
            500,
            LocalDate.now().minusYears(16),
            new BigDecimal("2000"),
            new BigDecimal("1500")
        );

        Money maxAmount = service.calculateMaximumLoanAmount(ineligible);

        assertThat(maxAmount.getAmount()).isEqualByComparingTo("0.00");
    }

    @Test
    void calculateMaximumLoanAmountShouldReturnPositiveForQualifiedCustomer() {
        Customer eligible = stubCustomer(
            true,
            780,
            LocalDate.now().minusYears(40),
            new BigDecimal("25000"),
            new BigDecimal("1000")
        );

        Money maxAmount = service.calculateMaximumLoanAmount(eligible);

        assertThat(maxAmount.getAmount()).isPositive();
    }

    @Test
    void isPreQualifiedShouldEvaluateBasicCriteriaOnly() {
        Customer eligible = stubCustomer(
            true,
            700,
            LocalDate.now().minusYears(30),
            new BigDecimal("9000"),
            new BigDecimal("500")
        );
        Customer ineligible = stubCustomer(
            true,
            500,
            LocalDate.now().minusYears(30),
            new BigDecimal("9000"),
            new BigDecimal("500")
        );

        assertThat(service.isPreQualified(eligible)).isTrue();
        assertThat(service.isPreQualified(ineligible)).isFalse();
        assertThat(service.isPreQualified(null)).isFalse();
    }

    private static Customer stubCustomer(
        boolean active,
        Integer creditScore,
        LocalDate dateOfBirth,
        BigDecimal monthlyIncome,
        BigDecimal obligations
    ) {
        return new Customer() {
            @Override
            public CustomerId getId() {
                return CustomerId.of("CUST-LOAN-ELIG-001");
            }

            @Override
            public boolean isActive() {
                return active;
            }

            @Override
            public Integer getCreditScore() {
                return creditScore;
            }

            @Override
            public LocalDate getDateOfBirth() {
                return dateOfBirth;
            }

            @Override
            public BigDecimal getMonthlyIncome() {
                return monthlyIncome;
            }

            @Override
            public BigDecimal getExistingMonthlyObligations() {
                return obligations;
            }

            @Override
            public String getFirstName() {
                return "Ali";
            }

            @Override
            public String getLastName() {
                return "Copur";
            }

            @Override
            public String getEmail() {
                return "ali@example.com";
            }

            @Override
            public String getPhoneNumber() {
                return "+971500000001";
            }
        };
    }
}
