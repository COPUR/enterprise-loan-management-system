package com.enterprise.openfinance.fxservices.domain.port.in;

import com.enterprise.openfinance.fxservices.domain.command.CreateFxQuoteCommand;
import com.enterprise.openfinance.fxservices.domain.command.ExecuteFxDealCommand;
import com.enterprise.openfinance.fxservices.domain.model.FxDealResult;
import com.enterprise.openfinance.fxservices.domain.model.FxQuoteItemResult;
import com.enterprise.openfinance.fxservices.domain.model.FxQuoteResult;
import com.enterprise.openfinance.fxservices.domain.query.GetFxQuoteQuery;

import java.util.Optional;

public interface FxUseCase {

    FxQuoteResult createQuote(CreateFxQuoteCommand command);

    FxDealResult executeDeal(ExecuteFxDealCommand command);

    Optional<FxQuoteItemResult> getQuote(GetFxQuoteQuery query);
}
