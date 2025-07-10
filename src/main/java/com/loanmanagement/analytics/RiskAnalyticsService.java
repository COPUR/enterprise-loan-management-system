package com.loanmanagement.analytics;

import com.loanmanagement.customer.domain.model.Customer;
import com.loanmanagement.shared.domain.model.Money;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Risk Analytics Service for portfolio analysis and risk assessment
 * Migrated from backup-src with enhanced functionality
 */
@Service
public class RiskAnalyticsService {

    public Map<String, Integer> calculatePortfolioRiskDistribution(List<Customer> customers) {
        Map<String, Integer> distribution = new HashMap<>();
        distribution.put("LOW", 0);
        distribution.put("MEDIUM", 0);
        distribution.put("HIGH", 0);
        
        for (Customer customer : customers) {
            String riskLevel = customer.calculateRiskLevel();
            distribution.merge(riskLevel, 1, Integer::sum);
        }
        
        return distribution;
    }

    public BigDecimal calculateAveragePortfolioCreditUtilization(List<Customer> customers) {
        if (customers.isEmpty()) {
            return BigDecimal.ZERO;
        }
        
        BigDecimal totalUtilization = customers.stream()
            .map(Customer::getCreditUtilizationRatio)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        return totalUtilization.divide(BigDecimal.valueOf(customers.size()), 2, RoundingMode.HALF_UP);
    }

    public List<Customer> identifyHighRiskCustomers(List<Customer> customers) {
        return customers.stream()
            .filter(customer -> "HIGH".equals(customer.calculateRiskLevel()))
            .sorted((c1, c2) -> {
                BigDecimal score1 = calculateCustomerRiskScore(c1);
                BigDecimal score2 = calculateCustomerRiskScore(c2);
                return score2.compareTo(score1); // Descending order (highest risk first)
            })
            .collect(Collectors.toList());
    }

    public Money calculateTotalPortfolioExposure(List<Customer> customers) {
        if (customers.isEmpty()) {
            return Money.zero("USD");
        }
        
        String currency = customers.get(0).getCreditLimit().getCurrency();
        BigDecimal totalExposure = customers.stream()
            .map(customer -> {
                Money creditLimit = customer.getCreditLimit();
                Money availableCredit = customer.getAvailableCredit();
                return creditLimit.subtract(availableCredit).getAmount();
            })
            .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        return Money.of(totalExposure, currency);
    }

    public BigDecimal calculateCustomerRiskScore(Customer customer) {
        if (customer.getCreditScore() == null) {
            return new BigDecimal("100"); // Maximum risk for unknown credit score
        }
        
        // Base risk from credit score (inverted: lower credit score = higher risk)
        BigDecimal creditRisk = new BigDecimal("100")
            .subtract(new BigDecimal(customer.getCreditScore()).divide(new BigDecimal("850"), 2, RoundingMode.HALF_UP)
                .multiply(new BigDecimal("100")));
        
        // Utilization risk
        BigDecimal utilizationRisk = customer.getCreditUtilizationRatio().multiply(new BigDecimal("30"));
        
        // Age risk (very young or very old customers have higher risk)
        int age = customer.getAge();
        BigDecimal ageRisk = BigDecimal.ZERO;
        if (age < 25 || age > 65) {
            ageRisk = new BigDecimal("10");
        }
        
        // Income stability risk
        Money income = customer.getMonthlyIncome();
        BigDecimal incomeRisk = BigDecimal.ZERO;
        if (income.getAmount().compareTo(new BigDecimal("2000")) < 0) {
            incomeRisk = new BigDecimal("15");
        }
        
        BigDecimal totalRisk = creditRisk.add(utilizationRisk).add(ageRisk).add(incomeRisk);
        
        // Cap at 100
        return totalRisk.min(new BigDecimal("100"));
    }

