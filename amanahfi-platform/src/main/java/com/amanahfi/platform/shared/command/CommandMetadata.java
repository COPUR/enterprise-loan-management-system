package com.amanahfi.platform.shared.command;

import lombok.Builder;
import lombok.Value;

import java.util.Map;

/**
 * Metadata container for commands in the AmanahFi Platform
 * 
 * This value object contains contextual information about commands
 * that is not part of the core business logic but is essential for
 * system operations, audit trails, and compliance.
 * 
 * @author AmanahFi Development Team
 * @version 1.0.0
 * @since 2024
 */
@Value
@Builder
public class CommandMetadata {

    /**
     * Source system or component that generated the command
     */
    String source;

    /**
     * IP address of the requester
     */
    String ipAddress;

    /**
     * User agent information
     */
    String userAgent;

    /**
     * Session identifier
     */
    String sessionId;

    /**
     * Request identifier for tracing
     */
    String requestId;

    /**
     * Additional custom metadata
     */
    Map<String, String> customData;

    /**
     * Sharia compliance context
     */
    ShariaContext shariaContext;

    /**
     * Regulatory context
     */
    RegulatoryContext regulatoryContext;

    /**
     * Creates empty metadata
     */
    public static CommandMetadata empty() {
        return CommandMetadata.builder()
                .customData(Map.of())
                .build();
    }

    /**
     * Creates metadata for API requests
     */
    public static CommandMetadata forApiRequest(String source, String ipAddress, String userAgent, String sessionId) {
        return CommandMetadata.builder()
                .source(source)
                .ipAddress(ipAddress)
                .userAgent(userAgent)
                .sessionId(sessionId)
                .customData(Map.of())
                .build();
    }

    /**
     * Sharia compliance context
     */
    @Value
    @Builder
    public static class ShariaContext {
        boolean requiresValidation;
        String shariaAuthority;
        String complianceReference;
        String islamicPrinciples;
    }

    /**
     * Regulatory compliance context
     */
    @Value
    @Builder
    public static class RegulatoryContext {
        String framework;
        String requirements;
        boolean reportingRequired;
        String jurisdiction;
    }
}