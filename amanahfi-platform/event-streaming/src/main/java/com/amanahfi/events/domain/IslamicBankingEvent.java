package com.amanahfi.events.domain;

/**
 * Marker interface for Islamic banking compliant events
 * 
 * Events implementing this interface indicate they:
 * - Follow Islamic finance principles (no Riba/interest)
 * - Are Sharia-compliant in their business logic
 * - Meet UAE regulatory requirements (CBUAE, VARA, HSA)
 * - Support audit trails for compliance reporting
 */
public interface IslamicBankingEvent extends DomainEvent {
    
    /**
     * Indicates if this event represents a Sharia-compliant operation
     */
    default boolean isIslamicBankingCompliant() {
        return true;
    }
    
    /**
     * Indicates if this event requires Sharia board review
     */
    default boolean requiresShariahBoardReview() {
        return false;
    }
    
    /**
     * Indicates if this event involves interest-based calculations (forbidden in Islamic banking)
     */
    default boolean hasRibaElements() {
        return false;
    }
    
    /**
     * UAE regulatory compliance indicators
     */
    default String getRegulatoryCompliance() {
        return "CBUAE,VARA,HSA";
    }
}