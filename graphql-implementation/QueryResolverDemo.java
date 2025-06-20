package com.bank.loanmanagement.infrastructure.graphql;

import com.bank.loanmanagement.domain.loan.*;
import com.bank.loanmanagement.domain.customer.CustomerId;
import com.bank.loanmanagement.infrastructure.graphql.dto.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.stereotype.Controller;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * GraphQL Query resolver with demo implementations
 * Following hexagonal architecture principles with clean separation
 */
@Controller
@RequiredArgsConstructor
@Slf4j
public class QueryResolverDemo {

    private final LoanRecommendationUseCase loanRecommendationUseCase;

    @QueryMapping
    public CustomerGraphQL customer(@Argument String id) {
        log.info("GraphQL Query: customer(id={})", id);
        
        return CustomerGraphQL.builder()
                .id(id)
                .customerId("CUST-" + id)
                .firstName("John")
                .lastName("Doe")
                .fullName("John Doe")
                .email("john.doe@example.com")
                .phone("+1-555-0123")
                .dateOfBirth(LocalDate.of(1985, 5, 15))
                .creditLimit(new BigDecimal("50000"))
                .availableCredit(new BigDecimal("35000"))
                .usedCredit(new BigDecimal("15000"))
                .annualIncome(new BigDecimal("75000"))
                .employmentStatus("EMPLOYED")
                .creditScore(720)
                .accountStatus("ACTIVE")
                .riskLevel("LOW")
                .createdAt(LocalDateTime.now().minusYears(2))
                .updatedAt(LocalDateTime.now())
                .build();
    }

    @QueryMapping
    public CustomerConnectionGraphQL customers(@Argument CustomerFilterGraphQL filter, @Argument PageInputGraphQL page) {
        log.info("GraphQL Query: customers with filter and pagination");
        
        List<CustomerGraphQL> customers = List.of(
                createSampleCustomer("1", "John", "Doe"),
                createSampleCustomer("2", "Jane", "Smith"),
                createSampleCustomer("3", "Bob", "Johnson")
        );

        return CustomerConnectionGraphQL.builder()
                .nodes(customers)
                .totalCount(customers.size())
                .pageInfo(PageInfoGraphQL.builder()
                        .hasNextPage(false)
                        .hasPreviousPage(false)
                        .build())
                .build();
    }

    @QueryMapping
    public LoanGraphQL loan(@Argument String id) {
        log.info("GraphQL Query: loan(id={})", id);
        
        return LoanGraphQL.builder()
                .id(id)
                .loanId("LOAN-" + id)
                .loanAmount(new BigDecimal("25000"))
                .outstandingAmount(new BigDecimal("18500"))
                .interestRate(new BigDecimal("7.25"))
                .installmentCount(36)
                .installmentAmount(new BigDecimal("780.45"))
                .totalRepaymentAmount(new BigDecimal("28096.20"))
                .loanType("PERSONAL")
                .purpose("Home improvement")
                .status("ACTIVE")
                .applicationDate(LocalDateTime.now().minusMonths(12))
                .approvalDate(LocalDateTime.now().minusMonths(12).plusDays(3))
                .disbursementDate(LocalDateTime.now().minusMonths(12).plusDays(5))
                .maturityDate(LocalDate.now().plusMonths(24))
                .overdueAmount(BigDecimal.ZERO)
                .daysOverdue(0)
                .build();
    }

    @QueryMapping
    public List<LoanRecommendationGraphQL> recommendations(@Argument String customerId, @Argument String type) {
        log.info("GraphQL Query: recommendations for customer {} of type {}", customerId, type);
        
        // Create a simple recommendation command for demonstration
        LoanRecommendationCommand command = new LoanRecommendationCommand(
                new CustomerId(customerId),
                com.bank.loanmanagement.domain.shared.Money.of(new BigDecimal("50000")),
                createDemoFinancialProfile(),
                LoanType.PERSONAL,
                List.of()
        );

        LoanRecommendationResult result = loanRecommendationUseCase.generateRecommendations(command);
        
        return result.getOffers().stream()
                .map(this::toLoanRecommendationGraphQL)
                .toList();
    }

