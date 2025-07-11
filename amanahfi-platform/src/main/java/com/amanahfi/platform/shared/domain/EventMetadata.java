package com.amanahfi.platform.shared.domain;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Value;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

/**
 * Metadata container for domain events in the AmanahFi Platform
 * 
 * This value object contains contextual information about domain events
 * that is not part of the core business data but is essential for
 * system operations, compliance, and audit purposes.
 * 
 * Key Features:
 * - Immutable value object for thread safety
 * - Rich contextual information for audit trails
 * - Compliance and regulatory tracking support
 * - Multi-tenant and multi-jurisdiction support
 * - Security context preservation
 * 
 * Islamic Finance Context:
 * - Tracks Sharia compliance requirements
 * - Supports religious calendar context
 * - Enables jurisdiction-specific processing
 * - Facilitates regulatory reporting
 * 
 * @author AmanahFi Development Team
 * @version 1.0.0
 * @since 2024
 */
@Value
@Builder(toBuilder = true)
public class EventMetadata {

    /**
     * User who initiated the action that caused this event
     */
    String userId;

    /**
     * Session identifier for correlation
     */
    String sessionId;

    /**
     * Source system or component that generated the event
     */
    String source;

    /**
     * Jurisdiction where the event occurred (ISO country code)
     */
    String jurisdiction;

    /**
     * Tenant identifier for multi-tenant deployments
     */
    String tenantId;

    /**
     * IP address of the user or system that triggered the event
     */
    String ipAddress;

    /**
     * User agent information (for web requests)
     */
    String userAgent;

    /**
     * Request trace identifier for distributed tracing
     */
    UUID traceId;

    /**
     * Span identifier within the trace
     */
    UUID spanId;

    /**
     * Timestamp when the metadata was created
     */
    Instant createdAt;

    /**
     * Additional custom metadata as key-value pairs
     */
    Map<String, Object> customMetadata;

    /**
     * Sharia compliance context
     */
    ShariaContext shariaContext;

    /**
     * Regulatory compliance context
     */
    RegulatoryContext regulatoryContext;

    /**
     * Constructor for JSON deserialization
     */
    @JsonCreator
    public EventMetadata(
            @JsonProperty("userId") String userId,
            @JsonProperty("sessionId") String sessionId,
            @JsonProperty("source") String source,
            @JsonProperty("jurisdiction") String jurisdiction,
            @JsonProperty("tenantId") String tenantId,
            @JsonProperty("ipAddress") String ipAddress,
            @JsonProperty("userAgent") String userAgent,
            @JsonProperty("traceId") UUID traceId,
            @JsonProperty("spanId") UUID spanId,
            @JsonProperty("createdAt") Instant createdAt,
            @JsonProperty("customMetadata") Map<String, Object> customMetadata,
            @JsonProperty("shariaContext") ShariaContext shariaContext,
            @JsonProperty("regulatoryContext") RegulatoryContext regulatoryContext) {
        this.userId = userId;
        this.sessionId = sessionId;
        this.source = source;
        this.jurisdiction = jurisdiction != null ? jurisdiction : "AE"; // Default to UAE
        this.tenantId = tenantId;
        this.ipAddress = ipAddress;
        this.userAgent = userAgent;
        this.traceId = traceId;
        this.spanId = spanId;
        this.createdAt = createdAt != null ? createdAt : Instant.now();
        this.customMetadata = customMetadata != null ? Map.copyOf(customMetadata) : Map.of();
        this.shariaContext = shariaContext;
        this.regulatoryContext = regulatoryContext;
    }

    /**
     * Creates a minimal EventMetadata with just the source
     */
    public static EventMetadata system(String source) {
        return EventMetadata.builder()
                .source(source)
                .jurisdiction("AE")
                .createdAt(Instant.now())
                .build();
    }

    /**
     * Creates EventMetadata for a user action
     */
    public static EventMetadata userAction(String userId, String sessionId, String jurisdiction) {
        return EventMetadata.builder()
                .userId(userId)
                .sessionId(sessionId)
                .source("USER_ACTION")
                .jurisdiction(jurisdiction != null ? jurisdiction : "AE")
                .createdAt(Instant.now())
                .build();
    }

    /**
     * Creates EventMetadata for an API request
     */
    public static EventMetadata apiRequest(String userId, String sessionId, String ipAddress, 
                                         String userAgent, String jurisdiction) {
        return EventMetadata.builder()
                .userId(userId)
                .sessionId(sessionId)
                .source("API_REQUEST")
                .ipAddress(ipAddress)
                .userAgent(userAgent)
                .jurisdiction(jurisdiction != null ? jurisdiction : "AE")
                .createdAt(Instant.now())
                .build();
    }

    /**
     * Sharia compliance context for Islamic finance operations
     */
    @Value
    @Builder
    public static class ShariaContext {
        
        /**
         * Whether this operation requires Sharia board approval
         */
        boolean requiresShariaApproval;
        
        /**
         * Sharia board reference or fatwa number
         */
        String shariaReference;
        
        /**
         * Islamic calendar date context
         */
        String islamicDate;
        
        /**
         * Relevant Islamic finance principles
         */
        String applicablePrinciples;
        
        /**
         * Compliance validation status
         */
        String complianceStatus;
    }

    /**
     * Regulatory compliance context for multi-jurisdiction operations
     */
    @Value
    @Builder
    public static class RegulatoryContext {
        
        /**
         * Regulatory framework applicable (e.g., CBUAE, VARA, SAMA)
         */
        String regulatoryFramework;
        
        /**
         * Compliance requirements that apply
         */
        String complianceRequirements;
        
        /**
         * Reporting obligations triggered
         */
        String reportingObligations;
        
        /**
         * Risk classification
         */
        String riskClassification;
        
        /**
         * Data residency requirements
         */
        String dataResidencyRequirements;
    }

    /**
     * Adds custom metadata to this instance
     */
    public EventMetadata withCustomMetadata(String key, Object value) {
        if (key == null || value == null) {
            return this;
        }
        
        var newCustomMetadata = Map.<String, Object>builder()
                .putAll(this.customMetadata)
                .put(key, value)
                .build();
        
        return this.toBuilder()
                .customMetadata(newCustomMetadata)
                .build();
    }

    /**
     * Adds Sharia context to this metadata
     */
    public EventMetadata withShariaContext(ShariaContext shariaContext) {
        return this.toBuilder()
                .shariaContext(shariaContext)
                .build();
    }

    /**
     * Adds regulatory context to this metadata
     */
    public EventMetadata withRegulatoryContext(RegulatoryContext regulatoryContext) {
        return this.toBuilder()
                .regulatoryContext(regulatoryContext)
                .build();
    }

    /**
     * Adds tracing information to this metadata
     */
    public EventMetadata withTracing(UUID traceId, UUID spanId) {
        return this.toBuilder()
                .traceId(traceId)
                .spanId(spanId)
                .build();
    }

    /**
     * Checks if this metadata indicates a Sharia-sensitive operation
     */
    public boolean isShariasensitive() {
        return shariaContext != null && shariaContext.requiresShariaApproval();
    }

    /**
     * Checks if this metadata indicates a regulatory-sensitive operation
     */
    public boolean isRegulatorySensitive() {
        return regulatoryContext != null && 
               regulatoryContext.reportingObligations() != null;
    }

    /**
     * Gets the effective jurisdiction (defaults to UAE)
     */
    public String getEffectiveJurisdiction() {
        return jurisdiction != null ? jurisdiction : "AE";
    }
}