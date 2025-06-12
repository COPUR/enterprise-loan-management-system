package com.bank.loanmanagement.graphql.config;

import graphql.scalars.ExtendedScalars;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.graphql.execution.RuntimeWiringConfigurer;
import org.springframework.graphql.server.WebGraphQlHandler;
import org.springframework.graphql.server.webmvc.GraphQlWebMvcConfigurer;
import org.springframework.web.servlet.function.RouterFunction;
import org.springframework.web.servlet.function.ServerResponse;
import org.springframework.beans.factory.annotation.Autowired;
import com.bank.loanmanagement.graphql.resolver.*;
import com.bank.loanmanagement.graphql.service.NaturalLanguageProcessingService;

import static org.springframework.web.servlet.function.RouterFunctions.route;
import static org.springframework.web.servlet.function.RequestPredicates.GET;

@Configuration
public class GraphQLConfig implements GraphQlWebMvcConfigurer {

    @Autowired
    private CustomerQueryResolver customerQueryResolver;
    
    @Autowired
    private LoanQueryResolver loanQueryResolver;
    
    @Autowired
    private PaymentQueryResolver paymentQueryResolver;
    
    @Autowired
    private SystemQueryResolver systemQueryResolver;
    
    @Autowired
    private AnalyticsQueryResolver analyticsQueryResolver;
    
    @Autowired
    private CustomerMutationResolver customerMutationResolver;
    
    @Autowired
    private LoanMutationResolver loanMutationResolver;
    
    @Autowired
    private PaymentMutationResolver paymentMutationResolver;
    
    @Autowired
    private SagaMutationResolver sagaMutationResolver;
    
    @Autowired
    private NaturalLanguageProcessingService nlpService;

