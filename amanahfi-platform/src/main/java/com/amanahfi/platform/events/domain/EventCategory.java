package com.amanahfi.platform.events.domain;

/**
 * Categories of events for processing and routing
 */
public enum EventCategory {
    ISLAMIC_FINANCE,    // Islamic finance related events
    REGULATORY,         // Regulatory compliance events
    PAYMENT,           // Payment and transfer events
    CBDC,              // Central Bank Digital Currency events
    CUSTOMER,          // Customer management events
    AUDIT,             // Audit trail events
    GENERAL            // General application events
}