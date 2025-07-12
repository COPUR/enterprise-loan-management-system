package com.amanahfi.platform.cbdc.port.in;

import com.amanahfi.platform.cbdc.domain.WalletType;
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
 * Command to create a new Digital Dirham wallet
 */
@Value
@Builder
public class CreateWalletCommand implements Command {
    UUID commandId = UUID.randomUUID();
    IdempotencyKey idempotencyKey;
    CommandMetadata metadata;
    
    String ownerId;
    WalletType walletType;
    Money initialBalance;
    String cordaNodeId;
    String notaryId;
    
    @Override
    public void validate() {
        Objects.requireNonNull(idempotencyKey, "Idempotency key cannot be null");
        Objects.requireNonNull(ownerId, "Owner ID cannot be null");
        Objects.requireNonNull(walletType, "Wallet type cannot be null");
        Objects.requireNonNull(initialBalance, "Initial balance cannot be null");
        Objects.requireNonNull(cordaNodeId, "Corda node ID cannot be null");
        Objects.requireNonNull(notaryId, "Notary ID cannot be null");
        
        if (ownerId.trim().isEmpty()) {
            throw new IllegalArgumentException("Owner ID cannot be empty");
        }
        
        if (!initialBalance.getCurrency().getCurrencyCode().equals("AED")) {
            throw new IllegalArgumentException("Digital Dirham must be in AED currency");
        }
        
        if (initialBalance.getAmount().compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Initial balance cannot be negative");
        }
        
        // Central Bank wallets can have any initial balance
        // Other wallets must start with zero balance
        if (walletType != WalletType.CENTRAL_BANK && 
            initialBalance.getAmount().compareTo(BigDecimal.ZERO) != 0) {
            throw new IllegalArgumentException("Non-Central Bank wallets must start with zero balance");
        }
    }
}