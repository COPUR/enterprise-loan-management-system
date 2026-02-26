package com.enterprise.openfinance.insurancequotes.domain.port.out;

import com.enterprise.openfinance.insurancequotes.domain.command.CreateMotorQuoteCommand;

import java.math.BigDecimal;

public interface QuotePricingPort {

    BigDecimal calculatePremium(CreateMotorQuoteCommand command);
}