    @Bean
    public RuntimeWiringConfigurer runtimeWiringConfigurer() {
        return wiringBuilder -> wiringBuilder
                // Extended scalars for banking data types
                .scalar(ExtendedScalars.Date)
                .scalar(ExtendedScalars.DateTime)
                .scalar(ExtendedScalars.BigDecimal)
                .scalar(ExtendedScalars.JSON)
                .scalar(ExtendedScalars.PositiveBigDecimal)
                .scalar(ExtendedScalars.NonNegativeBigDecimal)
                
                // Query resolvers
                .type("Query", builder -> builder
                    // Customer operations
                    .dataFetcher("customer", customerQueryResolver::getCustomer)
                    .dataFetcher("customers", customerQueryResolver::getCustomers)
                    .dataFetcher("customerCreditHistory", customerQueryResolver::getCustomerCreditHistory)
                    
                    // Loan operations
                    .dataFetcher("loan", loanQueryResolver::getLoan)
                    .dataFetcher("loans", loanQueryResolver::getLoans)
                    .dataFetcher("loansByCustomer", loanQueryResolver::getLoansByCustomer)
                    .dataFetcher("loanInstallments", loanQueryResolver::getLoanInstallments)
                    
                    // Payment operations
                    .dataFetcher("payment", paymentQueryResolver::getPayment)
                    .dataFetcher("payments", paymentQueryResolver::getPayments)
                    .dataFetcher("paymentsByLoan", paymentQueryResolver::getPaymentsByLoan)
                    .dataFetcher("paymentCalculation", paymentQueryResolver::calculatePayment)
                    
                    // Analytics and reporting
                    .dataFetcher("customerAnalytics", analyticsQueryResolver::getCustomerAnalytics)
                    .dataFetcher("loanAnalytics", analyticsQueryResolver::getLoanAnalytics)
                    .dataFetcher("paymentAnalytics", analyticsQueryResolver::getPaymentAnalytics)
                    .dataFetcher("riskAssessment", analyticsQueryResolver::getRiskAssessment)
                    
                    // System monitoring
                    .dataFetcher("systemHealth", systemQueryResolver::getSystemHealth)
                    .dataFetcher("circuitBreakerStatus", systemQueryResolver::getCircuitBreakerStatus)
                    .dataFetcher("sagaStates", systemQueryResolver::getSagaStates)
                    .dataFetcher("businessRules", systemQueryResolver::getBusinessRules)
                    .dataFetcher("interestRates", systemQueryResolver::getInterestRates)
                    
                    // Natural language queries for LLM integration
                    .dataFetcher("nlQuery", this::handleNaturalLanguageQuery)
                    .dataFetcher("recommendations", analyticsQueryResolver::getRecommendations)
                )
                
                // Mutation resolvers
                .type("Mutation", builder -> builder
                    // Customer mutations
                    .dataFetcher("createCustomer", customerMutationResolver::createCustomer)
                    .dataFetcher("updateCustomer", customerMutationResolver::updateCustomer)
                    .dataFetcher("reserveCredit", customerMutationResolver::reserveCredit)
                    .dataFetcher("releaseCredit", customerMutationResolver::releaseCredit)
                    
                    // Loan mutations
                    .dataFetcher("createLoan", loanMutationResolver::createLoan)
                    .dataFetcher("approveLoan", loanMutationResolver::approveLoan)
                    .dataFetcher("rejectLoan", loanMutationResolver::rejectLoan)
                    
                    // Payment mutations
                    .dataFetcher("processPayment", paymentMutationResolver::processPayment)
                    .dataFetcher("schedulePayment", paymentMutationResolver::schedulePayment)
                    
                    // SAGA operations
                    .dataFetcher("initiateLoanCreationSaga", sagaMutationResolver::initiateLoanCreationSaga)
                    .dataFetcher("compensateSaga", sagaMutationResolver::compensateSaga)
                    
                    // Bulk operations
                    .dataFetcher("bulkPaymentProcessing", paymentMutationResolver::bulkPaymentProcessing)
                    .dataFetcher("bulkLoanStatusUpdate", loanMutationResolver::bulkLoanStatusUpdate)
                )
                
                // Field resolvers for complex types
                .type("Customer", builder -> builder
                    .dataFetcher("fullName", env -> {
                        Customer customer = env.getSource();
                        return customer.getFirstName() + " " + customer.getLastName();
                    })
                    .dataFetcher("loans", customerQueryResolver::getCustomerLoans)
                    .dataFetcher("payments", customerQueryResolver::getCustomerPayments)
                    .dataFetcher("creditHistory", customerQueryResolver::getCustomerCreditHistoryField)
                    .dataFetcher("riskProfile", customerQueryResolver::getCustomerRiskProfile)
                )
                
                .type("Loan", builder -> builder
                    .dataFetcher("customer", loanQueryResolver::getLoanCustomer)
                    .dataFetcher("installments", loanQueryResolver::getLoanInstallmentsField)
                    .dataFetcher("payments", loanQueryResolver::getLoanPayments)
                    .dataFetcher("documents", loanQueryResolver::getLoanDocuments)
                    .dataFetcher("paymentHistory", loanQueryResolver::getLoanPaymentHistory)
                    .dataFetcher("nextInstallment", loanQueryResolver::getNextInstallment)
                    .dataFetcher("overdueAmount", loanQueryResolver::getOverdueAmount)
                    .dataFetcher("daysOverdue", loanQueryResolver::getDaysOverdue)
                )
                
                .type("Payment", builder -> builder
                    .dataFetcher("loan", paymentQueryResolver::getPaymentLoan)
                    .dataFetcher("customer", paymentQueryResolver::getPaymentCustomer)
                    .dataFetcher("installmentPayments", paymentQueryResolver::getInstallmentPayments)
                    .dataFetcher("calculation", paymentQueryResolver::getPaymentCalculationResult)
                );
    }

    @Bean
    public RouterFunction<ServerResponse> graphqlRouterFunction() {
        return route(GET("/graphql/playground"), 
            request -> ServerResponse.ok()
                .header("Content-Type", "text/html")
                .body(getGraphQLPlaygroundHtml()));
    }

    private Object handleNaturalLanguageQuery(graphql.schema.DataFetchingEnvironment environment) {
        String query = environment.getArgument("query");
        Object context = environment.getArgument("context");
        
        return nlpService.processNaturalLanguageQuery(query, context);
    }

