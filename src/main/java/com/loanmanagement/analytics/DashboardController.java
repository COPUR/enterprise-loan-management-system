package com.loanmanagement.analytics;

import com.loanmanagement.customer.domain.model.Customer;
import com.loanmanagement.shared.domain.model.Money;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Dashboard Controller for analytics and risk management endpoints
 * Provides REST API for portfolio analytics and risk assessment
 */
@RestController
@RequestMapping("/api/dashboard")
public class DashboardController {

    private final RiskAnalyticsService riskAnalyticsService;
    private final CustomerRepository customerRepository;

    public DashboardController(RiskAnalyticsService riskAnalyticsService, 
                             CustomerRepository customerRepository) {
        this.riskAnalyticsService = riskAnalyticsService;
        this.customerRepository = customerRepository;
    }

    @GetMapping("/portfolio-overview")
    public ResponseEntity<PortfolioOverviewResponse> getPortfolioOverview() {
        List<Customer> customers = customerRepository.findAllActiveCustomers();
        
        if (customers.isEmpty()) {
            return ResponseEntity.ok(new PortfolioOverviewResponse(
                0,
                Money.zero("USD"),
                Money.zero("USD"),
                BigDecimal.ZERO,
                BigDecimal.ZERO
            ));
        }
        
        RiskAnalyticsService.PortfolioPerformanceMetrics metrics = 
            riskAnalyticsService.calculatePortfolioPerformanceMetrics(customers);
        
        return ResponseEntity.ok(new PortfolioOverviewResponse(
            metrics.totalCustomers(),
            metrics.totalCreditLimit(),
            metrics.totalAvailableCredit(),
            metrics.portfolioUtilizationRate(),
            metrics.averageCreditScore()
        ));
    }

    @GetMapping("/risk-distribution")
    public ResponseEntity<RiskDistributionResponse> getRiskDistribution() {
        List<Customer> customers = customerRepository.findAllActiveCustomers();
        Map<String, Integer> riskDistribution = 
            riskAnalyticsService.calculatePortfolioRiskDistribution(customers);
        
        return ResponseEntity.ok(new RiskDistributionResponse(
            riskDistribution,
            customers.size(),
            LocalDateTime.now()
        ));
    }

    @GetMapping("/credit-utilization")
    public ResponseEntity<CreditUtilizationResponse> getCreditUtilizationMetrics() {
        List<Customer> customers = customerRepository.findAllActiveCustomers();
        BigDecimal averageUtilization = 
            riskAnalyticsService.calculateAveragePortfolioCreditUtilization(customers);
        
        // Calculate utilization bands
        Map<String, Integer> utilizationBands = customers.stream()
            .collect(Collectors.groupingBy(
                customer -> {
                    BigDecimal utilization = customer.getCreditUtilizationRatio();
                    if (utilization.compareTo(new BigDecimal("0.30")) <= 0) return "LOW";
                    else if (utilization.compareTo(new BigDecimal("0.70")) <= 0) return "MEDIUM";
                    else return "HIGH";
                },
                Collectors.collectingAndThen(Collectors.counting(), Math::toIntExact)
            ));
        
        // Get high utilization customers (>75%)
        List<CustomerUtilizationInfo> highUtilizationCustomers = customers.stream()
            .filter(customer -> customer.getCreditUtilizationRatio().compareTo(new BigDecimal("0.75")) > 0)
            .map(customer -> new CustomerUtilizationInfo(
                customer.getId(),
                customer.getFullName(),
                customer.getCreditUtilizationRatio(),
                customer.getCreditLimit(),
                customer.getAvailableCredit()
            ))
            .collect(Collectors.toList());
        
        return ResponseEntity.ok(new CreditUtilizationResponse(
            averageUtilization,
            utilizationBands,
            highUtilizationCustomers
        ));
    }

    @GetMapping("/high-risk-customers")
    public ResponseEntity<HighRiskCustomersResponse> getHighRiskCustomers() {
        List<Customer> customers = customerRepository.findAllActiveCustomers();
        List<Customer> highRiskCustomers = riskAnalyticsService.identifyHighRiskCustomers(customers);
        
        List<RiskCustomerInfo> riskCustomerInfos = customers.stream()
            .filter(customer -> !customer.calculateRiskLevel().equals("LOW"))
            .map(customer -> new RiskCustomerInfo(
                customer.getId(),
                customer.getFullName(),
                customer.calculateRiskLevel(),
                riskAnalyticsService.calculateCustomerRiskScore(customer),
                customer.getCreditUtilizationRatio()
            ))
            .collect(Collectors.toList());
        
        return ResponseEntity.ok(new HighRiskCustomersResponse(
            riskCustomerInfos,
            highRiskCustomers.size()
        ));
    }

