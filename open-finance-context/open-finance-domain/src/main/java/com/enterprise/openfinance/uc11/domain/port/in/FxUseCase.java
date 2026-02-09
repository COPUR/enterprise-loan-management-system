package com.enterprise.openfinance.uc11.domain.port.in;

import com.enterprise.openfinance.uc11.domain.command.CreateFxQuoteCommand;
import com.enterprise.openfinance.uc11.domain.command.ExecuteFxDealCommand;
import com.enterprise.openfinance.uc11.domain.model.FxDealResult;
import com.enterprise.openfinance.uc11.domain.model.FxQuoteItemResult;
import com.enterprise.openfinance.uc11.domain.model.FxQuoteResult;
import com.enterprise.openfinance.uc11.domain.query.GetFxQuoteQuery;

import java.util.Optional;

public interface FxUseCase {

    FxQuoteResult createQuote(CreateFxQuoteCommand command);

    FxDealResult executeDeal(ExecuteFxDealCommand command);

    Optional<FxQuoteItemResult> getQuote(GetFxQuoteQuery query);
}
