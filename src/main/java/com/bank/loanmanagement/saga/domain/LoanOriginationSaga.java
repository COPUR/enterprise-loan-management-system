package com.bank.loanmanagement.saga.domain;

import java.time.Duration;
import java.util.List;
import java.util.Map;

/**
 * BIAN-compliant Loan Origination SAGA Definition
 * Orchestrates the complete loan origination process across multiple service domains
 * Implements compensation patterns for distributed transaction consistency
 * Follows Berlin Group and FAPI security compliance requirements
 */
public class LoanOriginationSaga implements SagaDefinition {

    private static final String SAGA_TYPE = "LoanOriginationSaga";
    private static final String VERSION = "1.0";
    private static final Duration SAGA_TIMEOUT = Duration.ofHours(2);

    private final List<SagaStepDefinition> steps;
    private final RetryPolicy retryPolicy;
    private final ComplianceRequirements complianceRequirements;

    public LoanOriginationSaga() {
        this.steps = defineSteps();
        this.retryPolicy = createRetryPolicy();
        this.complianceRequirements = createComplianceRequirements();
    }

    @Override
    public String getSagaType() {
        return SAGA_TYPE;
    }

    @Override
    public String getVersion() {
        return VERSION;
    }

    @Override
    public String getDescription() {
        return "BIAN-compliant loan origination process covering customer validation, " +
               "credit assessment, risk evaluation, loan approval, and fulfillment";
    }

    @Override
    public List<SagaStepDefinition> getSteps() {
        return steps;
    }

    @Override
    public Duration getTimeout() {
        return SAGA_TIMEOUT;
    }

    @Override
    public RetryPolicy getRetryPolicy() {
        return retryPolicy;
    }

    @Override
    public String getServiceDomainContext() {
        return "LoanOrigination";
    }

    @Override
    public ComplianceRequirements getComplianceRequirements() {
        return complianceRequirements;
    }

