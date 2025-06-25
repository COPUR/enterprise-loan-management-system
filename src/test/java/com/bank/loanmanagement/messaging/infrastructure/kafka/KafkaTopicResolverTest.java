package com.bank.loanmanagement.messaging.infrastructure.kafka;

import com.bank.loanmanagement.sharedkernel.domain.event.DomainEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.*;

/**
 * Comprehensive TDD tests for KafkaTopicResolver
 * Tests BIAN-compliant topic routing and naming conventions
 * Ensures 85%+ test coverage for topic resolution logic
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("KafkaTopicResolver Tests")
class KafkaTopicResolverTest {

    private KafkaTopicResolver topicResolver;

    @BeforeEach
    void setUp() {
        topicResolver = new KafkaTopicResolver();
    }

    @Nested
    @DisplayName("Event Topic Resolution Tests")
    class EventTopicResolutionTests {

        @Test
        @DisplayName("Should resolve topic for consumer loan initiate event")
        void shouldResolveTopicForConsumerLoanInitiateEvent() {
            // Given
            DomainEvent event = new TestDomainEvent("AGG-123", "ConsumerLoan", "INITIATE");

            // When
            String topic = topicResolver.resolveTopicForEvent(event);

            // Then
            assertThat(topic).isEqualTo("banking.consumer-loan.commands");
        }

        @Test
        @DisplayName("Should resolve topic for payment initiation execute event")
        void shouldResolveTopicForPaymentInitiationExecuteEvent() {
            // Given
            DomainEvent event = new TestDomainEvent("AGG-123", "PaymentInitiation", "EXECUTE");

            // When
            String topic = topicResolver.resolveTopicForEvent(event);

            // Then
            assertThat(topic).isEqualTo("banking.payment-initiation.commands");
        }

        @Test
        @DisplayName("Should resolve topic for account information retrieve event")
        void shouldResolveTopicForAccountInformationRetrieveEvent() {
            // Given
            DomainEvent event = new TestDomainEvent("AGG-123", "AccountInformationServices", "RETRIEVE");

            // When
            String topic = topicResolver.resolveTopicForEvent(event);

            // Then
            assertThat(topic).isEqualTo("banking.account-information.queries");
        }

        @Test
        @DisplayName("Should resolve topic for customer management notify event")
        void shouldResolveTopicForCustomerManagementNotifyEvent() {
            // Given
            DomainEvent event = new TestDomainEvent("AGG-123", "CustomerManagement", "NOTIFY");

            // When
            String topic = topicResolver.resolveTopicForEvent(event);

            // Then
            assertThat(topic).isEqualTo("banking.customer-management.notifications");
        }

        @Test
        @DisplayName("Should resolve topic for credit risk exchange event")
        void shouldResolveTopicForCreditRiskExchangeEvent() {
            // Given
            DomainEvent event = new TestDomainEvent("AGG-123", "CreditRiskAssessment", "EXCHANGE");

            // When
            String topic = topicResolver.resolveTopicForEvent(event);

            // Then
            assertThat(topic).isEqualTo("banking.credit-risk-assessment.events");
        }

        @Test
        @DisplayName("Should add secure suffix for payment initiation events")
        void shouldAddSecureSuffixForPaymentInitiationEvents() {
            // Given
            DomainEvent paymentEvent = new TestDomainEvent("AGG-123", "PaymentInitiation", "INITIATE");

            // When
            String topic = topicResolver.resolveTopicForEvent(paymentEvent);

            // Then
            assertThat(topic).isEqualTo("banking.payment-initiation.commands.secure");
        }

        @Test
        @DisplayName("Should add secure suffix for customer management events")
        void shouldAddSecureSuffixForCustomerManagementEvents() {
            // Given
            DomainEvent customerEvent = new TestDomainEvent("AGG-123", "CustomerManagement", "UPDATE");

            // When
            String topic = topicResolver.resolveTopicForEvent(customerEvent);

            // Then
            assertThat(topic).isEqualTo("banking.customer-management.commands.secure");
        }

        @Test
        @DisplayName("Should add secure suffix for credit risk events")
        void shouldAddSecureSuffixForCreditRiskEvents() {
            // Given
            DomainEvent creditEvent = new TestDomainEvent("AGG-123", "CreditRiskAssessment", "EXECUTE");

            // When
            String topic = topicResolver.resolveTopicForEvent(creditEvent);

            // Then
            assertThat(topic).isEqualTo("banking.credit-risk-assessment.commands.secure");
        }

        @Test
        @DisplayName("Should handle unknown service domain with default mapping")
        void shouldHandleUnknownServiceDomainWithDefaultMapping() {
            // Given
            DomainEvent unknownEvent = new TestDomainEvent("AGG-123", "Unknown Service Domain", "INITIATE");

            // When
            String topic = topicResolver.resolveTopicForEvent(unknownEvent);

            // Then
            assertThat(topic).isEqualTo("banking.unknown-service-domain.commands");
        }

        @Test
        @DisplayName("Should handle unknown behavior qualifier with events default")
        void shouldHandleUnknownBehaviorQualifierWithEventsDefault() {
            // Given
            DomainEvent unknownBehaviorEvent = new TestDomainEvent("AGG-123", "ConsumerLoan", "UNKNOWN_BEHAVIOR");

            // When
            String topic = topicResolver.resolveTopicForEvent(unknownBehaviorEvent);

            // Then
            assertThat(topic).isEqualTo("banking.consumer-loan.events");
        }
    }

    @Nested
    @DisplayName("SAGA Topic Resolution Tests")
    class SagaTopicResolutionTests {

        @Test
        @DisplayName("Should resolve SAGA topic for loan origination")
        void shouldResolveSagaTopicForLoanOrigination() {
            // Given
            DomainEvent event = new TestDomainEvent("AGG-123", "ConsumerLoan", "INITIATE");
            String sagaType = "LoanOriginationSaga";

            // When
            String topic = topicResolver.resolveSagaTopicForEvent(event, sagaType);

            // Then
            assertThat(topic).isEqualTo("banking.consumer-loan.saga.loanoriginationsaga");
        }

        @Test
        @DisplayName("Should resolve SAGA topic for payment coordination")
        void shouldResolveSagaTopicForPaymentCoordination() {
            // Given
            DomainEvent event = new TestDomainEvent("AGG-123", "PaymentInitiation", "EXECUTE");
            String sagaType = "PaymentCoordinationSaga";

            // When
            String topic = topicResolver.resolveSagaTopicForEvent(event, sagaType);

            // Then
            assertThat(topic).isEqualTo("banking.payment-initiation.saga.paymentcoordinationsaga");
        }
    }

    @Nested
    @DisplayName("Service Domain Topic Listing Tests")
    class ServiceDomainTopicListingTests {

        @Test
        @DisplayName("Should return all topics for consumer loan service domain")
        void shouldReturnAllTopicsForConsumerLoanServiceDomain() {
            // When
            List<String> topics = topicResolver.getTopicsForServiceDomain("ConsumerLoan");

            // Then
            assertThat(topics).containsExactlyInAnyOrder(
                "banking.consumer-loan.commands",
                "banking.consumer-loan.events",
                "banking.consumer-loan.queries",
                "banking.consumer-loan.notifications"
            );
        }

        @Test
        @DisplayName("Should return all topics for payment initiation service domain")
        void shouldReturnAllTopicsForPaymentInitiationServiceDomain() {
            // When
            List<String> topics = topicResolver.getTopicsForServiceDomain("PaymentInitiation");

            // Then
            assertThat(topics).containsExactlyInAnyOrder(
                "banking.payment-initiation.commands",
                "banking.payment-initiation.events",
                "banking.payment-initiation.queries",
                "banking.payment-initiation.notifications"
            );
        }
    }

    @Nested
    @DisplayName("Dead Letter and Retry Topic Tests")
    class DeadLetterAndRetryTopicTests {

        @Test
        @DisplayName("Should generate dead letter topic for original topic")
        void shouldGenerateDeadLetterTopicForOriginalTopic() {
            // Given
            String originalTopic = "banking.consumer-loan.commands";

            // When
            String dlqTopic = topicResolver.getDeadLetterTopic(originalTopic);

            // Then
            assertThat(dlqTopic).isEqualTo("banking.consumer-loan.commands.dlq");
        }

        @Test
        @DisplayName("Should generate retry topic with level")
        void shouldGenerateRetryTopicWithLevel() {
            // Given
            String originalTopic = "banking.payment-initiation.commands";
            int retryLevel = 2;

            // When
            String retryTopic = topicResolver.getRetryTopic(originalTopic, retryLevel);

            // Then
            assertThat(retryTopic).isEqualTo("banking.payment-initiation.commands.retry.2");
        }
    }

    @Nested
    @DisplayName("Topic Validation Tests")
    class TopicValidationTests {

        @Test
        @DisplayName("Should validate correct banking topic format")
        void shouldValidateCorrectBankingTopicFormat() {
            // Given
            String validTopic = "banking.consumer-loan.commands";

            // When
            boolean isValid = topicResolver.isValidBankingTopic(validTopic);

            // Then
            assertThat(isValid).isTrue();
        }

        @Test
        @DisplayName("Should validate banking topic with secure suffix")
        void shouldValidateBankingTopicWithSecureSuffix() {
            // Given
            String validSecureTopic = "banking.payment-initiation.commands.secure";

            // When
            boolean isValid = topicResolver.isValidBankingTopic(validSecureTopic);

            // Then
            assertThat(isValid).isTrue();
        }

        @Test
        @DisplayName("Should reject invalid topic format")
        void shouldRejectInvalidTopicFormat() {
            // Given
            String invalidTopic = "invalid-topic-format";

            // When
            boolean isValid = topicResolver.isValidBankingTopic(invalidTopic);

            // Then
            assertThat(isValid).isFalse();
        }

        @Test
        @DisplayName("Should reject topic with wrong prefix")
        void shouldRejectTopicWithWrongPrefix() {
            // Given
            String wrongPrefixTopic = "finance.consumer-loan.commands";

            // When
            boolean isValid = topicResolver.isValidBankingTopic(wrongPrefixTopic);

            // Then
            assertThat(isValid).isFalse();
        }

        @Test
        @DisplayName("Should reject topic with insufficient parts")
        void shouldRejectTopicWithInsufficientParts() {
            // Given
            String insufficientTopic = "banking.consumer-loan";

            // When
            boolean isValid = topicResolver.isValidBankingTopic(insufficientTopic);

            // Then
            assertThat(isValid).isFalse();
        }
    }

    @Nested
    @DisplayName("Topic Parsing Tests")
    class TopicParsingTests {

        @Test
        @DisplayName("Should extract service domain from topic")
        void shouldExtractServiceDomainFromTopic() {
            // Given
            String topic = "banking.consumer-loan.commands";

            // When
            String serviceDomain = topicResolver.extractServiceDomainFromTopic(topic);

            // Then
            assertThat(serviceDomain).isEqualTo("consumer-loan");
        }

        @Test
        @DisplayName("Should extract behavior qualifier from topic")
        void shouldExtractBehaviorQualifierFromTopic() {
            // Given
            String topic = "banking.payment-initiation.events";

            // When
            String behaviorQualifier = topicResolver.extractBehaviorQualifierFromTopic(topic);

            // Then
            assertThat(behaviorQualifier).isEqualTo("events");
        }

        @Test
        @DisplayName("Should throw exception for invalid topic when extracting service domain")
        void shouldThrowExceptionForInvalidTopicWhenExtractingServiceDomain() {
            // Given
            String invalidTopic = "invalid-topic";

            // When & Then
            assertThatThrownBy(() -> topicResolver.extractServiceDomainFromTopic(invalidTopic))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Invalid banking topic format");
        }

        @Test
        @DisplayName("Should throw exception for invalid topic when extracting behavior qualifier")
        void shouldThrowExceptionForInvalidTopicWhenExtractingBehaviorQualifier() {
            // Given
            String invalidTopic = "invalid.format";

            // When & Then
            assertThatThrownBy(() -> topicResolver.extractBehaviorQualifierFromTopic(invalidTopic))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Invalid banking topic format");
        }
    }

    @Nested
    @DisplayName("Security Topic Detection Tests")
    class SecurityTopicDetectionTests {

        @Test
        @DisplayName("Should require secure topic for payment events")
        void shouldRequireSecureTopicForPaymentEvents() {
            // Given
            DomainEvent paymentEvent = new PaymentDomainEvent("AGG-123", "PaymentInitiation", "INITIATE");

            // When
            String topic = topicResolver.resolveTopicForEvent(paymentEvent);

            // Then
            assertThat(topic).endsWith(".secure");
        }

        @Test
        @DisplayName("Should require secure topic for customer events")
        void shouldRequireSecureTopicForCustomerEvents() {
            // Given
            DomainEvent customerEvent = new CustomerDomainEvent("AGG-123", "CustomerManagement", "UPDATE");

            // When
            String topic = topicResolver.resolveTopicForEvent(customerEvent);

            // Then
            assertThat(topic).endsWith(".secure");
        }

        @Test
        @DisplayName("Should require secure topic for credit events")
        void shouldRequireSecureTopicForCreditEvents() {
            // Given
            DomainEvent creditEvent = new CreditDomainEvent("AGG-123", "CreditRiskAssessment", "EXECUTE");

            // When
            String topic = topicResolver.resolveTopicForEvent(creditEvent);

            // Then
            assertThat(topic).endsWith(".secure");
        }

        @Test
        @DisplayName("Should not require secure topic for non-sensitive events")
        void shouldNotRequireSecureTopicForNonSensitiveEvents() {
            // Given
            DomainEvent nonSensitiveEvent = new TestDomainEvent("AGG-123", "LoanOrigination", "INITIATE");

            // When
            String topic = topicResolver.resolveTopicForEvent(nonSensitiveEvent);

            // Then
            assertThat(topic).doesNotEndWith(".secure");
        }
    }

    @Nested
    @DisplayName("Topic Configuration Tests")
    class TopicConfigurationTests {

        @Test
        @DisplayName("Should have correct default topic configuration")
        void shouldHaveCorrectDefaultTopicConfiguration() {
            // When
            Map<String, String> config = KafkaTopicResolver.TopicConfiguration.DEFAULT_TOPIC_CONFIG;

            // Then
            assertThat(config).containsEntry("cleanup.policy", "delete");
            assertThat(config).containsEntry("retention.ms", "604800000"); // 7 days
            assertThat(config).containsEntry("compression.type", "lz4");
            assertThat(config).containsEntry("min.insync.replicas", "2");
        }

        @Test
        @DisplayName("Should have enhanced secure topic configuration")
        void shouldHaveEnhancedSecureTopicConfiguration() {
            // When
            Map<String, String> config = KafkaTopicResolver.TopicConfiguration.SECURE_TOPIC_CONFIG;

            // Then
            assertThat(config).containsEntry("cleanup.policy", "delete");
            assertThat(config).containsEntry("retention.ms", "2592000000"); // 30 days
            assertThat(config).containsEntry("compression.type", "lz4");
            assertThat(config).containsEntry("min.insync.replicas", "3"); // Higher consistency
            assertThat(config).containsEntry("unclean.leader.election.enable", "false");
        }

        @Test
        @DisplayName("Should have SAGA-specific topic configuration")
        void shouldHaveSagaSpecificTopicConfiguration() {
            // When
            Map<String, String> config = KafkaTopicResolver.TopicConfiguration.SAGA_TOPIC_CONFIG;

            // Then
            assertThat(config).containsEntry("cleanup.policy", "compact");
            assertThat(config).containsEntry("retention.ms", "86400000"); // 24 hours
            assertThat(config).containsEntry("compression.type", "snappy");
            assertThat(config).containsEntry("min.insync.replicas", "2");
            assertThat(config).containsEntry("segment.ms", "3600000"); // 1 hour
        }

        @Test
        @DisplayName("Should have appropriate default partition and replication settings")
        void shouldHaveAppropriateDefaultPartitionAndReplicationSettings() {
            // When & Then
            assertThat(KafkaTopicResolver.TopicConfiguration.DEFAULT_PARTITIONS).isEqualTo(12);
            assertThat(KafkaTopicResolver.TopicConfiguration.DEFAULT_REPLICATION_FACTOR).isEqualTo((short) 3);
        }
    }

    // Test domain event implementations
    private static class TestDomainEvent extends DomainEvent {
        private final String serviceDomain;
        private final String behaviorQualifier;

        public TestDomainEvent(String aggregateId, String serviceDomain, String behaviorQualifier) {
            super(aggregateId, "TestAggregate", 1L);
            this.serviceDomain = serviceDomain;
            this.behaviorQualifier = behaviorQualifier;
        }

        @Override
        public String getEventType() {
            return "TestDomainEvent";
        }

        @Override
        public Object getEventData() {
            return Map.of("test", "data");
        }

        @Override
        public String getServiceDomain() {
            return serviceDomain;
        }

        @Override
        public String getBehaviorQualifier() {
            return behaviorQualifier;
        }
    }

    private static class PaymentDomainEvent extends TestDomainEvent {
        public PaymentDomainEvent(String aggregateId, String serviceDomain, String behaviorQualifier) {
            super(aggregateId, serviceDomain, behaviorQualifier);
        }

        @Override
        public String getEventType() {
            return "PaymentInitiated";
        }
    }

    private static class CustomerDomainEvent extends TestDomainEvent {
        public CustomerDomainEvent(String aggregateId, String serviceDomain, String behaviorQualifier) {
            super(aggregateId, serviceDomain, behaviorQualifier);
        }

        @Override
        public String getEventType() {
            return "CustomerUpdated";
        }
    }

    private static class CreditDomainEvent extends TestDomainEvent {
        public CreditDomainEvent(String aggregateId, String serviceDomain, String behaviorQualifier) {
            super(aggregateId, serviceDomain, behaviorQualifier);
        }

        @Override
        public String getEventType() {
            return "CreditAssessmentCompleted";
        }
    }
}