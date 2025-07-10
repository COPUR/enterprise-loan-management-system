package com.loanmanagement.analytics;

import com.loanmanagement.customer.domain.model.Customer;
import com.loanmanagement.customer.domain.model.CustomerStatus;
import com.loanmanagement.shared.domain.model.Money;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.*;

/**
 * Test-Driven Development tests for Risk Analytics Service
 * These tests are designed to FAIL initially and drive the implementation
 * of risk analytics features from backup-src
 */
@DisplayName("Risk Analytics Service - TDD Tests")
class RiskAnalyticsServiceTest {

    private RiskAnalyticsService riskAnalyticsService;
    private List<Customer> testCustomers;

    @BeforeEach
    void setUp() {
        riskAnalyticsService = new RiskAnalyticsService();
        
        // Create diverse customer portfolio for testing
        testCustomers = Arrays.asList(
            Customer.builder()
                .id(1L)
                .firstName("John")
                .lastName("Doe")
                .email("john.doe@example.com")
                .phone("555-0123")
                .dateOfBirth(LocalDate.of(1985, 6, 15))
                .creditScore(780)
                .monthlyIncome(Money.of(new BigDecimal("8000.00"), "USD"))
                .creditLimit(Money.of(new BigDecimal("16000.00"), "USD"))
                .availableCredit(Money.of(new BigDecimal("12000.00"), "USD"))
                .status(CustomerStatus.ACTIVE)
                .build(),
            Customer.builder()
                .id(2L)
                .firstName("Jane")
                .lastName("Smith")
                .email("jane.smith@example.com")
                .phone("555-0124")
                .dateOfBirth(LocalDate.of(1990, 3, 20))
                .creditScore(650)
                .monthlyIncome(Money.of(new BigDecimal("4000.00"), "USD"))
                .creditLimit(Money.of(new BigDecimal("4000.00"), "USD"))
                .availableCredit(Money.of(new BigDecimal("1000.00"), "USD"))
                .status(CustomerStatus.ACTIVE)
                .build(),
            Customer.builder()
                .id(3L)
                .firstName("Bob")
                .lastName("Johnson")
                .email("bob.johnson@example.com")
                .phone("555-0125")
                .dateOfBirth(LocalDate.of(1975, 11, 5))
                .creditScore(550)
                .monthlyIncome(Money.of(new BigDecimal("3000.00"), "USD"))
                .creditLimit(Money.of(new BigDecimal("1500.00"), "USD"))
                .availableCredit(Money.of(new BigDecimal("500.00"), "USD"))
                .status(CustomerStatus.ACTIVE)
                .build()
        );
    }

    @Test
    @DisplayName("Should calculate portfolio risk distribution")
    void shouldCalculatePortfolioRiskDistribution() {
        // When
        Map<String, Integer> riskDistribution = riskAnalyticsService.calculatePortfolioRiskDistribution(testCustomers);
        
        // Then
        assertThat(riskDistribution).isNotNull();
        assertThat(riskDistribution).containsKeys("LOW", "MEDIUM", "HIGH");
        assertThat(riskDistribution.get("LOW")).isEqualTo(1);
        assertThat(riskDistribution.get("MEDIUM")).isEqualTo(1);
        assertThat(riskDistribution.get("HIGH")).isEqualTo(1);
    }

    @Test
    @DisplayName("Should calculate average portfolio credit utilization")
    void shouldCalculateAveragePortfolioCreditUtilization() {
        // When
        BigDecimal averageUtilization = riskAnalyticsService.calculateAveragePortfolioCreditUtilization(testCustomers);
        
        // Then
        assertThat(averageUtilization).isNotNull();
        assertThat(averageUtilization).isGreaterThan(BigDecimal.ZERO);
        assertThat(averageUtilization).isLessThan(BigDecimal.ONE);
        // Expected: (0.25 + 0.75 + 0.67) / 3 = 0.56 approximately
        assertThat(averageUtilization).isCloseTo(new BigDecimal("0.56"), within(new BigDecimal("0.1")));
    }

    @Test
    @DisplayName("Should identify high-risk customers")
    void shouldIdentifyHighRiskCustomers() {
        // When
        List<Customer> highRiskCustomers = riskAnalyticsService.identifyHighRiskCustomers(testCustomers);
        
        // Then
        assertThat(highRiskCustomers).hasSize(1);
        assertThat(highRiskCustomers.get(0).getCreditScore()).isEqualTo(550);
        assertThat(highRiskCustomers.get(0).getLastName()).isEqualTo("Johnson");
    }

    @Test
    @DisplayName("Should calculate total portfolio exposure")
    void shouldCalculateTotalPortfolioExposure() {
        // When
        Money totalExposure = riskAnalyticsService.calculateTotalPortfolioExposure(testCustomers);
        
        // Then
        assertThat(totalExposure).isNotNull();
        assertThat(totalExposure.getCurrency()).isEqualTo("USD");
        // Expected: (4000 + 3000 + 1000) = 8000 USD in used credit
        assertThat(totalExposure.getAmount()).isEqualTo(new BigDecimal("8000.00"));
    }