    private List<SagaStepDefinition> defineSteps() {
        return List.of(
            // Step 1: Customer Management - Validate customer
            new SagaStepDefinitionBuilder()
                .stepId("validate-customer")
                .stepType("CustomerValidation")
                .description("Validate customer information and eligibility")
                .targetServiceDomain("CustomerManagement")
                .behaviorQualifier("RETRIEVE")
                .serviceOperation("validateCustomerEligibility")
                .stepTimeout(Duration.ofMinutes(5))
                .maxRetries(3)
                .compensationAction("logCustomerValidationFailure")
                .compensationRequired(false)
                .retryableExceptions(List.of(
                    java.util.concurrent.TimeoutException.class,
                    org.springframework.dao.TransientDataAccessException.class
                ))
                .fatalExceptions(List.of(
                    SecurityException.class,
                    IllegalArgumentException.class
                ))
                .build(),

            // Step 2: Account Information Services - Verify accounts
            new SagaStepDefinitionBuilder()
                .stepId("verify-accounts")
                .stepType("AccountVerification")
                .description("Verify debtor and creditor account information")
                .targetServiceDomain("AccountInformationServices")
                .behaviorQualifier("RETRIEVE")
                .serviceOperation("verifyAccountInformation")
                .stepTimeout(Duration.ofMinutes(3))
                .maxRetries(2)
                .dependsOnSteps(List.of("validate-customer"))
                .compensationAction("releaseAccountReservation")
                .compensationRequired(true)
                .compensationTimeout(Duration.ofMinutes(1))
                .build(),

            // Step 3: Credit Risk Assessment - Perform risk analysis
            new SagaStepDefinitionBuilder()
                .stepId("assess-credit-risk")
                .stepType("CreditRiskAssessment")
                .description("Perform comprehensive credit risk assessment")
                .targetServiceDomain("CreditRiskAssessment")
                .behaviorQualifier("EXECUTE")
                .serviceOperation("executeRiskAssessment")
                .stepTimeout(Duration.ofMinutes(10))
                .maxRetries(2)
                .dependsOnSteps(List.of("validate-customer", "verify-accounts"))
                .compensationAction("cancelRiskAssessment")
                .compensationRequired(true)
                .compensationTimeout(Duration.ofMinutes(2))
                .build(),

            // Step 4: Consumer Loan - Create loan arrangement
            new SagaStepDefinitionBuilder()
                .stepId("create-loan-arrangement")
                .stepType("LoanArrangementCreation")
                .description("Create consumer loan arrangement")
                .targetServiceDomain("ConsumerLoan")
                .behaviorQualifier("INITIATE")
                .serviceOperation("initiateLoanArrangement")
                .stepTimeout(Duration.ofMinutes(5))
                .maxRetries(3)
                .dependsOnSteps(List.of("assess-credit-risk"))
                .compensationAction("cancelLoanArrangement")
                .compensationRequired(true)
                .compensationTimeout(Duration.ofMinutes(3))
                .build(),

            // Step 5: Consumer Loan - Grant loan approval (conditional)
            new SagaStepDefinitionBuilder()
                .stepId("grant-loan-approval")
                .stepType("LoanApproval")
                .description("Grant loan approval based on risk assessment")
                .targetServiceDomain("ConsumerLoan")
                .behaviorQualifier("GRANT")
                .serviceOperation("grantLoanApproval")
                .stepTimeout(Duration.ofMinutes(3))
                .maxRetries(2)
                .dependsOnSteps(List.of("create-loan-arrangement"))
                .executionCondition(data -> {
                    // Only execute if risk assessment is approved
                    Object riskResult = data.get("creditRiskResult");
                    return riskResult != null && "APPROVED".equals(riskResult.toString());
                })
                .compensationAction("revokeLoanApproval")
                .compensationRequired(true)
                .compensationTimeout(Duration.ofMinutes(1))
                .build(),

            // Step 6: Payment Initiation - Setup disbursement
            new SagaStepDefinitionBuilder()
                .stepId("setup-disbursement")
                .stepType("PaymentSetup")
                .description("Setup payment initiation for loan disbursement")
                .targetServiceDomain("PaymentInitiation")
                .behaviorQualifier("INITIATE")
                .serviceOperation("initiatePayment")
                .stepTimeout(Duration.ofMinutes(5))
                .maxRetries(3)
                .dependsOnSteps(List.of("grant-loan-approval"))
                .compensationAction("cancelPaymentInitiation")
                .compensationRequired(true)
                .compensationTimeout(Duration.ofMinutes(2))
                .build(),

            // Step 7: Consumer Loan - Execute fulfillment
            new SagaStepDefinitionBuilder()
                .stepId("execute-fulfillment")
                .stepType("LoanFulfillment")
                .description("Execute loan fulfillment and disbursement")
                .targetServiceDomain("ConsumerLoan")
                .behaviorQualifier("EXECUTE")
                .serviceOperation("executeLoanFulfillment")
                .stepTimeout(Duration.ofMinutes(7))
                .maxRetries(2)
                .dependsOnSteps(List.of("setup-disbursement"))
                .compensationAction("reverseLoanFulfillment")
                .compensationRequired(true)
                .compensationTimeout(Duration.ofMinutes(5))
                .build(),

            // Step 8: Payment Initiation - Execute payment
            new SagaStepDefinitionBuilder()
                .stepId("execute-payment")
                .stepType("PaymentExecution")
                .description("Execute payment for loan disbursement")
                .targetServiceDomain("PaymentInitiation")
                .behaviorQualifier("EXECUTE")
                .serviceOperation("executePayment")
                .stepTimeout(Duration.ofMinutes(10))
                .maxRetries(3)
                .dependsOnSteps(List.of("execute-fulfillment"))
                .compensationAction("reversePayment")
                .compensationRequired(true)
                .compensationTimeout(Duration.ofMinutes(5))
                .retryableExceptions(List.of(
                    java.util.concurrent.TimeoutException.class,
                    org.springframework.dao.TransientDataAccessException.class,
                    java.net.ConnectException.class
                ))
                .build(),

            // Step 9: Notification and Completion
            new SagaStepDefinitionBuilder()
                .stepId("notify-completion")
                .stepType("NotificationDelivery")
                .description("Notify customer and stakeholders of loan completion")
                .targetServiceDomain("CustomerManagement")
                .behaviorQualifier("NOTIFY")
                .serviceOperation("notifyLoanCompletion")
                .stepTimeout(Duration.ofMinutes(2))
                .maxRetries(5)
                .dependsOnSteps(List.of("execute-payment"))
                .compensationAction("sendFailureNotification")
                .compensationRequired(false)
                .build()
        );
    }

