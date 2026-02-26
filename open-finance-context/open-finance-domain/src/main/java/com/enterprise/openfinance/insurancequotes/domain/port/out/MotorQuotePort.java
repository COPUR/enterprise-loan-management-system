package com.enterprise.openfinance.insurancequotes.domain.port.out;

import com.enterprise.openfinance.insurancequotes.domain.model.MotorInsuranceQuote;

import java.util.Optional;

public interface MotorQuotePort {

    MotorInsuranceQuote save(MotorInsuranceQuote quote);

    Optional<MotorInsuranceQuote> findById(String quoteId);
}
