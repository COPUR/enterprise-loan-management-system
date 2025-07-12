package com.amanahfi.platform.cbdc.port.in;

import com.amanahfi.platform.shared.command.Command;
import com.amanahfi.platform.shared.command.CommandMetadata;
import com.amanahfi.platform.shared.domain.Money;
import com.amanahfi.platform.shared.idempotence.IdempotencyKey;
import lombok.Builder;
import lombok.Value;

import java.math.BigDecimal;
import java.util.Objects;
import java.util.UUID;

/**
 * Command to process Islamic finance compliant Digital Dirham transfer
 */
@Value
@Builder
public class IslamicFinanceTransferCommand implements Command {
    UUID commandId = UUID.randomUUID();
    IdempotencyKey idempotencyKey;
    CommandMetadata metadata;
    
    String fromWalletId;
    String toWalletId;
    Money amount;
    String reference;
    String islamicFinanceProductId;
    String shariaReferenceNumber;
    
    @Override
    public void validate() {
        Objects.requireNonNull(idempotencyKey, "Idempotency key cannot be null");
        Objects.requireNonNull(fromWalletId, "From wallet ID cannot be null");
        Objects.requireNonNull(toWalletId, "To wallet ID cannot be null");
        Objects.requireNonNull(amount, "Amount cannot be null");
        Objects.requireNonNull(reference, "Reference cannot be null");
        Objects.requireNonNull(islamicFinanceProductId, "Islamic finance product ID cannot be null");
        
        if (fromWalletId.trim().isEmpty()) {
            throw new IllegalArgumentException("From wallet ID cannot be empty");
        }
        
        if (toWalletId.trim().isEmpty()) {
            throw new IllegalArgumentException("To wallet ID cannot be empty");
        }
        
        if (fromWalletId.equals(toWalletId)) {
            throw new IllegalArgumentException("Cannot transfer to the same wallet");
        }
        
        if (!amount.getCurrency().getCurrencyCode().equals("AED")) {
            throw new IllegalArgumentException("Islamic finance transfers must be in AED");
        }
        
        if (amount.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Transfer amount must be positive");
        }
        
        if (reference.trim().isEmpty()) {
            throw new IllegalArgumentException("Reference cannot be empty");
        }
        
        if (islamicFinanceProductId.trim().isEmpty()) {
            throw new IllegalArgumentException("Islamic finance product ID cannot be empty");
        }
        
        // Additional Islamic finance validations
        validateShariaCompliance();
    }
    
    private void validateShariaCompliance() {
        // Sharia compliance validations
        if (amount.getAmount().compareTo(new BigDecimal("1000000")) > 0) {
            if (shariaReferenceNumber == null || shariaReferenceNumber.trim().isEmpty()) {
                throw new IllegalArgumentException("Sharia reference number required for transfers above 1M AED");
            }
        }
    }
}