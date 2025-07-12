package com.amanahfi.platform.cbdc.domain;

import com.amanahfi.platform.shared.domain.Money;
import lombok.Builder;
import lombok.Value;

import java.time.Instant;

/**
 * Balance summary for Digital Dirham wallet
 */
@Value
@Builder
public class BalanceSummary {
    String walletId;
    String ownerId;
    Money currentBalance;
    long totalTransactions;
    long pendingTransactions;
    Instant lastUpdated;
}