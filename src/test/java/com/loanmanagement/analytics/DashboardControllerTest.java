package com.loanmanagement.analytics;

import com.loanmanagement.customer.domain.model.Customer;
import com.loanmanagement.customer.domain.model.CustomerStatus;
import com.loanmanagement.shared.domain.model.Money;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * Test-Driven Development tests for Dashboard Controller
 * These tests are designed to FAIL initially and drive the implementation
 * of dashboard analytics endpoints from backup-src
 */
@DisplayName("Dashboard Controller - TDD Tests")
class DashboardControllerTest {

    @Mock
    private RiskAnalyticsService riskAnalyticsService;
    
    @Mock
    private CustomerRepository customerRepository;
    
    private DashboardController dashboardController;
    private List<Customer> testCustomers;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        dashboardController = new DashboardController(riskAnalyticsService, customerRepository);
        
        // Create test customers
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
                .address("123 Main St")
                .occupation("Software Engineer")
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
                .address("456 Oak Ave")
                .occupation("Manager")
                .status(CustomerStatus.ACTIVE)
                .build()
        );
    }

    @Test
    @DisplayName("Should return portfolio overview dashboard data")
    void shouldReturnPortfolioOverviewDashboardData() {
        // Given
        when(customerRepository.findAllActiveCustomers()).thenReturn(testCustomers);
        
        when(riskAnalyticsService.calculatePortfolioPerformanceMetrics(testCustomers))
            .thenReturn(new RiskAnalyticsService.PortfolioPerformanceMetrics(
                2,
                Money.of(new BigDecimal("20000.00"), "USD"),
                Money.of(new BigDecimal("13000.00"), "USD"),
                new BigDecimal("715"),
                new BigDecimal("0.35")
            ));
        
        // When
        ResponseEntity<DashboardController.PortfolioOverviewResponse> response = dashboardController.getPortfolioOverview();
        
        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().totalCustomers()).isEqualTo(2);
        assertThat(response.getBody().totalCreditLimit()).isEqualTo(Money.of(new BigDecimal("20000.00"), "USD"));
        assertThat(response.getBody().totalAvailableCredit()).isEqualTo(Money.of(new BigDecimal("13000.00"), "USD"));
        assertThat(response.getBody().portfolioUtilizationRate()).isEqualTo(new BigDecimal("0.35"));
        assertThat(response.getBody().averageCreditScore()).isEqualTo(new BigDecimal("715"));
    }

    @Test
    @DisplayName("Should return risk distribution analytics")
    void shouldReturnRiskDistributionAnalytics() {
        // Given
        when(customerRepository.findAllActiveCustomers()).thenReturn(testCustomers);
        
        Map<String, Integer> riskDistribution = Map.of(
            "LOW", 1,
            "MEDIUM", 1,
            "HIGH", 0
        );
        
        when(riskAnalyticsService.calculatePortfolioRiskDistribution(testCustomers))
            .thenReturn(riskDistribution);
        
        // When
        ResponseEntity<DashboardController.RiskDistributionResponse> response = dashboardController.getRiskDistribution();
        
        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().riskDistribution()).isEqualTo(riskDistribution);
        assertThat(response.getBody().totalCustomers()).isEqualTo(2);
        assertThat(response.getBody().lastUpdated()).isNotNull();
    }

    @Test
    @DisplayName("Should return credit utilization metrics")
    void shouldReturnCreditUtilizationMetrics() {
        // Given
        when(customerRepository.findAllActiveCustomers()).thenReturn(testCustomers);
        
        BigDecimal averageUtilization = new BigDecimal("0.35");
        when(riskAnalyticsService.calculateAveragePortfolioCreditUtilization(testCustomers))
            .thenReturn(averageUtilization);
        
        // When
        ResponseEntity<DashboardController.CreditUtilizationResponse> response = dashboardController.getCreditUtilizationMetrics();
        
        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().averageUtilization()).isEqualTo(averageUtilization);
        assertThat(response.getBody().utilizationBands()).isNotEmpty();
        assertThat(response.getBody().highUtilizationCustomers()).isNotNull();
    }

    @Test
    @DisplayName("Should return top high-risk customers")
    void shouldReturnTopHighRiskCustomers() {
        // Given
        when(customerRepository.findAllActiveCustomers()).thenReturn(testCustomers);
        
        List<Customer> highRiskCustomers = Arrays.asList(testCustomers.get(1)); // Jane Smith
        when(riskAnalyticsService.identifyHighRiskCustomers(testCustomers))
            .thenReturn(highRiskCustomers);
        
        // When
        ResponseEntity<DashboardController.HighRiskCustomersResponse> response = dashboardController.getHighRiskCustomers();
        
        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().customers()).isNotEmpty();
        assertThat(response.getBody().totalHighRiskCustomers()).isEqualTo(1);
    }

    @Test
    @DisplayName("Should return credit limit recommendations")
    void shouldReturnCreditLimitRecommendations() {
        // Given
        when(customerRepository.findAllActiveCustomers()).thenReturn(testCustomers);
        
        when(riskAnalyticsService.generateCreditLimitRecommendation(any(Customer.class)))
            .thenReturn(new RiskAnalyticsService.CreditLimitRecommendation(
                1L,
                "INCREASE",
                Money.of(new BigDecimal("20000.00"), "USD"),
                "Excellent payment history and low utilization"
            ))
            .thenReturn(new RiskAnalyticsService.CreditLimitRecommendation(
                2L,
                "MAINTAIN",
                Money.of(new BigDecimal("4000.00"), "USD"),
                "Current utilization within acceptable range"
            ));
        
        // When
        ResponseEntity<DashboardController.CreditLimitRecommendationsResponse> response = dashboardController.getCreditLimitRecommendations();
        
        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().recommendations()).hasSize(2);
        assertThat(response.getBody().recommendations().get(0).recommendedAction()).isEqualTo("INCREASE");
        assertThat(response.getBody().recommendations().get(1).recommendedAction()).isEqualTo("MAINTAIN");
    }

    @Test
    @DisplayName("Should return concentration risk analysis")
    void shouldReturnConcentrationRiskAnalysis() {
        // Given
        when(customerRepository.findAllActiveCustomers()).thenReturn(testCustomers);
        
        Map<String, RiskAnalyticsService.ConcentrationRiskMetrics> concentrationRisk = Map.of(
            "EXCELLENT", new RiskAnalyticsService.ConcentrationRiskMetrics(
                1,
                Money.of(new BigDecimal("4000.00"), "USD"),
                new BigDecimal("0.25")
            ),
            "GOOD", new RiskAnalyticsService.ConcentrationRiskMetrics(
                1,
                Money.of(new BigDecimal("3000.00"), "USD"),
                new BigDecimal("0.75")
            )
        );
        
        when(riskAnalyticsService.calculateConcentrationRiskByCreditScoreBands(testCustomers))
            .thenReturn(concentrationRisk);
        
        // When
        ResponseEntity<DashboardController.ConcentrationRiskResponse> response = dashboardController.getConcentrationRiskAnalysis();
        
        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().creditScoreBands()).isEqualTo(concentrationRisk);
        assertThat(response.getBody().riskLevel()).isIn("LOW", "MEDIUM", "HIGH");
    }

    @Test
    @DisplayName("Should return stress test results")
    void shouldReturnStressTestResults() {
        // Given
        when(customerRepository.findAllActiveCustomers()).thenReturn(testCustomers);
        
        RiskAnalyticsService.StressTestResult stressTestResult = new RiskAnalyticsService.StressTestResult(
            "Economic Downturn",
            Money.of(new BigDecimal("2500.00"), "USD"),
            1,
            Arrays.asList("Monitor high-risk customers", "Reduce credit limits for poor credit scores")
        );
        
        when(riskAnalyticsService.performStressTest(any(), any()))
            .thenReturn(stressTestResult);
        
        // When
        ResponseEntity<DashboardController.StressTestResponse> response = dashboardController.performStressTest("Economic Downturn");
        
        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().scenarioName()).isEqualTo("Economic Downturn");
        assertThat(response.getBody().projectedLosses()).isEqualTo(Money.of(new BigDecimal("2500.00"), "USD"));
        assertThat(response.getBody().affectedCustomers()).isEqualTo(1);
        assertThat(response.getBody().recommendedActions()).hasSize(2);
    }

    @Test
    @DisplayName("Should handle empty customer portfolio gracefully")
    void shouldHandleEmptyCustomerPortfolioGracefully() {
        // Given
        when(customerRepository.findAllActiveCustomers()).thenReturn(Arrays.asList());
        
        // When
        ResponseEntity<DashboardController.PortfolioOverviewResponse> response = dashboardController.getPortfolioOverview();
        
        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().totalCustomers()).isEqualTo(0);
        assertThat(response.getBody().totalCreditLimit()).isEqualTo(Money.zero("USD"));
        assertThat(response.getBody().totalAvailableCredit()).isEqualTo(Money.zero("USD"));
    }
}