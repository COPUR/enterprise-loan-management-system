package com.bank.loanmanagement.loan.infrastructure.messaging;

import com.bank.loanmanagement.loan.domain.shared.DomainEvent;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Enterprise Banking Event Topic Router - Industry Standards Compliant
 * Implements comprehensive banking domain topic routing following industry best practices
 */
@Component
public class EventTopicRouter {

    private static final Map<String, TopicConfiguration> eventTopicMappings;

    static {
        Map<String, TopicConfiguration> aMap = new HashMap<>();
        // Customer Lifecycle Management Events
        aMap.put("CustomerCreated", TopicConfiguration.of("banking.customer.onboarding.kyc-completed.v1", "customer_id"));
        aMap.put("CustomerUpdated", TopicConfiguration.of("banking.customer.lifecycle.profile-updated.v1", "customer_id"));
        aMap.put("CustomerBlocked", TopicConfiguration.of("banking.customer.lifecycle.account-blocked.v1", "customer_id"));
        aMap.put("CustomerKYCCompleted", TopicConfiguration.of("banking.customer.onboarding.kyc-completed.v1", "customer_id"));

        // Account & Product Management Events
        aMap.put("AccountOpened", TopicConfiguration.of("banking.account.deposits.created.v1", "account_number"));
        aMap.put("AccountClosed", TopicConfiguration.of("banking.account.deposits.closed.v1", "account_number"));
        aMap.put("BalanceUpdated", TopicConfiguration.of("banking.account.deposits.balance-updated.v1", "account_number"));

        // Loan Domain Events (Enhanced)
        aMap.put("LoanApplicationSubmitted", TopicConfiguration.of("banking.account.loans.originated.v1", "customer_id"));
        aMap.put("LoanApproved", TopicConfiguration.of("banking.account.loans.approved.v1", "customer_id"));
        aMap.put("LoanRejected", TopicConfiguration.of("banking.account.loans.rejected.v1", "customer_id"));
        aMap.put("LoanDisbursed", TopicConfiguration.of("banking.account.loans.disbursed.v1", "customer_id"));
        aMap.put("LoanFullyPaid", TopicConfiguration.of("banking.account.loans.fully-paid.v1", "customer_id"));
        aMap.put("LoanPaymentReceived", TopicConfiguration.of("banking.account.loans.payment-received.v1", "account_number"));

        // High-Volume Transaction Processing Events
        aMap.put("PaymentInitiated", TopicConfiguration.of("banking.transaction.payments.initiated.v1", "account_number"));
        aMap.put("PaymentProcessed", TopicConfiguration.of("banking.transaction.payments.processed.v1", "account_number"));
        aMap.put("PaymentSettled", TopicConfiguration.of("banking.transaction.payments.settled.v1", "account_number"));
        aMap.put("PaymentFailed", TopicConfiguration.of("banking.transaction.payments.failed.v1", "account_number"));
        aMap.put("DomesticTransfer", TopicConfiguration.of("banking.transaction.transfers.domestic.v1", "account_number"));
        aMap.put("InternationalTransfer", TopicConfiguration.of("banking.transaction.transfers.international.v1", "account_number"));
        aMap.put("ATMWithdrawal", TopicConfiguration.of("banking.transaction.atm.withdrawal.v1", "account_number"));
        aMap.put("POSPurchase", TopicConfiguration.of("banking.transaction.pos.purchase.v1", "account_number"));

        // Credit & Risk Management Events
        aMap.put("CreditAssessmentRequested", TopicConfiguration.of("banking.credit.assessment.requested.v1", "customer_id"));
        aMap.put("AIRiskAssessmentCompleted", TopicConfiguration.of("banking.credit.assessment.completed.v1", "customer_id"));
        aMap.put("CreditLimitUpdated", TopicConfiguration.of("banking.credit.limits.updated.v1", "customer_id"));
        aMap.put("CreditBureauInquiry", TopicConfiguration.of("banking.credit.bureau.inquiry.v1", "customer_id"));
        aMap.put("CreditBureauResponse", TopicConfiguration.of("banking.credit.bureau.response.v1", "customer_id"));
        aMap.put("RiskExposureCalculated", TopicConfiguration.of("banking.risk.exposure.calculated.v1", "portfolio_id"));
        aMap.put("PortfolioUpdated", TopicConfiguration.of("banking.risk.portfolio.updated.v1", "portfolio_id"));

        // Compliance & Regulatory Events (Industry Standards)
        aMap.put("AMLScreeningCompleted", TopicConfiguration.of("banking.compliance.aml.screening.v1", "customer_id"));
        aMap.put("KYCVerificationCompleted", TopicConfiguration.of("banking.compliance.kyc.verification.v1", "customer_id"));
        aMap.put("SanctionsCheckCompleted", TopicConfiguration.of("banking.compliance.sanctions.check.v1", "customer_id"));
        aMap.put("CTRFiled", TopicConfiguration.of("banking.compliance.ctr.filed.v1", "transaction_id"));
        aMap.put("SARFiled", TopicConfiguration.of("banking.compliance.sar.filed.v1", "case_id"));
        aMap.put("FATCAReporting", TopicConfiguration.of("banking.compliance.fatca.reporting.v1", "customer_id"));
        aMap.put("Basel3Calculation", TopicConfiguration.of("banking.compliance.basel3.calculation.v1", "calculation_id"));
        aMap.put("ComplianceCheckCompleted", TopicConfiguration.of("banking.compliance.check-completed.v1", "entity_id"));
        aMap.put("RegulatoryReportGenerated", TopicConfiguration.of("banking.compliance.report-generated.v1", "report_id"));

        // Fraud & Security Events
        aMap.put("FraudDetected", TopicConfiguration.of("banking.fraud.transaction.flagged.v1", "transaction_id"));
        aMap.put("FraudInvestigationInitiated", TopicConfiguration.of("banking.fraud.investigation.initiated.v1", "case_id"));
        aMap.put("FraudInvestigationCompleted", TopicConfiguration.of("banking.fraud.investigation.completed.v1", "case_id"));
        aMap.put("FraudModelUpdated", TopicConfiguration.of("banking.fraud.model.updated.v1", "model_id"));
        aMap.put("AuthenticationFailed", TopicConfiguration.of("banking.security.authentication.failed.v1", "user_id"));
        aMap.put("DeviceRegistered", TopicConfiguration.of("banking.security.device.registered.v1", "user_id"));

        // Treasury & Operations Events
        aMap.put("LiquidityCalculated", TopicConfiguration.of("banking.treasury.liquidity.calculated.v1", "date"));
        aMap.put("RatesUpdated", TopicConfiguration.of("banking.treasury.rates.updated.v1", "rate_type"));
        aMap.put("ReconciliationCompleted", TopicConfiguration.of("banking.operations.reconciliation.completed.v1", "batch_id"));
        aMap.put("SettlementProcessed", TopicConfiguration.of("banking.operations.settlement.processed.v1", "settlement_id"));

        aMap.put("DigitalSessionStarted", TopicConfiguration.of("banking.digital.session.started.v1", "user_id"));
        aMap.put("MobileTransaction", TopicConfiguration.of("banking.digital.transaction.mobile.v1", "user_id"));
        aMap.put("NotificationSent", TopicConfiguration.of("banking.digital.notification.sent.v1", "user_id"));
        aMap.put("BiometricVerified", TopicConfiguration.of("banking.digital.biometric.verified.v1", "user_id"));

        aMap.put("AIModelInference", TopicConfiguration.of("banking.ai.model.inference.v1", "model_execution_id"));
        aMap.put("AIRecommendationGenerated", TopicConfiguration.of("banking.ai.recommendation.generated.v1", "customer_id"));
        aMap.put("AIAnomalyDetected", TopicConfiguration.of("banking.ai.anomaly.detected.v1", "entity_id"));
        aMap.put("AIModelUpdated", TopicConfiguration.of("banking.ai.model.updated.v1", "model_id"));
        aMap.put("BehaviorAnalyzed", TopicConfiguration.of("banking.analytics.behavior.analyzed.v1", "customer_id"));

        aMap.put("SagaStarted", TopicConfiguration.of("banking.saga.loan-origination.started.v1", "saga_id"));
        aMap.put("SagaCompleted", TopicConfiguration.of("banking.saga.loan-origination.completed.v1", "saga_id"));
        aMap.put("SagaFailed", TopicConfiguration.of("banking.saga.loan-origination.failed.v1", "saga_id"));
        aMap.put("SagaCompensating", TopicConfiguration.of("banking.saga.loan-origination.compensating.v1", "saga_id"));

        aMap.put("PSD2AccountAccess", TopicConfiguration.of("banking.psd2.berlin-group.account-access.v1", "consent_id"));
        aMap.put("PSD2PaymentInitiation", TopicConfiguration.of("banking.psd2.berlin-group.payment-initiation.v1", "payment_id"));
        aMap.put("PSD2ConsentGiven", TopicConfiguration.of("banking.psd2.berlin-group.consent-given.v1", "consent_id"));

        aMap.put("BIANLoanOrigination", TopicConfiguration.of("banking.bian.service-domain.loan-origination.v1", "service_domain_reference"));
        aMap.put("BIANCustomerManagement", TopicConfiguration.of("banking.bian.service-domain.customer-management.v1", "customer_reference"));
        aMap.put("BIANProductDirectory", TopicConfiguration.of("banking.bian.service-domain.product-directory.v1", "product_reference"));

        aMap.put("ShariahComplianceCheck", TopicConfiguration.of("banking.islamic.shariah-compliance.checked.v1", "transaction_id"));
        aMap.put("MurabahaContractCreated", TopicConfiguration.of("banking.islamic.murabaha.contract-created.v1", "contract_id"));
        aMap.put("IjaraLeaseActivated", TopicConfiguration.of("banking.islamic.ijara.lease-activated.v1", "lease_id"));
        aMap.put("MusharakaInvestmentMade", TopicConfiguration.of("banking.islamic.musharaka.investment-made.v1", "investment_id"));

        aMap.put("CircuitBreakerOpened", TopicConfiguration.of("banking.infrastructure.circuit-breaker.opened.v1", "service_name"));
        aMap.put("CircuitBreakerClosed", TopicConfiguration.of("banking.infrastructure.circuit-breaker.closed.v1", "service_name"));
        aMap.put("RateLimitExceeded", TopicConfiguration.of("banking.infrastructure.rate-limit.exceeded.v1", "user_id"));
        aMap.put("ServiceHealthChanged", TopicConfiguration.of("banking.infrastructure.health.changed.v1", "service_name"));

        aMap.put("ExternalCreditBureauResponse", TopicConfiguration.of("banking.external.credit-bureau.response.v1", "inquiry_id"));
        aMap.put("ExternalRegulatoryUpdate", TopicConfiguration.of("banking.external.regulatory.update.v1", "regulation_id"));
        aMap.put("ExternalMarketDataUpdate", TopicConfiguration.of("banking.external.market-data.update.v1", "instrument_id"));

        aMap.put("LocalizationEventGenerated", TopicConfiguration.of("banking.localization.events.generated.v1", "locale"));
        aMap.put("TranslationRequested", TopicConfiguration.of("banking.localization.translation.requested.v1", "content_id"));

        aMap.put("AuditEventGenerated", TopicConfiguration.of("banking.audit.events.generated.v1", "entity_id"));
        aMap.put("DeadLetterEvent", TopicConfiguration.of("banking.infrastructure.dlq.failed-events.v1", "original_topic"));
        eventTopicMappings = Collections.unmodifiableMap(aMap);
    }

    /**
     * Topic Configuration with partition key strategy
     */
    public static class TopicConfiguration {
        private final String topicName;
        private final String partitionKeyField;

        private TopicConfiguration(String topicName, String partitionKeyField) {
            this.topicName = topicName;
            this.partitionKeyField = partitionKeyField;
        }

        public static TopicConfiguration of(String topicName, String partitionKeyField) {
            return new TopicConfiguration(topicName, partitionKeyField);
        }

        public String getTopicName() { return topicName; }
        public String getPartitionKeyField() { return partitionKeyField; }
    }

    public String getTopicForEvent(DomainEvent event) {
        TopicConfiguration config = eventTopicMappings.get(event.getEventType());
        return config != null ? config.getTopicName() : "banking.domain.events.v1";
    }

    public TopicConfiguration getTopicConfigurationForEvent(DomainEvent event) {
        return eventTopicMappings.getOrDefault(event.getEventType(),
            TopicConfiguration.of("banking.domain.events.v1", "aggregate_id"));
    }
}
