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
 * Command to burn Digital Dirham (Central Bank only)
 */
@Value
@Builder
public class BurnDigitalDirhamCommand implements Command {
    UUID commandId = UUID.randomUUID();
    IdempotencyKey idempotencyKey;
    CommandMetadata metadata;
    
    String centralBankWalletId;
    Money amount;
    String authorization;
    String monetaryPolicyReference;
    
    @Override
    public void validate() {
        Objects.requireNonNull(idempotencyKey, "Idempotency key cannot be null");
        Objects.requireNonNull(centralBankWalletId, "Central Bank wallet ID cannot be null");
        Objects.requireNonNull(amount, "Amount cannot be null");
        Objects.requireNonNull(authorization, "Authorization cannot be null");
        
        if (centralBankWalletId.trim().isEmpty()) {
            throw new IllegalArgumentException("Central Bank wallet ID cannot be empty");
        }
        
        if (!amount.getCurrency().getCurrencyCode().equals("AED")) {
            throw new IllegalArgumentException("Digital Dirham burning must be in AED");
        }
        
        if (amount.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Burn amount must be positive");
        }
        
        if (authorization.trim().isEmpty()) {
            throw new IllegalArgumentException("Authorization cannot be empty");
        }
        
        // Large burning requires additional validation
        if (amount.getAmount().compareTo(new BigDecimal("1000000000")) > 0) { // 1 billion AED
            if (monetaryPolicyReference == null || monetaryPolicyReference.trim().isEmpty()) {
                throw new IllegalArgumentException("Monetary policy reference required for large burning operations");
            }
        }
    }
}