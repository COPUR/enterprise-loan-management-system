package com.bank.loanmanagement.performance;

import com.bank.loanmanagement.customermanagement.domain.model.Customer;
import com.bank.loanmanagement.domain.loan.LoanApplication;
import com.bank.loanmanagement.domain.payment.Payment;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.gatling.javaapi.core.*;
import io.gatling.javaapi.http.*;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

import java.time.Duration;
import java.util.concurrent.ThreadLocalRandom;

import static io.gatling.javaapi.core.CoreDsl.*;
import static io.gatling.javaapi.http.HttpDsl.*;

/**
 * Comprehensive Load Test Suite for Enterprise Loan Management System
 * Tests all microservices under realistic banking load conditions
 * Includes PCI-DSS compliance validation under stress
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestPropertySource(properties = {
    "spring.profiles.active=load-test",
    "logging.level.com.bank.loanmanagement=INFO",
    "management.metrics.export.prometheus.enabled=true"
})
public class LoadTestSuite extends Simulation {

    // Load Test Configuration
    private static final String BASE_URL = System.getProperty("load.test.url", "https://localhost:8080");
    private static final int CONCURRENT_USERS = Integer.parseInt(System.getProperty("load.test.users", "1000"));
    private static final Duration RAMP_UP_DURATION = Duration.parse(System.getProperty("load.test.rampup", "PT5M"));
    private static final Duration TEST_DURATION = Duration.parse(System.getProperty("load.test.duration", "PT15M"));
    
    // Banking Load Patterns
    private static final int PEAK_HOUR_MULTIPLIER = 3;
    private static final int BUSINESS_HOURS_BASE_LOAD = CONCURRENT_USERS;
    private static final int OFF_HOURS_BASE_LOAD = CONCURRENT_USERS / 4;

    private final HttpProtocolBuilder httpProtocol = http
        .baseUrl(BASE_URL)
        .acceptHeader("application/json")
        .contentTypeHeader("application/json")
        .userAgentHeader("LoadTest/1.0 (Banking Performance Suite)")
        .header("X-Test-Type", "load-test")
        .header("X-Compliance-Test", "PCI-DSS-v4")
        // Banking security headers
        .header("X-Frame-Options", "DENY")
        .header("X-Content-Type-Options", "nosniff")
        .header("Strict-Transport-Security", "max-age=31536000; includeSubDomains")
        .check(status().not(500), status().not(502), status().not(503))
        .disableFollowRedirect();

    // Customer Management Load Test Scenarios
    private final ScenarioBuilder customerManagementScenario = scenario("Customer Management Load Test")
        .during(TEST_DURATION).on(
            exec(session -> {
                session.set("customerId", "CUST_" + ThreadLocalRandom.current().nextInt(1000000));
                session.set("correlationId", java.util.UUID.randomUUID().toString());
                return session;
            })
            .pace(Duration.ofSeconds(2))
            .group("Customer Operations").on(
                // Create Customer (10% of operations)
                randomSwitch().on(
                    Choice.withWeight(10.0, exec(createCustomerRequest())),
                    Choice.withWeight(30.0, exec(getCustomerRequest())),
                    Choice.withWeight(25.0, exec(updateCustomerRequest())),
                    Choice.withWeight(20.0, exec(checkCustomerEligibilityRequest())),
                    Choice.withWeight(10.0, exec(deactivateCustomerRequest())),
                    Choice.withWeight(5.0, exec(kycVerificationRequest()))
                )
            )
        );

    // Loan Processing Load Test Scenarios
    private final ScenarioBuilder loanProcessingScenario = scenario("Loan Processing Load Test")
        .during(TEST_DURATION).on(
            exec(session -> {
                session.set("loanId", "LOAN_" + ThreadLocalRandom.current().nextInt(1000000));
                session.set("applicationId", "APP_" + ThreadLocalRandom.current().nextInt(1000000));
                return session;
            })
            .pace(Duration.ofSeconds(3))
            .group("Loan Operations").on(
                randomSwitch().on(
                    Choice.withWeight(15.0, exec(submitLoanApplicationRequest())),
                    Choice.withWeight(20.0, exec(getLoanStatusRequest())),
                    Choice.withWeight(10.0, exec(approveLoanRequest())),
                    Choice.withWeight(5.0, exec(rejectLoanRequest())),
                    Choice.withWeight(25.0, exec(calculateLoanOffersRequest())),
                    Choice.withWeight(15.0, exec(getLoanDetailsRequest())),
                    Choice.withWeight(10.0, exec(updateLoanTermsRequest()))
                )
            )
        );

    // Payment Processing Load Test Scenarios
    private final ScenarioBuilder paymentProcessingScenario = scenario("Payment Processing Load Test")
        .during(TEST_DURATION).on(
            exec(session -> {
                session.set("paymentId", "PAY_" + ThreadLocalRandom.current().nextInt(1000000));
                session.set("transactionId", "TXN_" + ThreadLocalRandom.current().nextInt(1000000));
                return session;
            })
            .pace(Duration.ofSeconds(1))
            .group("Payment Operations").on(
                randomSwitch().on(
                    Choice.withWeight(40.0, exec(processPaymentRequest())),
                    Choice.withWeight(20.0, exec(getPaymentStatusRequest())),
                    Choice.withWeight(15.0, exec(refundPaymentRequest())),
                    Choice.withWeight(10.0, exec(validatePaymentRequest())),
                    Choice.withWeight(10.0, exec(getPaymentHistoryRequest())),
                    Choice.withWeight(5.0, exec(cancelPaymentRequest()))
                )
            )
        );

    // High-Frequency Monitoring Scenario
    private final ScenarioBuilder monitoringScenario = scenario("System Monitoring Load Test")
        .during(TEST_DURATION).on(
            pace(Duration.ofSeconds(5))
            .group("Monitoring Operations").on(
                randomSwitch().on(
                    Choice.withWeight(30.0, exec(healthCheckRequest())),
                    Choice.withWeight(25.0, exec(metricsRequest())),
                    Choice.withWeight(20.0, exec(systemInfoRequest())),
                    Choice.withWeight(15.0, exec(complianceStatusRequest())),
                    Choice.withWeight(10.0, exec(performanceStatsRequest()))
                )
            )
        );

    // Fraud Detection Scenario
    private final ScenarioBuilder fraudDetectionScenario = scenario("Fraud Detection Load Test")
        .during(TEST_DURATION).on(
            pace(Duration.ofSeconds(1))
            .group("Fraud Detection").on(
                randomSwitch().on(
                    Choice.withWeight(70.0, exec(normalTransactionRequest())),
                    Choice.withWeight(20.0, exec(suspiciousTransactionRequest())),
                    Choice.withWeight(10.0, exec(fraudulentTransactionRequest()))
                )
            )
        );

    // Load Test Execution Setup
    {
        setUp(
            // Business Hours Load Pattern
            customerManagementScenario.injectOpen(
                rampUsers(BUSINESS_HOURS_BASE_LOAD).during(RAMP_UP_DURATION),
                constantUsersPerSec(BUSINESS_HOURS_BASE_LOAD / 60).during(TEST_DURATION)
            ),
            
            // Loan Processing Load (Lower frequency, higher complexity)
            loanProcessingScenario.injectOpen(
                rampUsers(BUSINESS_HOURS_BASE_LOAD / 2).during(RAMP_UP_DURATION),
                constantUsersPerSec(BUSINESS_HOURS_BASE_LOAD / 120).during(TEST_DURATION)
            ),
            
            // Payment Processing Load (High frequency)
            paymentProcessingScenario.injectOpen(
                rampUsers(BUSINESS_HOURS_BASE_LOAD * 2).during(RAMP_UP_DURATION),
                constantUsersPerSec(BUSINESS_HOURS_BASE_LOAD / 30).during(TEST_DURATION)
            ),
            
            // Monitoring Load (Continuous)
            monitoringScenario.injectOpen(
                atOnceUsers(50),
                constantUsersPerSec(10).during(TEST_DURATION)
            ),
            
            // Fraud Detection Load
            fraudDetectionScenario.injectOpen(
                rampUsers(100).during(Duration.ofMinutes(2)),
                constantUsersPerSec(20).during(TEST_DURATION)
            )
        ).protocols(httpProtocol)
        .assertions(
            // Performance SLA Assertions
            global().responseTime().percentile3().lt(2000),    // 99.9th percentile < 2s
            global().responseTime().percentile2().lt(1000),    // 99th percentile < 1s
            global().responseTime().mean().lt(500),            // Mean response time < 500ms
            global().successfulRequests().percent().gt(99.5),  // 99.5% success rate
            
            // Business Critical Operations
            forAll().responseTime().percentile3().lt(5000),    // No operation > 5s
            details("Customer Operations").responseTime().percentile2().lt(1500),
            details("Loan Operations").responseTime().percentile2().lt(3000),
            details("Payment Operations").responseTime().percentile2().lt(800),
            
            // PCI-DSS Compliance SLA
            details("Payment Operations").successfulRequests().percent().gt(99.9),
            details("Fraud Detection").responseTime().mean().lt(200)
        );
    }

    // Customer Management HTTP Requests
    private ChainBuilder createCustomerRequest() {
        return exec(http("Create Customer")
            .post("/api/v1/customers")
            .header("X-Correlation-ID", "#{correlationId}")
            .body(StringBody("""
                {
                    "personalDetails": {
                        "firstName": "LoadTest",
                        "lastName": "Customer#{customerId}",
                        "email": "loadtest#{customerId}@bank.com",
                        "phone": "+1555#{customerId}",
                        "dateOfBirth": "1990-01-01"
                    },
                    "address": {
                        "street": "123 Test Street",
                        "city": "TestCity",
                        "state": "TS",
                        "postalCode": "12345",
                        "country": "US"
                    },
                    "customerType": "INDIVIDUAL"
                }
                """))
            .check(status().is(201))
            .check(jsonPath("$.customerId").saveAs("createdCustomerId")));
    }

    private ChainBuilder getCustomerRequest() {
        return exec(http("Get Customer")
            .get("/api/v1/customers/#{customerId}")
            .header("X-Correlation-ID", "#{correlationId}")
            .check(status().in(200, 404)));
    }

    private ChainBuilder updateCustomerRequest() {
        return exec(http("Update Customer")
            .put("/api/v1/customers/#{customerId}")
            .header("X-Correlation-ID", "#{correlationId}")
            .body(StringBody("""
                {
                    "personalDetails": {
                        "email": "updated#{customerId}@bank.com",
                        "phone": "+1555#{customerId}"
                    }
                }
                """))
            .check(status().in(200, 404)));
    }

    private ChainBuilder checkCustomerEligibilityRequest() {
        return exec(http("Check Customer Eligibility")
            .post("/api/v1/customers/#{customerId}/eligibility")
            .header("X-Correlation-ID", "#{correlationId}")
            .body(StringBody("""
                {
                    "loanType": "PERSONAL",
                    "requestedAmount": {
                        "value": 25000.00,
                        "currency": "USD"
                    }
                }
                """))
            .check(status().in(200, 404)));
    }

    private ChainBuilder deactivateCustomerRequest() {
        return exec(http("Deactivate Customer")
            .delete("/api/v1/customers/#{customerId}")
            .header("X-Correlation-ID", "#{correlationId}")
            .check(status().in(200, 404)));
    }

    private ChainBuilder kycVerificationRequest() {
        return exec(http("KYC Verification")
            .post("/api/v1/customers/#{customerId}/kyc")
            .header("X-Correlation-ID", "#{correlationId}")
            .body(StringBody("""
                {
                    "documentType": "DRIVERS_LICENSE",
                    "documentNumber": "DL#{customerId}",
                    "verificationMethod": "AUTOMATED"
                }
                """))
            .check(status().in(200, 404)));
    }

    // Loan Processing HTTP Requests
    private ChainBuilder submitLoanApplicationRequest() {
        return exec(http("Submit Loan Application")
            .post("/api/v1/loans/applications")
            .header("X-Correlation-ID", "#{correlationId}")
            .body(StringBody("""
                {
                    "customerId": "#{customerId}",
                    "loanType": "PERSONAL",
                    "requestedAmount": {
                        "value": #{randomAmount},
                        "currency": "USD"
                    },
                    "termMonths": 24,
                    "purpose": "DEBT_CONSOLIDATION"
                }
                """.replace("#{randomAmount}", String.valueOf(ThreadLocalRandom.current().nextInt(5000, 50000)))))
            .check(status().is(201))
            .check(jsonPath("$.applicationId").saveAs("applicationId")));
    }

    private ChainBuilder getLoanStatusRequest() {
        return exec(http("Get Loan Status")
            .get("/api/v1/loans/#{loanId}/status")
            .header("X-Correlation-ID", "#{correlationId}")
            .check(status().in(200, 404)));
    }

    private ChainBuilder approveLoanRequest() {
        return exec(http("Approve Loan")
            .post("/api/v1/loans/#{loanId}/approve")
            .header("X-Correlation-ID", "#{correlationId}")
            .body(StringBody("""
                {
                    "approvedAmount": {
                        "value": 20000.00,
                        "currency": "USD"
                    },
                    "interestRate": 0.0549,
                    "termMonths": 24
                }
                """))
            .check(status().in(200, 404)));
    }

    private ChainBuilder rejectLoanRequest() {
        return exec(http("Reject Loan")
            .post("/api/v1/loans/#{loanId}/reject")
            .header("X-Correlation-ID", "#{correlationId}")
            .body(StringBody("""
                {
                    "reason": "INSUFFICIENT_CREDIT_SCORE",
                    "details": "Credit score below minimum threshold"
                }
                """))
            .check(status().in(200, 404)));
    }

    private ChainBuilder calculateLoanOffersRequest() {
        return exec(http("Calculate Loan Offers")
            .post("/api/v1/loans/offers")
            .header("X-Correlation-ID", "#{correlationId}")
            .body(StringBody("""
                {
                    "customerId": "#{customerId}",
                    "requestedAmount": {
                        "value": 30000.00,
                        "currency": "USD"
                    },
                    "preferredTerms": [12, 24, 36]
                }
                """))
            .check(status().is(200)));
    }

    private ChainBuilder getLoanDetailsRequest() {
        return exec(http("Get Loan Details")
            .get("/api/v1/loans/#{loanId}")
            .header("X-Correlation-ID", "#{correlationId}")
            .check(status().in(200, 404)));
    }

    private ChainBuilder updateLoanTermsRequest() {
        return exec(http("Update Loan Terms")
            .put("/api/v1/loans/#{loanId}/terms")
            .header("X-Correlation-ID", "#{correlationId}")
            .body(StringBody("""
                {
                    "interestRate": 0.0599,
                    "termMonths": 36
                }
                """))
            .check(status().in(200, 404)));
    }

    // Payment Processing HTTP Requests (PCI-DSS Compliant)
    private ChainBuilder processPaymentRequest() {
        return exec(http("Process Payment")
            .post("/api/v1/payments")
            .header("X-Correlation-ID", "#{correlationId}")
            .header("X-PCI-Scope", "true")
            .body(StringBody("""
                {
                    "paymentMethod": "CREDIT_CARD",
                    "amount": {
                        "value": #{paymentAmount},
                        "currency": "USD"
                    },
                    "cardDetails": {
                        "cardNumber": "4111111111111111",
                        "expiryMonth": 12,
                        "expiryYear": 2025,
                        "cvv": "123",
                        "holderName": "LoadTest User"
                    },
                    "loanId": "#{loanId}"
                }
                """.replace("#{paymentAmount}", String.valueOf(ThreadLocalRandom.current().nextInt(100, 2000)))))
            .check(status().is(201))
            .check(jsonPath("$.paymentId").saveAs("paymentId")));
    }

    private ChainBuilder getPaymentStatusRequest() {
        return exec(http("Get Payment Status")
            .get("/api/v1/payments/#{paymentId}/status")
            .header("X-Correlation-ID", "#{correlationId}")
            .check(status().in(200, 404)));
    }

    private ChainBuilder refundPaymentRequest() {
        return exec(http("Refund Payment")
            .post("/api/v1/payments/#{paymentId}/refund")
            .header("X-Correlation-ID", "#{correlationId}")
            .body(StringBody("""
                {
                    "refundAmount": {
                        "value": 100.00,
                        "currency": "USD"
                    },
                    "reason": "CUSTOMER_REQUEST"
                }
                """))
            .check(status().in(200, 404)));
    }

    private ChainBuilder validatePaymentRequest() {
        return exec(http("Validate Payment")
            .post("/api/v1/payments/validate")
            .header("X-Correlation-ID", "#{correlationId}")
            .body(StringBody("""
                {
                    "cardNumber": "4111111111111111",
                    "expiryMonth": 12,
                    "expiryYear": 2025,
                    "cvv": "123"
                }
                """))
            .check(status().is(200)));
    }

    private ChainBuilder getPaymentHistoryRequest() {
        return exec(http("Get Payment History")
            .get("/api/v1/payments/history")
            .queryParam("customerId", "#{customerId}")
            .queryParam("limit", "10")
            .header("X-Correlation-ID", "#{correlationId}")
            .check(status().is(200)));
    }

    private ChainBuilder cancelPaymentRequest() {
        return exec(http("Cancel Payment")
            .post("/api/v1/payments/#{paymentId}/cancel")
            .header("X-Correlation-ID", "#{correlationId}")
            .body(StringBody("""
                {
                    "reason": "CUSTOMER_REQUEST",
                    "cancelledBy": "LOAD_TEST"
                }
                """))
            .check(status().in(200, 404)));
    }

    // System Monitoring Requests
    private ChainBuilder healthCheckRequest() {
        return exec(http("Health Check")
            .get("/actuator/health")
            .check(status().is(200))
            .check(jsonPath("$.status").is("UP")));
    }

    private ChainBuilder metricsRequest() {
        return exec(http("Metrics")
            .get("/actuator/prometheus")
            .check(status().is(200)));
    }

    private ChainBuilder systemInfoRequest() {
        return exec(http("System Info")
            .get("/actuator/info")
            .check(status().is(200)));
    }

    private ChainBuilder complianceStatusRequest() {
        return exec(http("Compliance Status")
            .get("/api/v1/compliance/status")
            .header("X-Correlation-ID", "#{correlationId}")
            .check(status().is(200)));
    }

    private ChainBuilder performanceStatsRequest() {
        return exec(http("Performance Stats")
            .get("/api/v1/system/performance")
            .header("X-Correlation-ID", "#{correlationId}")
            .check(status().is(200)));
    }

    // Fraud Detection Requests
    private ChainBuilder normalTransactionRequest() {
        return exec(http("Normal Transaction")
            .post("/api/v1/fraud/analyze")
            .header("X-Correlation-ID", "#{correlationId}")
            .body(StringBody("""
                {
                    "transactionId": "#{transactionId}",
                    "amount": {
                        "value": #{normalAmount},
                        "currency": "USD"
                    },
                    "merchantCategory": "GROCERY",
                    "location": {
                        "city": "TestCity",
                        "state": "TS",
                        "country": "US"
                    }
                }
                """.replace("#{normalAmount}", String.valueOf(ThreadLocalRandom.current().nextInt(10, 500)))))
            .check(status().is(200))
            .check(jsonPath("$.riskScore").lt(0.3)));
    }

    private ChainBuilder suspiciousTransactionRequest() {
        return exec(http("Suspicious Transaction")
            .post("/api/v1/fraud/analyze")
            .header("X-Correlation-ID", "#{correlationId}")
            .body(StringBody("""
                {
                    "transactionId": "#{transactionId}",
                    "amount": {
                        "value": #{suspiciousAmount},
                        "currency": "USD"
                    },
                    "merchantCategory": "ATM",
                    "location": {
                        "city": "UnknownCity",
                        "state": "XX",
                        "country": "XX"
                    },
                    "timeOfDay": "03:00"
                }
                """.replace("#{suspiciousAmount}", String.valueOf(ThreadLocalRandom.current().nextInt(2000, 10000)))))
            .check(status().is(200))
            .check(jsonPath("$.riskScore").between(0.3, 0.8)));
    }

    private ChainBuilder fraudulentTransactionRequest() {
        return exec(http("Fraudulent Transaction")
            .post("/api/v1/fraud/analyze")
            .header("X-Correlation-ID", "#{correlationId}")
            .body(StringBody("""
                {
                    "transactionId": "#{transactionId}",
                    "amount": {
                        "value": #{fraudAmount},
                        "currency": "USD"
                    },
                    "merchantCategory": "ONLINE",
                    "location": {
                        "city": "FraudCity",
                        "state": "FR",
                        "country": "FR"
                    },
                    "rapidSuccession": true,
                    "unusualPattern": true
                }
                """.replace("#{fraudAmount}", String.valueOf(ThreadLocalRandom.current().nextInt(5000, 50000)))))
            .check(status().is(200))
            .check(jsonPath("$.riskScore").gt(0.8)));
    }
}