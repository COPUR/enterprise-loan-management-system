package com.enterprise.openfinance.uc10.domain.port.out;

import com.enterprise.openfinance.uc10.domain.model.MotorInsuranceQuote;

import java.util.Optional;

public interface MotorQuotePort {

    MotorInsuranceQuote save(MotorInsuranceQuote quote);

    Optional<MotorInsuranceQuote> findById(String quoteId);
}