    @GetMapping("/credit-limit-recommendations")
    public ResponseEntity<CreditLimitRecommendationsResponse> getCreditLimitRecommendations() {
        List<Customer> customers = customerRepository.findAllActiveCustomers();
        
        List<RiskAnalyticsService.CreditLimitRecommendation> recommendations = customers.stream()
            .map(riskAnalyticsService::generateCreditLimitRecommendation)
            .collect(Collectors.toList());
        
        return ResponseEntity.ok(new CreditLimitRecommendationsResponse(
            recommendations,
            recommendations.size()
        ));
    }

    @GetMapping("/concentration-risk")
    public ResponseEntity<ConcentrationRiskResponse> getConcentrationRiskAnalysis() {
        List<Customer> customers = customerRepository.findAllActiveCustomers();
        Map<String, RiskAnalyticsService.ConcentrationRiskMetrics> concentrationRisk = 
            riskAnalyticsService.calculateConcentrationRiskByCreditScoreBands(customers);
        
        // Calculate overall risk level based on concentration
        String overallRiskLevel = calculateOverallConcentrationRisk(concentrationRisk, customers.size());
        
        return ResponseEntity.ok(new ConcentrationRiskResponse(
            concentrationRisk,
            overallRiskLevel
        ));
    }

    @PostMapping("/stress-test/{scenarioName}")
    public ResponseEntity<StressTestResponse> performStressTest(@PathVariable String scenarioName) {
        List<Customer> customers = customerRepository.findAllActiveCustomers();
        
        // Create stress test scenario based on name
        RiskAnalyticsService.StressTestScenario scenario = createStressTestScenario(scenarioName);
        
        RiskAnalyticsService.StressTestResult result = 
            riskAnalyticsService.performStressTest(customers, scenario);
        
        return ResponseEntity.ok(new StressTestResponse(
            result.scenarioName(),
            result.projectedLosses(),
            result.affectedCustomers(),
            result.recommendedActions()
        ));
    }

    private String calculateOverallConcentrationRisk(
        Map<String, RiskAnalyticsService.ConcentrationRiskMetrics> concentrationRisk,
        int totalCustomers) {
        
        if (totalCustomers == 0) return "LOW";
        
        int poorCustomers = concentrationRisk.get("POOR").customerCount();
        int fairCustomers = concentrationRisk.get("FAIR").customerCount();
        
        double highRiskPercentage = (double) (poorCustomers + fairCustomers) / totalCustomers;
        
        if (highRiskPercentage > 0.40) return "HIGH";
        else if (highRiskPercentage > 0.20) return "MEDIUM";
        else return "LOW";
    }

    private RiskAnalyticsService.StressTestScenario createStressTestScenario(String scenarioName) {
        return switch (scenarioName.toLowerCase()) {
            case "economic downturn" -> new RiskAnalyticsService.StressTestScenario(
                "Economic Downturn",
                new BigDecimal("0.30"),
                new BigDecimal("0.20")
            );
            case "market volatility" -> new RiskAnalyticsService.StressTestScenario(
                "Market Volatility",
                new BigDecimal("0.15"),
                new BigDecimal("0.10")
            );
            case "interest rate shock" -> new RiskAnalyticsService.StressTestScenario(
                "Interest Rate Shock",
                new BigDecimal("0.25"),
                new BigDecimal("0.15")
            );
            default -> new RiskAnalyticsService.StressTestScenario(
                "Mild Stress",
                new BigDecimal("0.10"),
                new BigDecimal("0.05")
            );
        };
    }

    // Response record classes for REST endpoints
    public record PortfolioOverviewResponse(
        int totalCustomers,
        Money totalCreditLimit,
        Money totalAvailableCredit,
        BigDecimal portfolioUtilizationRate,
        BigDecimal averageCreditScore
    ) {}

    public record RiskDistributionResponse(
        Map<String, Integer> riskDistribution,
        int totalCustomers,
        LocalDateTime lastUpdated
    ) {}

    public record CreditUtilizationResponse(
        BigDecimal averageUtilization,
        Map<String, Integer> utilizationBands,
        List<CustomerUtilizationInfo> highUtilizationCustomers
    ) {}

    public record CustomerUtilizationInfo(
        Long customerId,
        String customerName,
        BigDecimal utilizationRate,
        Money creditLimit,
        Money availableCredit
    ) {}

    public record HighRiskCustomersResponse(
        List<RiskCustomerInfo> customers,
        int totalHighRiskCustomers
    ) {}

    public record RiskCustomerInfo(
        Long customerId,
        String customerName,
        String riskLevel,
        BigDecimal riskScore,
        BigDecimal creditUtilization
    ) {}

    public record CreditLimitRecommendationsResponse(
        List<RiskAnalyticsService.CreditLimitRecommendation> recommendations,
        int totalRecommendations
    ) {}

    public record ConcentrationRiskResponse(
        Map<String, RiskAnalyticsService.ConcentrationRiskMetrics> creditScoreBands,
        String riskLevel
    ) {}

    public record StressTestResponse(
        String scenarioName,
        Money projectedLosses,
        int affectedCustomers,
        List<String> recommendedActions
    ) {}
}