    private RetryPolicy createRetryPolicy() {
        return new DefaultRetryPolicy(
            3, // maxRetries
            Duration.ofSeconds(1), // initialDelay
            Duration.ofMinutes(1), // maxDelay
            2.0, // backoffMultiplier
            List.of(
                java.util.concurrent.TimeoutException.class,
                org.springframework.dao.TransientDataAccessException.class,
                java.net.ConnectException.class,
                java.io.IOException.class
            ),
            List.of(
                SecurityException.class,
                IllegalArgumentException.class,
                java.security.InvalidKeyException.class
            )
        );
    }

    private ComplianceRequirements createComplianceRequirements() {
        return new DefaultComplianceRequirements(
            true, // auditRequired
            true, // bianCompliant
            true, // berlinGroupCompliant
            true, // fapiSecurityRequired
            List.of(
                "LoanOriginationAuditReport",
                "CreditRiskAssessmentReport",
                "PaymentInitiationReport",
                "RegulatoryComplianceReport"
            ),
            Map.of(
                "regulatoryFramework", "PSD2_BIAN_FAPI",
                "complianceVersion", "1.0",
                "auditLevel", "COMPREHENSIVE",
                "dataRetentionPeriod", "7_YEARS",
                "encryptionStandard", "AES_256_GCM",
                "signatureAlgorithm", "RS256"
            )
        );
    }

    /**
     * Factory method to create loan origination SAGA with specific configuration
     */
    public static LoanOriginationSaga createForLoanType(String loanType) {
        // Could customize steps based on loan type
        // For now, return standard configuration
        return new LoanOriginationSaga();
    }

    /**
     * Validate SAGA definition compliance
     */
    public ValidationResult validateCompliance() {
        boolean allStepsHaveServiceDomains = steps.stream()
            .allMatch(step -> step.getTargetServiceDomain() != null);
        
        boolean allStepsHaveBehaviorQualifiers = steps.stream()
            .allMatch(step -> step.getBehaviorQualifier() != null);
        
        boolean compensationProperlyConfigured = steps.stream()
            .filter(SagaStepDefinition::isCompensationRequired)
            .allMatch(step -> step.getCompensationAction() != null);

        boolean isCompliant = allStepsHaveServiceDomains && 
                            allStepsHaveBehaviorQualifiers && 
                            compensationProperlyConfigured;

        return new ValidationResult(
            isCompliant,
            isCompliant ? "SAGA definition is fully compliant" : "SAGA definition has compliance issues",
            isCompliant ? List.of() : List.of(
                !allStepsHaveServiceDomains ? "Some steps missing service domain" : null,
                !allStepsHaveBehaviorQualifiers ? "Some steps missing behavior qualifier" : null,
                !compensationProperlyConfigured ? "Compensation not properly configured" : null
            ).stream().filter(s -> s != null).toList()
        );
    }

    /**
     * SAGA validation result
     */
    public record ValidationResult(
        boolean isValid,
        String message,
        List<String> validationErrors
    ) {}
}