    public PortfolioPerformanceMetrics calculatePortfolioPerformanceMetrics(List<Customer> customers) {
        if (customers.isEmpty()) {
            return new PortfolioPerformanceMetrics(
                0,
                Money.zero("USD"),
                Money.zero("USD"),
                BigDecimal.ZERO,
                BigDecimal.ZERO
            );
        }
        
        String currency = customers.get(0).getCreditLimit().getCurrency();
        
        BigDecimal totalCreditLimit = customers.stream()
            .map(customer -> customer.getCreditLimit().getAmount())
            .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        BigDecimal totalAvailableCredit = customers.stream()
            .map(customer -> customer.getAvailableCredit().getAmount())
            .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        BigDecimal averageCreditScore = customers.stream()
            .filter(customer -> customer.getCreditScore() != null)
            .map(customer -> new BigDecimal(customer.getCreditScore()))
            .reduce(BigDecimal.ZERO, BigDecimal::add)
            .divide(BigDecimal.valueOf(customers.size()), 0, RoundingMode.HALF_UP);
        
        BigDecimal portfolioUtilizationRate = totalCreditLimit.compareTo(BigDecimal.ZERO) > 0 ?
            totalCreditLimit.subtract(totalAvailableCredit).divide(totalCreditLimit, 2, RoundingMode.HALF_UP) :
            BigDecimal.ZERO;
        
        return new PortfolioPerformanceMetrics(
            customers.size(),
            Money.of(totalCreditLimit, currency),
            Money.of(totalAvailableCredit, currency),
            averageCreditScore,
            portfolioUtilizationRate
        );
    }

    public BigDecimal predictDefaultProbability(Customer customer) {
        BigDecimal riskScore = calculateCustomerRiskScore(customer);
        
        // Convert risk score to probability (0-1 scale)
        BigDecimal baseProbability = riskScore.divide(new BigDecimal("100"), 4, RoundingMode.HALF_UP);
        
        // Adjust based on credit utilization
        BigDecimal utilizationAdjustment = customer.getCreditUtilizationRatio()
            .multiply(new BigDecimal("0.20")); // High utilization increases probability
        
        // Adjust based on credit score
        BigDecimal creditScoreAdjustment = BigDecimal.ZERO;
        if (customer.getCreditScore() != null && customer.getCreditScore() < 600) {
            creditScoreAdjustment = new BigDecimal("0.15");
        }
        
        BigDecimal totalProbability = baseProbability.add(utilizationAdjustment).add(creditScoreAdjustment);
        
        // Cap between 0 and 1
        return totalProbability.max(BigDecimal.ZERO).min(BigDecimal.ONE);
    }

    public CreditLimitRecommendation generateCreditLimitRecommendation(Customer customer) {
        String action;
        Money recommendedLimit;
        String rationale;
        
        BigDecimal riskScore = calculateCustomerRiskScore(customer);
        BigDecimal utilization = customer.getCreditUtilizationRatio();
        Integer creditScore = customer.getCreditScore();
        
        if (creditScore != null && creditScore >= 750 && utilization.compareTo(new BigDecimal("0.30")) < 0) {
            action = "INCREASE";
            recommendedLimit = customer.getCreditLimit().multiply(new BigDecimal("1.25"));
            rationale = "Excellent payment history and low utilization";
        } else if (creditScore != null && creditScore >= 650 && utilization.compareTo(new BigDecimal("0.70")) < 0) {
            action = "MAINTAIN";
            recommendedLimit = customer.getCreditLimit();
            rationale = "Current utilization within acceptable range";
        } else {
            action = "DECREASE";
            recommendedLimit = customer.getCreditLimit().multiply(new BigDecimal("0.80"));
            rationale = "High utilization or poor credit score indicates increased risk";
        }
        
        return new CreditLimitRecommendation(
            customer.getId(),
            action,
            recommendedLimit,
            rationale
        );
    }

