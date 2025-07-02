package com.bank.loanmanagement.loan.application.port.out;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;

/**
 * Port for market data services
 * Hexagonal architecture - outbound port
 */
public interface MarketDataPort {

    /**
     * Get current market conditions for loan pricing
     */
    MarketConditions getCurrentMarketConditions();

    record MarketConditions(
            BigDecimal baseInterestRate,
            BigDecimal federalRate,
            String economicOutlook,
            Map<String, BigDecimal> competitorRates,
            LocalDateTime timestamp
    ) {}
}