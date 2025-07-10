package com.loanmanagement.payment.domain.model;

import lombok.Builder;
import lombok.Value;
import lombok.With;

import java.time.LocalDateTime;

/**
 * Value object representing escrow details for a payment.
 */
@Value
@Builder
@With
public class EscrowDetails {
    
    String escrowAccountId;
    String escrowAccountName;
    String escrowAmount;
    String escrowCurrency;
    String escrowType;
    String escrowStatus;
    LocalDateTime escrowCreatedAt;
    LocalDateTime escrowReleaseDate;
    String escrowConditions;
    String escrowAgentId;
    String escrowAgentName;
    boolean isEscrowRequired;
    
    public static EscrowDetails create(
            String escrowAccountId,
            String escrowAccountName,
            String escrowAmount,
            String escrowCurrency,
            String escrowType,
            String escrowStatus,
            LocalDateTime escrowReleaseDate,
            String escrowConditions,
            String escrowAgentId,
            String escrowAgentName,
            boolean isEscrowRequired) {
        
        if (isEscrowRequired) {
            if (escrowAccountId == null || escrowAccountId.trim().isEmpty()) {
                throw new IllegalArgumentException("Escrow account ID cannot be null or empty when escrow is required");
            }
            if (escrowAccountName == null || escrowAccountName.trim().isEmpty()) {
                throw new IllegalArgumentException("Escrow account name cannot be null or empty when escrow is required");
            }
            if (escrowAmount == null || escrowAmount.trim().isEmpty()) {
                throw new IllegalArgumentException("Escrow amount cannot be null or empty when escrow is required");
            }
            if (escrowCurrency == null || escrowCurrency.trim().isEmpty()) {
                throw new IllegalArgumentException("Escrow currency cannot be null or empty when escrow is required");
            }
            if (escrowType == null || escrowType.trim().isEmpty()) {
                throw new IllegalArgumentException("Escrow type cannot be null or empty when escrow is required");
            }
            if (escrowStatus == null || escrowStatus.trim().isEmpty()) {
                throw new IllegalArgumentException("Escrow status cannot be null or empty when escrow is required");
            }
            if (escrowAgentId == null || escrowAgentId.trim().isEmpty()) {
                throw new IllegalArgumentException("Escrow agent ID cannot be null or empty when escrow is required");
            }
            if (escrowAgentName == null || escrowAgentName.trim().isEmpty()) {
                throw new IllegalArgumentException("Escrow agent name cannot be null or empty when escrow is required");
            }
        }
        
        return EscrowDetails.builder()
                .escrowAccountId(escrowAccountId != null ? escrowAccountId.trim() : null)
                .escrowAccountName(escrowAccountName != null ? escrowAccountName.trim() : null)
                .escrowAmount(escrowAmount != null ? escrowAmount.trim() : null)
                .escrowCurrency(escrowCurrency != null ? escrowCurrency.trim() : null)
                .escrowType(escrowType != null ? escrowType.trim() : null)
                .escrowStatus(escrowStatus != null ? escrowStatus.trim() : null)
                .escrowCreatedAt(LocalDateTime.now())
                .escrowReleaseDate(escrowReleaseDate)
                .escrowConditions(escrowConditions != null ? escrowConditions.trim() : null)
                .escrowAgentId(escrowAgentId != null ? escrowAgentId.trim() : null)
                .escrowAgentName(escrowAgentName != null ? escrowAgentName.trim() : null)
                .isEscrowRequired(isEscrowRequired)
                .build();
    }
    
    public boolean isEscrowActive() {
        return isEscrowRequired && "ACTIVE".equals(escrowStatus);
    }
    
    public boolean isEscrowReleased() {
        return "RELEASED".equals(escrowStatus);
    }
    
    public boolean isEscrowExpired() {
        return escrowReleaseDate != null && LocalDateTime.now().isAfter(escrowReleaseDate);
    }
}