    public Map<String, ConcentrationRiskMetrics> calculateConcentrationRiskByCreditScoreBands(List<Customer> customers) {
        Map<String, List<Customer>> bands = new HashMap<>();
        bands.put("EXCELLENT", new ArrayList<>());
        bands.put("GOOD", new ArrayList<>());
        bands.put("FAIR", new ArrayList<>());
        bands.put("POOR", new ArrayList<>());
        
        for (Customer customer : customers) {
            if (customer.getCreditScore() == null) {
                bands.get("POOR").add(customer);
            } else if (customer.getCreditScore() >= 750) {
                bands.get("EXCELLENT").add(customer);
            } else if (customer.getCreditScore() >= 700) {
                bands.get("GOOD").add(customer);
            } else if (customer.getCreditScore() >= 650) {
                bands.get("FAIR").add(customer);
            } else {
                bands.get("POOR").add(customer);
            }
        }
        
        Map<String, ConcentrationRiskMetrics> result = new HashMap<>();
        for (Map.Entry<String, List<Customer>> entry : bands.entrySet()) {
            List<Customer> bandCustomers = entry.getValue();
            
            if (bandCustomers.isEmpty()) {
                result.put(entry.getKey(), new ConcentrationRiskMetrics(
                    0,
                    Money.zero("USD"),
                    BigDecimal.ZERO
                ));
                continue;
            }
            
            String currency = bandCustomers.get(0).getCreditLimit().getCurrency();
            BigDecimal totalExposure = bandCustomers.stream()
                .map(customer -> {
                    Money creditLimit = customer.getCreditLimit();
                    Money availableCredit = customer.getAvailableCredit();
                    return creditLimit.subtract(availableCredit).getAmount();
                })
                .reduce(BigDecimal.ZERO, BigDecimal::add);
            
            BigDecimal averageUtilization = bandCustomers.stream()
                .map(Customer::getCreditUtilizationRatio)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .divide(BigDecimal.valueOf(bandCustomers.size()), 2, RoundingMode.HALF_UP);
            
            result.put(entry.getKey(), new ConcentrationRiskMetrics(
                bandCustomers.size(),
                Money.of(totalExposure, currency),
                averageUtilization
            ));
        }
        
        return result;
    }

    public StressTestResult performStressTest(List<Customer> customers, StressTestScenario scenario) {
        if (customers.isEmpty()) {
            return new StressTestResult(
                scenario.name(),
                Money.zero("USD"),
                0,
                Arrays.asList("No customers to stress test")
            );
        }
        
        String currency = customers.get(0).getCreditLimit().getCurrency();
        BigDecimal projectedLossAmount = BigDecimal.ZERO;
        int affectedCustomers = 0;
        
        for (Customer customer : customers) {
            BigDecimal baseDefaultProbability = predictDefaultProbability(customer);
            BigDecimal stressedDefaultProbability = baseDefaultProbability.add(scenario.defaultRateIncrease());
            
            if (stressedDefaultProbability.compareTo(new BigDecimal("0.15")) > 0) {
                affectedCustomers++;
                Money exposure = customer.getCreditLimit().subtract(customer.getAvailableCredit());
                BigDecimal expectedLoss = exposure.getAmount().multiply(stressedDefaultProbability);
                projectedLossAmount = projectedLossAmount.add(expectedLoss);
            }
        }
        
        List<String> recommendedActions = generateStressTestRecommendations(affectedCustomers, customers.size());
        
        return new StressTestResult(
            scenario.name(),
            Money.of(projectedLossAmount, currency),
            affectedCustomers,
            recommendedActions
        );
    }

    private List<String> generateStressTestRecommendations(int affectedCustomers, int totalCustomers) {
        List<String> recommendations = new ArrayList<>();
        
        if (affectedCustomers == 0) {
            recommendations.add("Portfolio appears resilient to stress scenario");
            recommendations.add("Continue monitoring market conditions");
        } else {
            double affectedPercentage = (double) affectedCustomers / totalCustomers;
            
            if (affectedPercentage > 0.20) {
                recommendations.add("Consider implementing emergency credit tightening measures");
                recommendations.add("Increase provisioning for potential losses");
            }
            
            recommendations.add("Monitor high-risk customers closely");
            recommendations.add("Review and potentially reduce credit limits for poor credit scores");
            recommendations.add("Enhance collection procedures");
            
            if (affectedPercentage > 0.10) {
                recommendations.add("Consider stress testing more frequently");
            }
        }
        
        return recommendations;
    }

    // Record classes for data structures
    public record PortfolioPerformanceMetrics(
        int totalCustomers,
        Money totalCreditLimit,
        Money totalAvailableCredit,
        BigDecimal averageCreditScore,
        BigDecimal portfolioUtilizationRate
    ) {}

    public record CreditLimitRecommendation(
        Long customerId,
        String recommendedAction,
        Money recommendedLimit,
        String rationale
    ) {}

    public record ConcentrationRiskMetrics(
        int customerCount,
        Money totalExposure,
        BigDecimal averageUtilization
    ) {}

    public record StressTestScenario(
        String name,
        BigDecimal defaultRateIncrease,
        BigDecimal creditScoreDecrease
    ) {}

    public record StressTestResult(
        String scenarioName,
        Money projectedLosses,
        int affectedCustomers,
        List<String> recommendedActions
    ) {}
}