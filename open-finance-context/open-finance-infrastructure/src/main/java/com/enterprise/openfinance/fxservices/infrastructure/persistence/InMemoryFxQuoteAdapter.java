package com.enterprise.openfinance.fxservices.infrastructure.persistence;

import com.enterprise.openfinance.fxservices.domain.model.FxQuote;
import com.enterprise.openfinance.fxservices.domain.model.FxQuoteStatus;
import com.enterprise.openfinance.fxservices.domain.port.out.FxQuotePort;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class InMemoryFxQuoteAdapter implements FxQuotePort {

    private final ConcurrentHashMap<String, FxQuote> data = new ConcurrentHashMap<>();

    public InMemoryFxQuoteAdapter() {
        data.put("Q-EXPIRED-001", new FxQuote(
                "Q-EXPIRED-001",
                "TPP-001",
                "AED",
                "USD",
                new BigDecimal("1000.00"),
                new BigDecimal("272.29"),
                new BigDecimal("0.272290"),
                FxQuoteStatus.QUOTED,
                Instant.parse("2000-01-01T00:00:00Z"),
                Instant.parse("1999-12-31T23:59:00Z"),
                Instant.parse("1999-12-31T23:59:00Z")
        ));
    }

    @Override
    public FxQuote save(FxQuote quote) {
        data.put(quote.quoteId(), quote);
        return quote;
    }

    @Override
    public Optional<FxQuote> findById(String quoteId) {
        return Optional.ofNullable(data.get(quoteId));
    }
}
