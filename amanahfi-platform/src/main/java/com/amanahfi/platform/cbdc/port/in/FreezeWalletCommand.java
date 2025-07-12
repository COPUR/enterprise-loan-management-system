package com.amanahfi.platform.cbdc.port.in;

import com.amanahfi.platform.shared.command.Command;
import com.amanahfi.platform.shared.command.CommandMetadata;
import com.amanahfi.platform.shared.idempotence.IdempotencyKey;
import lombok.Builder;
import lombok.Value;

import java.util.Objects;
import java.util.UUID;

/**
 * Command to freeze a Digital Dirham wallet
 */
@Value
@Builder
public class FreezeWalletCommand implements Command {
    UUID commandId = UUID.randomUUID();
    IdempotencyKey idempotencyKey;
    CommandMetadata metadata;
    
    String walletId;
    String reason;
    String authorizedBy;
    String regulatoryReference;
    
    @Override
    public void validate() {
        Objects.requireNonNull(idempotencyKey, "Idempotency key cannot be null");
        Objects.requireNonNull(walletId, "Wallet ID cannot be null");
        Objects.requireNonNull(reason, "Reason cannot be null");
        Objects.requireNonNull(authorizedBy, "Authorized by cannot be null");
        
        if (walletId.trim().isEmpty()) {
            throw new IllegalArgumentException("Wallet ID cannot be empty");
        }
        
        if (reason.trim().isEmpty()) {
            throw new IllegalArgumentException("Reason cannot be empty");
        }
        
        if (authorizedBy.trim().isEmpty()) {
            throw new IllegalArgumentException("Authorized by cannot be empty");
        }
    }
}