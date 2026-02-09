package com.enterprise.openfinance.uc10.infrastructure.persistence;

import com.enterprise.openfinance.uc10.domain.model.MotorInsuranceQuote;
import com.enterprise.openfinance.uc10.domain.port.out.MotorQuotePort;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class InMemoryMotorQuoteAdapter implements MotorQuotePort {

    private final ConcurrentHashMap<String, MotorInsuranceQuote> data = new ConcurrentHashMap<>();

    @Override
    public MotorInsuranceQuote save(MotorInsuranceQuote quote) {
        data.put(quote.quoteId(), quote);
        return quote;
    }

    @Override
    public Optional<MotorInsuranceQuote> findById(String quoteId) {
        return Optional.ofNullable(data.get(quoteId));
    }
}
