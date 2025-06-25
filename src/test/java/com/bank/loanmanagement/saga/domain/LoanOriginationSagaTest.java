package com.bank.loanmanagement.saga.domain;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeoutException;

import static org.assertj.core.api.Assertions.*;

/**
 * Comprehensive TDD tests for LoanOriginationSaga
 * Tests SAGA definition, step configuration, and compliance validation
 * Ensures 85%+ test coverage for SAGA orchestration patterns
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("LoanOriginationSaga Tests")
class LoanOriginationSagaTest {

    private LoanOriginationSaga loanOriginationSaga;

    @BeforeEach
    void setUp() {
        loanOriginationSaga = new LoanOriginationSaga();
    }

    @Nested
    @DisplayName("SAGA Definition Tests")
    class SagaDefinitionTests {

        @Test
        @DisplayName("Should have correct SAGA type and version")
        void shouldHaveCorrectSagaTypeAndVersion() {
            // When & Then
            assertThat(loanOriginationSaga.getSagaType()).isEqualTo("LoanOriginationSaga");
            assertThat(loanOriginationSaga.getVersion()).isEqualTo("1.0");
        }

        @Test
        @DisplayName("Should have appropriate timeout duration")
        void shouldHaveAppropriateTimeoutDuration() {
            // When
            Duration timeout = loanOriginationSaga.getTimeout();

            // Then
            assertThat(timeout).isEqualTo(Duration.ofHours(2));
        }

        @Test
        @DisplayName("Should have loan origination service domain context")
        void shouldHaveLoanOriginationServiceDomainContext() {
            // When & Then
            assertThat(loanOriginationSaga.getServiceDomainContext()).isEqualTo("LoanOrigination");
        }

        @Test
        @DisplayName("Should have meaningful description")
        void shouldHaveMeaningfulDescription() {
            // When
            String description = loanOriginationSaga.getDescription();

            // Then
            assertThat(description).isNotBlank();
            assertThat(description).contains("BIAN-compliant");
            assertThat(description).contains("loan origination");
            assertThat(description).contains("customer validation");
            assertThat(description).contains("credit assessment");
            assertThat(description).contains("fulfillment");
        }
    }

    @Nested
    @DisplayName("SAGA Steps Configuration Tests")
    class SagaStepsConfigurationTests {

        @Test
        @DisplayName("Should have 9 properly ordered steps")
        void shouldHave9ProperlyOrderedSteps() {
            // When
            List<SagaDefinition.SagaStepDefinition> steps = loanOriginationSaga.getSteps();

            // Then
            assertThat(steps).hasSize(9);
            
            // Verify step order and IDs
            assertThat(steps.get(0).getStepId()).isEqualTo("validate-customer");
            assertThat(steps.get(1).getStepId()).isEqualTo("verify-accounts");
            assertThat(steps.get(2).getStepId()).isEqualTo("assess-credit-risk");
            assertThat(steps.get(3).getStepId()).isEqualTo("create-loan-arrangement");
            assertThat(steps.get(4).getStepId()).isEqualTo("grant-loan-approval");
            assertThat(steps.get(5).getStepId()).isEqualTo("setup-disbursement");
            assertThat(steps.get(6).getStepId()).isEqualTo("execute-fulfillment");
            assertThat(steps.get(7).getStepId()).isEqualTo("execute-payment");
            assertThat(steps.get(8).getStepId()).isEqualTo("notify-completion");
        }

        @Test
        @DisplayName("Should have correct BIAN service domain mappings")
        void shouldHaveCorrectBianServiceDomainMappings() {
            // Given
            List<SagaDefinition.SagaStepDefinition> steps = loanOriginationSaga.getSteps();

            // When & Then
            assertThat(findStepById(steps, "validate-customer").getTargetServiceDomain())
                .isEqualTo("CustomerManagement");
            assertThat(findStepById(steps, "verify-accounts").getTargetServiceDomain())
                .isEqualTo("AccountInformationServices");
            assertThat(findStepById(steps, "assess-credit-risk").getTargetServiceDomain())
                .isEqualTo("CreditRiskAssessment");
            assertThat(findStepById(steps, "create-loan-arrangement").getTargetServiceDomain())
                .isEqualTo("ConsumerLoan");
            assertThat(findStepById(steps, "grant-loan-approval").getTargetServiceDomain())
                .isEqualTo("ConsumerLoan");
            assertThat(findStepById(steps, "setup-disbursement").getTargetServiceDomain())
                .isEqualTo("PaymentInitiation");
            assertThat(findStepById(steps, "execute-fulfillment").getTargetServiceDomain())
                .isEqualTo("ConsumerLoan");
            assertThat(findStepById(steps, "execute-payment").getTargetServiceDomain())
                .isEqualTo("PaymentInitiation");
            assertThat(findStepById(steps, "notify-completion").getTargetServiceDomain())
                .isEqualTo("CustomerManagement");
        }

        @Test
        @DisplayName("Should have correct BIAN behavior qualifiers")
        void shouldHaveCorrectBianBehaviorQualifiers() {
            // Given
            List<SagaDefinition.SagaStepDefinition> steps = loanOriginationSaga.getSteps();

            // When & Then
            assertThat(findStepById(steps, "validate-customer").getBehaviorQualifier())
                .isEqualTo("RETRIEVE");
            assertThat(findStepById(steps, "verify-accounts").getBehaviorQualifier())
                .isEqualTo("RETRIEVE");
            assertThat(findStepById(steps, "assess-credit-risk").getBehaviorQualifier())
                .isEqualTo("EXECUTE");
            assertThat(findStepById(steps, "create-loan-arrangement").getBehaviorQualifier())
                .isEqualTo("INITIATE");
            assertThat(findStepById(steps, "grant-loan-approval").getBehaviorQualifier())
                .isEqualTo("GRANT");
            assertThat(findStepById(steps, "setup-disbursement").getBehaviorQualifier())
                .isEqualTo("INITIATE");
            assertThat(findStepById(steps, "execute-fulfillment").getBehaviorQualifier())
                .isEqualTo("EXECUTE");
            assertThat(findStepById(steps, "execute-payment").getBehaviorQualifier())
                .isEqualTo("EXECUTE");
            assertThat(findStepById(steps, "notify-completion").getBehaviorQualifier())
                .isEqualTo("NOTIFY");
        }

        @Test
        @DisplayName("Should have proper step dependencies")
        void shouldHaveProperStepDependencies() {
            // Given
            List<SagaDefinition.SagaStepDefinition> steps = loanOriginationSaga.getSteps();

            // When & Then
            // First step should have no dependencies
            assertThat(findStepById(steps, "validate-customer").getDependsOnSteps()).isEmpty();
            
            // Account verification depends on customer validation
            assertThat(findStepById(steps, "verify-accounts").getDependsOnSteps())
                .containsExactly("validate-customer");
            
            // Risk assessment depends on customer validation and account verification
            assertThat(findStepById(steps, "assess-credit-risk").getDependsOnSteps())
                .containsExactlyInAnyOrder("validate-customer", "verify-accounts");
            
            // Loan arrangement depends on risk assessment
            assertThat(findStepById(steps, "create-loan-arrangement").getDependsOnSteps())
                .containsExactly("assess-credit-risk");
            
            // Approval depends on loan arrangement
            assertThat(findStepById(steps, "grant-loan-approval").getDependsOnSteps())
                .containsExactly("create-loan-arrangement");
            
            // Payment setup depends on approval
            assertThat(findStepById(steps, "setup-disbursement").getDependsOnSteps())
                .containsExactly("grant-loan-approval");
            
            // Fulfillment depends on payment setup
            assertThat(findStepById(steps, "execute-fulfillment").getDependsOnSteps())
                .containsExactly("setup-disbursement");
            
            // Payment execution depends on fulfillment
            assertThat(findStepById(steps, "execute-payment").getDependsOnSteps())
                .containsExactly("execute-fulfillment");
            
            // Notification depends on payment execution
            assertThat(findStepById(steps, "notify-completion").getDependsOnSteps())
                .containsExactly("execute-payment");
        }

        @Test
        @DisplayName("Should have appropriate timeouts for each step")
        void shouldHaveAppropriateTimeoutsForEachStep() {
            // Given
            List<SagaDefinition.SagaStepDefinition> steps = loanOriginationSaga.getSteps();

            // When & Then
            assertThat(findStepById(steps, "validate-customer").getStepTimeout())
                .isEqualTo(Duration.ofMinutes(5));
            assertThat(findStepById(steps, "verify-accounts").getStepTimeout())
                .isEqualTo(Duration.ofMinutes(3));
            assertThat(findStepById(steps, "assess-credit-risk").getStepTimeout())
                .isEqualTo(Duration.ofMinutes(10));
            assertThat(findStepById(steps, "execute-payment").getStepTimeout())
                .isEqualTo(Duration.ofMinutes(10));
            assertThat(findStepById(steps, "notify-completion").getStepTimeout())
                .isEqualTo(Duration.ofMinutes(2));
        }
    }

    @Nested
    @DisplayName("Compensation Configuration Tests")
    class CompensationConfigurationTests {

        @Test
        @DisplayName("Should have compensation actions for critical steps")
        void shouldHaveCompensationActionsForCriticalSteps() {
            // Given
            List<SagaDefinition.SagaStepDefinition> steps = loanOriginationSaga.getSteps();

            // When & Then
            SagaDefinition.SagaStepDefinition verifyAccountsStep = findStepById(steps, "verify-accounts");
            assertThat(verifyAccountsStep.isCompensationRequired()).isTrue();
            assertThat(verifyAccountsStep.getCompensationAction()).isEqualTo("releaseAccountReservation");

            SagaDefinition.SagaStepDefinition riskAssessmentStep = findStepById(steps, "assess-credit-risk");
            assertThat(riskAssessmentStep.isCompensationRequired()).isTrue();
            assertThat(riskAssessmentStep.getCompensationAction()).isEqualTo("cancelRiskAssessment");

            SagaDefinition.SagaStepDefinition loanArrangementStep = findStepById(steps, "create-loan-arrangement");
            assertThat(loanArrangementStep.isCompensationRequired()).isTrue();
            assertThat(loanArrangementStep.getCompensationAction()).isEqualTo("cancelLoanArrangement");

            SagaDefinition.SagaStepDefinition paymentStep = findStepById(steps, "execute-payment");
            assertThat(paymentStep.isCompensationRequired()).isTrue();
            assertThat(paymentStep.getCompensationAction()).isEqualTo("reversePayment");
        }

        @Test
        @DisplayName("Should not require compensation for read-only operations")
        void shouldNotRequireCompensationForReadOnlyOperations() {
            // Given
            List<SagaDefinition.SagaStepDefinition> steps = loanOriginationSaga.getSteps();

            // When & Then
            SagaDefinition.SagaStepDefinition validateCustomerStep = findStepById(steps, "validate-customer");
            assertThat(validateCustomerStep.isCompensationRequired()).isFalse();

            SagaDefinition.SagaStepDefinition notifyStep = findStepById(steps, "notify-completion");
            assertThat(notifyStep.isCompensationRequired()).isFalse();
        }

        @Test
        @DisplayName("Should have appropriate compensation timeouts")
        void shouldHaveAppropriateCompensationTimeouts() {
            // Given
            List<SagaDefinition.SagaStepDefinition> steps = loanOriginationSaga.getSteps();

            // When & Then
            SagaDefinition.SagaStepDefinition verifyAccountsStep = findStepById(steps, "verify-accounts");
            assertThat(verifyAccountsStep.getCompensationTimeout()).isEqualTo(Duration.ofMinutes(1));

            SagaDefinition.SagaStepDefinition fulfillmentStep = findStepById(steps, "execute-fulfillment");
            assertThat(fulfillmentStep.getCompensationTimeout()).isEqualTo(Duration.ofMinutes(5));

            SagaDefinition.SagaStepDefinition paymentStep = findStepById(steps, "execute-payment");
            assertThat(paymentStep.getCompensationTimeout()).isEqualTo(Duration.ofMinutes(5));
        }
    }

    @Nested
    @DisplayName("Retry Policy Tests")
    class RetryPolicyTests {

        @Test
        @DisplayName("Should have appropriate retry policy configuration")
        void shouldHaveAppropriateRetryPolicyConfiguration() {
            // When
            SagaDefinition.RetryPolicy retryPolicy = loanOriginationSaga.getRetryPolicy();

            // Then
            assertThat(retryPolicy.getMaxRetries()).isEqualTo(3);
            assertThat(retryPolicy.getInitialDelay()).isEqualTo(Duration.ofSeconds(1));
            assertThat(retryPolicy.getMaxDelay()).isEqualTo(Duration.ofMinutes(1));
            assertThat(retryPolicy.getBackoffMultiplier()).isEqualTo(2.0);
        }

        @Test
        @DisplayName("Should have correct retryable exceptions")
        void shouldHaveCorrectRetryableExceptions() {
            // When
            SagaDefinition.RetryPolicy retryPolicy = loanOriginationSaga.getRetryPolicy();

            // Then
            assertThat(retryPolicy.getRetryableExceptions()).contains(
                TimeoutException.class,
                org.springframework.dao.TransientDataAccessException.class,
                java.net.ConnectException.class,
                java.io.IOException.class
            );
        }

        @Test
        @DisplayName("Should have correct non-retryable exceptions")
        void shouldHaveCorrectNonRetryableExceptions() {
            // When
            SagaDefinition.RetryPolicy retryPolicy = loanOriginationSaga.getRetryPolicy();

            // Then
            assertThat(retryPolicy.getNonRetryableExceptions()).contains(
                SecurityException.class,
                IllegalArgumentException.class,
                java.security.InvalidKeyException.class
            );
        }

        @Test
        @DisplayName("Should have step-specific retry configurations")
        void shouldHaveStepSpecificRetryConfigurations() {
            // Given
            List<SagaDefinition.SagaStepDefinition> steps = loanOriginationSaga.getSteps();

            // When & Then
            assertThat(findStepById(steps, "validate-customer").getMaxRetries()).isEqualTo(3);
            assertThat(findStepById(steps, "assess-credit-risk").getMaxRetries()).isEqualTo(2);
            assertThat(findStepById(steps, "execute-payment").getMaxRetries()).isEqualTo(3);
            assertThat(findStepById(steps, "notify-completion").getMaxRetries()).isEqualTo(5);
        }
    }

    @Nested
    @DisplayName("Compliance Requirements Tests")
    class ComplianceRequirementsTests {

        @Test
        @DisplayName("Should require full compliance for regulatory adherence")
        void shouldRequireFullComplianceForRegulatoryAdherence() {
            // When
            SagaDefinition.ComplianceRequirements compliance = loanOriginationSaga.getComplianceRequirements();

            // Then
            assertThat(compliance.isAuditRequired()).isTrue();
            assertThat(compliance.isBianCompliant()).isTrue();
            assertThat(compliance.isBerlinGroupCompliant()).isTrue();
            assertThat(compliance.isFapiSecurityRequired()).isTrue();
        }

        @Test
        @DisplayName("Should specify required regulatory reports")
        void shouldSpecifyRequiredRegulatoryReports() {
            // When
            SagaDefinition.ComplianceRequirements compliance = loanOriginationSaga.getComplianceRequirements();

            // Then
            List<String> reports = compliance.getRegulatoryReports();
            assertThat(reports).contains(
                "LoanOriginationAuditReport",
                "CreditRiskAssessmentReport",
                "PaymentInitiationReport",
                "RegulatoryComplianceReport"
            );
        }

        @Test
        @DisplayName("Should have comprehensive compliance metadata")
        void shouldHaveComprehensiveComplianceMetadata() {
            // When
            SagaDefinition.ComplianceRequirements compliance = loanOriginationSaga.getComplianceRequirements();
            Map<String, String> metadata = compliance.getComplianceMetadata();

            // Then
            assertThat(metadata).containsEntry("regulatoryFramework", "PSD2_BIAN_FAPI");
            assertThat(metadata).containsEntry("complianceVersion", "1.0");
            assertThat(metadata).containsEntry("auditLevel", "COMPREHENSIVE");
            assertThat(metadata).containsEntry("dataRetentionPeriod", "7_YEARS");
            assertThat(metadata).containsEntry("encryptionStandard", "AES_256_GCM");
            assertThat(metadata).containsEntry("signatureAlgorithm", "RS256");
        }
    }

    @Nested
    @DisplayName("Conditional Execution Tests")
    class ConditionalExecutionTests {

        @Test
        @DisplayName("Should have conditional execution for loan approval step")
        void shouldHaveConditionalExecutionForLoanApprovalStep() {
            // Given
            List<SagaDefinition.SagaStepDefinition> steps = loanOriginationSaga.getSteps();
            SagaDefinition.SagaStepDefinition approvalStep = findStepById(steps, "grant-loan-approval");

            // When - Test with approved risk result
            Map<String, Object> approvedData = Map.of("creditRiskResult", "APPROVED");
            boolean shouldExecuteApproved = approvalStep.getExecutionCondition().test(approvedData);

            // Then
            assertThat(shouldExecuteApproved).isTrue();

            // When - Test with rejected risk result
            Map<String, Object> rejectedData = Map.of("creditRiskResult", "REJECTED");
            boolean shouldExecuteRejected = approvalStep.getExecutionCondition().test(rejectedData);

            // Then
            assertThat(shouldExecuteRejected).isFalse();

            // When - Test with missing risk result
            Map<String, Object> missingData = Map.of();
            boolean shouldExecuteMissing = approvalStep.getExecutionCondition().test(missingData);

            // Then
            assertThat(shouldExecuteMissing).isFalse();
        }
    }

    @Nested
    @DisplayName("Factory Method Tests")
    class FactoryMethodTests {

        @Test
        @DisplayName("Should create SAGA for different loan types")
        void shouldCreateSagaForDifferentLoanTypes() {
            // When
            LoanOriginationSaga personalLoanSaga = LoanOriginationSaga.createForLoanType("PERSONAL");
            LoanOriginationSaga mortgageSaga = LoanOriginationSaga.createForLoanType("MORTGAGE");

            // Then
            assertThat(personalLoanSaga).isNotNull();
            assertThat(mortgageSaga).isNotNull();
            assertThat(personalLoanSaga.getSagaType()).isEqualTo("LoanOriginationSaga");
            assertThat(mortgageSaga.getSagaType()).isEqualTo("LoanOriginationSaga");
        }
    }

    @Nested
    @DisplayName("Validation Tests")
    class ValidationTests {

        @Test
        @DisplayName("Should validate as compliant SAGA definition")
        void shouldValidateAsCompliantSagaDefinition() {
            // When
            LoanOriginationSaga.ValidationResult result = loanOriginationSaga.validateCompliance();

            // Then
            assertThat(result.isValid()).isTrue();
            assertThat(result.message()).contains("fully compliant");
            assertThat(result.validationErrors()).isEmpty();
        }

        @Test
        @DisplayName("Should detect all steps have service domains")
        void shouldDetectAllStepsHaveServiceDomains() {
            // Given
            List<SagaDefinition.SagaStepDefinition> steps = loanOriginationSaga.getSteps();

            // When & Then
            for (SagaDefinition.SagaStepDefinition step : steps) {
                assertThat(step.getTargetServiceDomain())
                    .as("Step %s should have service domain", step.getStepId())
                    .isNotNull()
                    .isNotBlank();
            }
        }

        @Test
        @DisplayName("Should detect all steps have behavior qualifiers")
        void shouldDetectAllStepsHaveBehaviorQualifiers() {
            // Given
            List<SagaDefinition.SagaStepDefinition> steps = loanOriginationSaga.getSteps();

            // When & Then
            for (SagaDefinition.SagaStepDefinition step : steps) {
                assertThat(step.getBehaviorQualifier())
                    .as("Step %s should have behavior qualifier", step.getStepId())
                    .isNotNull()
                    .isNotBlank();
            }
        }

        @Test
        @DisplayName("Should validate compensation is properly configured")
        void shouldValidateCompensationIsProperlyConfigured() {
            // Given
            List<SagaDefinition.SagaStepDefinition> steps = loanOriginationSaga.getSteps();

            // When & Then
            for (SagaDefinition.SagaStepDefinition step : steps) {
                if (step.isCompensationRequired()) {
                    assertThat(step.getCompensationAction())
                        .as("Step %s requires compensation but has no compensation action", step.getStepId())
                        .isNotNull()
                        .isNotBlank();
                }
            }
        }
    }

    // Helper method to find step by ID
    private SagaDefinition.SagaStepDefinition findStepById(List<SagaDefinition.SagaStepDefinition> steps, String stepId) {
        return steps.stream()
                .filter(step -> step.getStepId().equals(stepId))
                .findFirst()
                .orElseThrow(() -> new AssertionError("Step not found: " + stepId));
    }
}