    @Test
    @DisplayName("Should generate risk score for individual customer")
    void shouldGenerateRiskScoreForIndividualCustomer() {
        // Given
        Customer customer = testCustomers.get(0); // High credit score, low utilization
        
        // When
        BigDecimal riskScore = riskAnalyticsService.calculateCustomerRiskScore(customer);
        
        // Then
        assertThat(riskScore).isNotNull();
        assertThat(riskScore).isGreaterThan(BigDecimal.ZERO);
        assertThat(riskScore).isLessThan(new BigDecimal("100"));
        // High credit score + low utilization should result in low risk score
        assertThat(riskScore).isLessThan(new BigDecimal("30"));
    }

    @Test
    @DisplayName("Should calculate portfolio performance metrics")
    void shouldCalculatePortfolioPerformanceMetrics() {
        // When
        RiskAnalyticsService.PortfolioPerformanceMetrics metrics = riskAnalyticsService.calculatePortfolioPerformanceMetrics(testCustomers);
        
        // Then
        assertThat(metrics).isNotNull();
        assertThat(metrics.totalCustomers()).isEqualTo(3);
        assertThat(metrics.totalCreditLimit()).isEqualTo(Money.of(new BigDecimal("21500.00"), "USD"));
        assertThat(metrics.totalAvailableCredit()).isEqualTo(Money.of(new BigDecimal("13500.00"), "USD"));
        assertThat(metrics.averageCreditScore()).isEqualTo(new BigDecimal("660"));
        assertThat(metrics.portfolioUtilizationRate()).isCloseTo(new BigDecimal("0.37"), within(new BigDecimal("0.01")));
    }

    @Test
    @DisplayName("Should predict default probability for customer")
    void shouldPredictDefaultProbabilityForCustomer() {
        // Given
        Customer highRiskCustomer = testCustomers.get(2); // Poor credit score, high utilization
        
        // When
        BigDecimal defaultProbability = riskAnalyticsService.predictDefaultProbability(highRiskCustomer);
        
        // Then
        assertThat(defaultProbability).isNotNull();
        assertThat(defaultProbability).isGreaterThan(BigDecimal.ZERO);
        assertThat(defaultProbability).isLessThan(BigDecimal.ONE);
        // High utilization + poor credit score should result in higher default probability
        assertThat(defaultProbability).isGreaterThan(new BigDecimal("0.15"));
    }

    @Test
    @DisplayName("Should generate credit limit recommendations")
    void shouldGenerateCreditLimitRecommendations() {
        // Given
        Customer customer = testCustomers.get(0); // Good customer profile
        
        // When
        RiskAnalyticsService.CreditLimitRecommendation recommendation = riskAnalyticsService.generateCreditLimitRecommendation(customer);
        
        // Then
        assertThat(recommendation).isNotNull();
        assertThat(recommendation.customerId()).isEqualTo(1L);
        assertThat(recommendation.recommendedAction()).isIn("INCREASE", "MAINTAIN", "DECREASE");
        assertThat(recommendation.recommendedLimit()).isNotNull();
        assertThat(recommendation.rationale()).isNotEmpty();
        
        // Good customer should get increase recommendation
        assertThat(recommendation.recommendedAction()).isEqualTo("INCREASE");
        assertThat(recommendation.recommendedLimit().getAmount()).isGreaterThan(customer.getCreditLimit().getAmount());
    }

    @Test
    @DisplayName("Should calculate concentration risk by credit score bands")
    void shouldCalculateConcentrationRiskByCreditScoreBands() {
        // When
        Map<String, RiskAnalyticsService.ConcentrationRiskMetrics> concentrationRisk = riskAnalyticsService.calculateConcentrationRiskByCreditScoreBands(testCustomers);
        
        // Then
        assertThat(concentrationRisk).isNotNull();
        assertThat(concentrationRisk).containsKeys("EXCELLENT", "GOOD", "FAIR", "POOR");
        
        RiskAnalyticsService.ConcentrationRiskMetrics excellentBand = concentrationRisk.get("EXCELLENT");
        assertThat(excellentBand.customerCount()).isEqualTo(1);
        assertThat(excellentBand.totalExposure()).isEqualTo(Money.of(new BigDecimal("4000.00"), "USD"));
        assertThat(excellentBand.averageUtilization()).isEqualTo(new BigDecimal("0.25"));
    }

    @Test
    @DisplayName("Should generate portfolio stress test results")
    void shouldGeneratePortfolioStressTestResults() {
        // Given
        RiskAnalyticsService.StressTestScenario scenario = new RiskAnalyticsService.StressTestScenario(
            "Economic Downturn",
            new BigDecimal("0.30"), // 30% default rate increase
            new BigDecimal("0.20")  // 20% credit score decrease
        );
        
        // When
        RiskAnalyticsService.StressTestResult result = riskAnalyticsService.performStressTest(testCustomers, scenario);
        
        // Then
        assertThat(result).isNotNull();
        assertThat(result.scenarioName()).isEqualTo("Economic Downturn");
        assertThat(result.projectedLosses().getAmount()).isGreaterThan(Money.zero("USD").getAmount());
        assertThat(result.affectedCustomers()).isGreaterThan(0);
        assertThat(result.recommendedActions()).isNotEmpty();
    }

}