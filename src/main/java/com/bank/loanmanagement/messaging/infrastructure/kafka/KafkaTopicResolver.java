package com.bank.loanmanagement.messaging.infrastructure.kafka;

import com.bank.loanmanagement.sharedkernel.domain.event.DomainEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Map;

/**
 * Kafka Topic Resolver for BIAN-compliant event routing
 * Routes domain events to appropriate Kafka topics based on:
 * - BIAN service domain
 * - Event type
 * - Behavior qualifier
 * - Security requirements
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class KafkaTopicResolver {

    private static final String TOPIC_PREFIX = "banking";
    private static final String TOPIC_SEPARATOR = ".";
    private static final String SAGA_TOPIC_SUFFIX = "saga";
    private static final String SECURE_TOPIC_SUFFIX = "secure";

    // BIAN Service Domain to Kafka topic mapping
    private final Map<String, String> serviceDomainTopics = Map.of(
        "ConsumerLoan", "consumer-loan",
        "PaymentInitiation", "payment-initiation", 
        "AccountInformationServices", "account-information",
        "CustomerManagement", "customer-management",
        "CreditRiskAssessment", "credit-risk-assessment",
        "LoanOrigination", "loan-origination"
    );

    // Behavior qualifier routing rules
    private final Map<String, String> behaviorQualifierRouting = Map.of(
        "INITIATE", "commands",
        "UPDATE", "commands", 
        "EXECUTE", "commands",
        "CONTROL", "commands",
        "GRANT", "commands",
        "EXCHANGE", "events",
        "RETRIEVE", "queries",
        "NOTIFY", "notifications",
        "CAPTURE", "events"
    );

    /**
     * Resolve Kafka topic for domain event based on BIAN compliance rules
     */
    public String resolveTopicForEvent(DomainEvent event) {
        log.debug("Resolving topic for event {} in service domain {}", 
                 event.getEventType(), event.getServiceDomain());

        String serviceDomainTopic = getServiceDomainTopic(event.getServiceDomain());
        String behaviorQualifierTopic = getBehaviorQualifierTopic(event.getBehaviorQualifier());
        
        // Base topic structure: banking.{service-domain}.{behavior-qualifier}
        String baseTopic = String.join(TOPIC_SEPARATOR, 
            TOPIC_PREFIX, serviceDomainTopic, behaviorQualifierTopic);

        // Add security suffix if event contains sensitive data
        if (requiresSecureTopic(event)) {
            baseTopic += TOPIC_SEPARATOR + SECURE_TOPIC_SUFFIX;
        }

        log.debug("Resolved topic {} for event {}", baseTopic, event.getEventId());
        return baseTopic;
    }

    /**
     * Resolve topic for SAGA coordination events
     */
    public String resolveSagaTopicForEvent(DomainEvent event, String sagaType) {
        String serviceDomainTopic = getServiceDomainTopic(event.getServiceDomain());
        
        // SAGA topic structure: banking.{service-domain}.saga.{saga-type}
        String sagaTopic = String.join(TOPIC_SEPARATOR,
            TOPIC_PREFIX, serviceDomainTopic, SAGA_TOPIC_SUFFIX, sagaType.toLowerCase());
        
        log.debug("Resolved SAGA topic {} for event {} in SAGA type {}", 
                 sagaTopic, event.getEventId(), sagaType);
        return sagaTopic;
    }

    /**
     * Get all topics for a specific BIAN service domain
     */
    public java.util.List<String> getTopicsForServiceDomain(String serviceDomain) {
        String serviceDomainTopic = getServiceDomainTopic(serviceDomain);
        
        return behaviorQualifierRouting.values().stream()
                .distinct()
                .map(behaviorTopic -> String.join(TOPIC_SEPARATOR, 
                    TOPIC_PREFIX, serviceDomainTopic, behaviorTopic))
                .toList();
    }

    /**
     * Get dead letter topic for failed event processing
     */
    public String getDeadLetterTopic(String originalTopic) {
        return originalTopic + TOPIC_SEPARATOR + "dlq";
    }

    /**
     * Get retry topic for event reprocessing
     */
    public String getRetryTopic(String originalTopic, int retryLevel) {
        return originalTopic + TOPIC_SEPARATOR + "retry" + TOPIC_SEPARATOR + retryLevel;
    }

    /**
     * Check if topic exists based on naming convention
     */
    public boolean isValidBankingTopic(String topic) {
        return topic.startsWith(TOPIC_PREFIX + TOPIC_SEPARATOR) &&
               topic.split("\\" + TOPIC_SEPARATOR).length >= 3;
    }

    /**
     * Extract service domain from topic name
     */
    public String extractServiceDomainFromTopic(String topic) {
        if (!isValidBankingTopic(topic)) {
            throw new IllegalArgumentException("Invalid banking topic format: " + topic);
        }
        
        String[] parts = topic.split("\\" + TOPIC_SEPARATOR);
        return parts[1]; // banking.{service-domain}.{behavior-qualifier}
    }

    /**
     * Extract behavior qualifier from topic name
     */
    public String extractBehaviorQualifierFromTopic(String topic) {
        if (!isValidBankingTopic(topic)) {
            throw new IllegalArgumentException("Invalid banking topic format: " + topic);
        }
        
        String[] parts = topic.split("\\" + TOPIC_SEPARATOR);
        return parts[2]; // banking.{service-domain}.{behavior-qualifier}
    }

    private String getServiceDomainTopic(String serviceDomain) {
        String topic = serviceDomainTopics.get(serviceDomain);
        if (topic == null) {
            log.warn("Unknown service domain {}, using default mapping", serviceDomain);
            return serviceDomain.toLowerCase().replace(" ", "-");
        }
        return topic;
    }

    private String getBehaviorQualifierTopic(String behaviorQualifier) {
        String topic = behaviorQualifierRouting.get(behaviorQualifier);
        if (topic == null) {
            log.warn("Unknown behavior qualifier {}, using events as default", behaviorQualifier);
            return "events";
        }
        return topic;
    }

    private boolean requiresSecureTopic(DomainEvent event) {
        // Determine if event contains sensitive data requiring secure topic
        // This would be based on:
        // 1. Event type (e.g., payment events, customer PII)
        // 2. Service domain security requirements
        // 3. Data classification policies
        
        String eventType = event.getEventType();
        String serviceDomain = event.getServiceDomain();
        
        // Payment and customer events always require secure topics
        if (serviceDomain.equals("PaymentInitiation") || 
            serviceDomain.equals("CustomerManagement") ||
            serviceDomain.equals("CreditRiskAssessment")) {
            return true;
        }
        
        // Events containing sensitive operation types
        if (eventType.contains("Payment") || 
            eventType.contains("Customer") || 
            eventType.contains("Credit") ||
            eventType.contains("Personal")) {
            return true;
        }
        
        return false;
    }

    /**
     * Topic configuration for Kafka admin operations
     */
    public static class TopicConfiguration {
        public static final int DEFAULT_PARTITIONS = 12;
        public static final short DEFAULT_REPLICATION_FACTOR = 3;
        public static final Map<String, String> DEFAULT_TOPIC_CONFIG = Map.of(
            "cleanup.policy", "delete",
            "retention.ms", "604800000", // 7 days
            "compression.type", "lz4",
            "min.insync.replicas", "2"
        );
        
        public static final Map<String, String> SECURE_TOPIC_CONFIG = Map.of(
            "cleanup.policy", "delete",
            "retention.ms", "2592000000", // 30 days for compliance
            "compression.type", "lz4",
            "min.insync.replicas", "3", // Higher consistency for secure data
            "unclean.leader.election.enable", "false"
        );
        
        public static final Map<String, String> SAGA_TOPIC_CONFIG = Map.of(
            "cleanup.policy", "compact",
            "retention.ms", "86400000", // 24 hours for SAGA coordination
            "compression.type", "snappy",
            "min.insync.replicas", "2",
            "segment.ms", "3600000" // 1 hour segments
        );
    }
}