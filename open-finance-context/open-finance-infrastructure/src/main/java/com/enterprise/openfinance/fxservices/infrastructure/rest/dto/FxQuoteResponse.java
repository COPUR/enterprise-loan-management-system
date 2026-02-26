package com.enterprise.openfinance.fxservices.infrastructure.rest.dto;

import com.enterprise.openfinance.fxservices.domain.model.FxQuote;
import com.enterprise.openfinance.fxservices.domain.model.FxQuoteItemResult;
import com.enterprise.openfinance.fxservices.domain.model.FxQuoteResult;
import com.fasterxml.jackson.annotation.JsonProperty;

public record FxQuoteResponse(
        @JsonProperty("Data") Data data,
        @JsonProperty("Links") Links links
) {

    public static FxQuoteResponse from(FxQuoteResult result, String self) {
        return new FxQuoteResponse(
                new Data(Quote.from(result.quote())),
                new Links(self)
        );
    }

    public static FxQuoteResponse from(FxQuoteItemResult result, String self) {
        return new FxQuoteResponse(
                new Data(Quote.from(result.quote())),
                new Links(self)
        );
    }

    public record Data(
            @JsonProperty("Quote") Quote quote
    ) {
    }

    public record Links(
            @JsonProperty("Self") String self
    ) {
    }

    public record Quote(
            @JsonProperty("QuoteId") String quoteId,
            @JsonProperty("Status") String status,
            @JsonProperty("SourceAmount") Amount sourceAmount,
            @JsonProperty("TargetAmount") Amount targetAmount,
            @JsonProperty("ExchangeRate") String exchangeRate,
            @JsonProperty("ValidUntil") String validUntil
    ) {

        static Quote from(FxQuote quote) {
            return new Quote(
                    quote.quoteId(),
                    quote.status().apiValue(),
                    new Amount(quote.sourceAmount().toPlainString(), quote.sourceCurrency()),
                    new Amount(quote.targetAmount().toPlainString(), quote.targetCurrency()),
                    quote.exchangeRate().toPlainString(),
                    quote.validUntil().toString()
            );
        }
    }

    public record Amount(
            @JsonProperty("Amount") String amount,
            @JsonProperty("Currency") String currency
    ) {
    }
}