    @QueryMapping
    public SystemHealthGraphQL systemHealth() {
        log.info("GraphQL Query: systemHealth");
        
        return SystemHealthGraphQL.builder()
                .status("UP")
                .timestamp(LocalDateTime.now())
                .services(List.of(
                        ServiceHealthGraphQL.builder()
                                .serviceName("loan-service")
                                .status("UP")
                                .port(8080)
                                .responseTime(25.5)
                                .errorRate(0.01)
                                .build(),
                        ServiceHealthGraphQL.builder()
                                .serviceName("payment-service")
                                .status("UP")
                                .port(8081)
                                .responseTime(18.2)
                                .errorRate(0.005)
                                .build()
                ))
                .database(DatabaseHealthGraphQL.builder()
                        .status("UP")
                        .connectionCount(15)
                        .responseTime(5.2)
                        .build())
                .cache(CacheHealthGraphQL.builder()
                        .status("UP")
                        .hitRate(0.85)
                        .memoryUsage(0.62)
                        .build())
                .metrics(SystemMetricsGraphQL.builder()
                        .cpuUsage(0.45)
                        .memoryUsage(0.68)
                        .activeConnections(23)
                        .requestsPerSecond(125.5)
                        .build())
                .build();
    }

    @QueryMapping
    public RiskAssessmentGraphQL riskAssessment(@Argument String customerId) {
        log.info("GraphQL Query: riskAssessment for customer {}", customerId);
        
        return RiskAssessmentGraphQL.builder()
                .customerId(customerId)
                .overallRiskScore(35.5)
                .creditRisk(25.0)
                .incomeRisk(15.0)
                .behavioralRisk(20.0)
                .marketRisk(10.0)
                .riskFactors(List.of(
                        "Moderate debt-to-income ratio: 32.5%",
                        "Recent credit inquiry activity"
                ))
                .recommendations(List.of(
                        "Consider debt consolidation",
                        "Maintain current payment schedule"
                ))
                .nextReviewDate(LocalDate.now().plusMonths(6))
                .build();
    }

    // Helper methods
    private CustomerGraphQL createSampleCustomer(String id, String firstName, String lastName) {
        return CustomerGraphQL.builder()
                .id(id)
                .customerId("CUST-" + id)
                .firstName(firstName)
                .lastName(lastName)
                .fullName(firstName + " " + lastName)
                .email(firstName.toLowerCase() + "." + lastName.toLowerCase() + "@example.com")
                .creditLimit(new BigDecimal("40000"))
                .availableCredit(new BigDecimal("30000"))
                .usedCredit(new BigDecimal("10000"))
                .employmentStatus("EMPLOYED")
                .creditScore(700 + Integer.parseInt(id) * 10)
                .accountStatus("ACTIVE")
                .riskLevel("LOW")
                .createdAt(LocalDateTime.now().minusYears(1))
                .updatedAt(LocalDateTime.now())
                .build();
    }

    private LoanRecommendationGraphQL toLoanRecommendationGraphQL(LoanOffer offer) {
        return LoanRecommendationGraphQL.builder()
                .type(offer.getLoanType().name())
                .title("Recommended " + offer.getLoanType().name().toLowerCase() + " loan")
                .description(offer.getDescription())
                .priority("HIGH")
                .impact("Potential savings of $" + offer.calculateMonthlySavings().getAmount())
                .actionRequired(true)
                .estimatedBenefit("Lower interest rate")
                .implementationEffort("LOW")
                .build();
    }

    private CustomerFinancialProfile createDemoFinancialProfile() {
        return new CustomerFinancialProfile(
                com.bank.loanmanagement.domain.shared.Money.of(new BigDecimal("75000")),
                com.bank.loanmanagement.domain.shared.Money.of(new BigDecimal("25000")),
                com.bank.loanmanagement.domain.customer.CreditScore.of(720),
                com.bank.loanmanagement.domain.customer.EmploymentStatus.EMPLOYED
        );
    }
}