    private String getGraphQLPlaygroundHtml() {
        return """
            <!DOCTYPE html>
            <html>
            <head>
                <meta charset=utf-8/>
                <meta name="viewport" content="user-scalable=no, initial-scale=1.0, minimum-scale=1.0, maximum-scale=1.0, minimal-ui">
                <title>Enterprise Loan Management - GraphQL Playground</title>
                <link rel="stylesheet" href="//cdn.jsdelivr.net/npm/graphql-playground-react/build/static/css/index.css" />
                <link rel="shortcut icon" href="//cdn.jsdelivr.net/npm/graphql-playground-react/build/favicon.png" />
                <script src="//cdn.jsdelivr.net/npm/graphql-playground-react/build/static/js/middleware.js"></script>
            </head>
            <body>
                <div id="root">
                    <style>
                        body {
                            background-color: rgb(23, 42, 58);
                            font-family: Open Sans, sans-serif;
                            height: 90vh;
                        }
                        #root {
                            height: 100%;
                            width: 100%;
                            display: flex;
                            align-items: center;
                            justify-content: center;
                        }
                        .loading {
                            font-size: 32px;
                            font-weight: 200;
                            color: rgba(255, 255, 255, .6);
                            margin-left: 20px;
                        }
                        img {
                            width: 78px;
                            height: 78px;
                        }
                        .title {
                            font-weight: 400;
                        }
                    </style>
                    <img src="//cdn.jsdelivr.net/npm/graphql-playground-react/build/logo.png" alt="">
                    <div class="loading"> Loading
                        <span class="title">Enterprise Loan Management GraphQL Playground</span>
                    </div>
                </div>
                <script>window.addEventListener('load', function (event) {
                    GraphQLPlayground.init(document.getElementById('root'), {
                        endpoint: '/graphql',
                        subscriptionEndpoint: '/graphql',
                        settings: {
                            'editor.theme': 'dark',
                            'editor.fontSize': 14,
                            'editor.fontFamily': '"Source Code Pro", "Consolas", "Inconsolata", "Droid Sans Mono", "Monaco", monospace',
                            'editor.reuseHeaders': true,
                            'tracing.hideTracingResponse': true,
                            'queryPlan.hideQueryPlanResponse': true,
                            'editor.cursorShape': 'line',
                            'request.credentials': 'omit',
                        },
                        tabs: [{
                            endpoint: '/graphql',
                            query: `# Enterprise Loan Management System GraphQL API
# Welcome to the comprehensive banking platform GraphQL interface
# Optimized for MCP and LLM integration

# Example: Get customer with loans and payments
query GetCustomerData {
  customer(id: "1") {
    customerId
    fullName
    email
    creditLimit
    availableCredit
    accountStatus
    loans {
      loanId
      loanAmount
      outstandingAmount
      status
      installments {
        installmentNumber
        dueDate
        totalAmount
        status
      }
    }
    payments {
      paymentId
      paymentAmount
      paymentDate
      status
    }
  }
}

# Example: Natural language query for LLM integration
query NaturalLanguageExample {
  nlQuery(
    query: "Show me all overdue loans for customers with high risk"
    context: {
      domain: RISK_MANAGEMENT
      language: "en"
    }
  ) {
    intent
    result
    confidence
    suggestions
  }
}

# Example: Get analytics for decision making
query AnalyticsExample {
  loanAnalytics(period: LAST_30_DAYS) {
    totalLoansCreated
    totalLoanAmount
    approvalRate
    defaultRate
    loanTypeDistribution {
      loanType
      count
      totalAmount
    }
  }
}

# Example: Create a new loan (mutation)
mutation CreateLoanExample {
  createLoan(input: {
    customerId: "1"
    loanAmount: 25000.00
    interestRate: 0.15
    installmentCount: 12
    loanType: PERSONAL
    purpose: "Home improvement"
  }) {
    ... on LoanSuccess {
      loan {
        loanId
        loanAmount
        installmentAmount
        status
      }
      message
    }
    ... on LoanError {
      message
      code
      field
    }
  }
}`
                        }]
                    })
                })</script>
            </body>
            </html>
            """;
    }
}