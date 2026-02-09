package com.enterprise.openfinance.uc10.domain.port.out;

import com.enterprise.openfinance.uc10.domain.command.CreateMotorQuoteCommand;

import java.math.BigDecimal;

public interface QuotePricingPort {

    BigDecimal calculatePremium(CreateMotorQuoteCommand command);
}
