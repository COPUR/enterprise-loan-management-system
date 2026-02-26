package com.enterprise.openfinance.insurancequotes.domain.port.in;

import com.enterprise.openfinance.insurancequotes.domain.command.AcceptMotorQuoteCommand;
import com.enterprise.openfinance.insurancequotes.domain.command.CreateMotorQuoteCommand;
import com.enterprise.openfinance.insurancequotes.domain.model.MotorQuoteItemResult;
import com.enterprise.openfinance.insurancequotes.domain.model.MotorQuoteResult;
import com.enterprise.openfinance.insurancequotes.domain.query.GetMotorQuoteQuery;

import java.util.Optional;

public interface InsuranceQuoteUseCase {

    MotorQuoteResult createQuote(CreateMotorQuoteCommand command);

    MotorQuoteResult acceptQuote(AcceptMotorQuoteCommand command);

    Optional<MotorQuoteItemResult> getQuote(GetMotorQuoteQuery query);
}
