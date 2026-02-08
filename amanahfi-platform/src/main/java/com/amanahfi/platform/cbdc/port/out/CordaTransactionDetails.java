package com.amanahfi.platform.cbdc.port.out;

import com.amanahfi.platform.shared.domain.Money;
import lombok.Builder;
import lombok.Value;

import java.time.Instant;
import java.util.List;

/**
 * Details of a Corda transaction
 */
@Value
@Builder
public class CordaTransactionDetails {
    String transactionHash;
    String fromWalletId;
    String toWalletId;
    Money amount;
    String reference;
    String transactionType;
    Instant timestamp;
    String status;
    List<String> notarySignatures;
    long blockHeight;
    String blockHash;
}