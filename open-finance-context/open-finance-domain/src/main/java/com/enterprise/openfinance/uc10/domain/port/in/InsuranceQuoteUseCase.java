package com.enterprise.openfinance.uc10.domain.port.in;

import com.enterprise.openfinance.uc10.domain.command.AcceptMotorQuoteCommand;
import com.enterprise.openfinance.uc10.domain.command.CreateMotorQuoteCommand;
import com.enterprise.openfinance.uc10.domain.model.MotorQuoteItemResult;
import com.enterprise.openfinance.uc10.domain.model.MotorQuoteResult;
import com.enterprise.openfinance.uc10.domain.query.GetMotorQuoteQuery;

import java.util.Optional;

public interface InsuranceQuoteUseCase {

    MotorQuoteResult createQuote(CreateMotorQuoteCommand command);

    MotorQuoteResult acceptQuote(AcceptMotorQuoteCommand command);

    Optional<MotorQuoteItemResult> getQuote(GetMotorQuoteQuery query);
}
