package com.enterprise.openfinance.fxservices.infrastructure.rest.dto;

import com.enterprise.openfinance.fxservices.domain.model.FxDeal;
import com.enterprise.openfinance.fxservices.domain.model.FxDealResult;
import com.fasterxml.jackson.annotation.JsonProperty;

public record FxDealResponse(
        @JsonProperty("Data") Data data,
        @JsonProperty("Links") Links links
) {

    public static FxDealResponse from(FxDealResult result, String self) {
        return new FxDealResponse(
                new Data(Deal.from(result.deal())),
                new Links(self)
        );
    }

    public record Data(
            @JsonProperty("Deal") Deal deal
    ) {
    }

    public record Links(
            @JsonProperty("Self") String self
    ) {
    }

    public record Deal(
            @JsonProperty("DealId") String dealId,
            @JsonProperty("QuoteId") String quoteId,
            @JsonProperty("Status") String status,
            @JsonProperty("BookedAt") String bookedAt,
            @JsonProperty("SourceAmount") Amount sourceAmount,
            @JsonProperty("TargetAmount") Amount targetAmount,
            @JsonProperty("ExchangeRate") String exchangeRate
    ) {

        static Deal from(FxDeal deal) {
            return new Deal(
                    deal.dealId(),
                    deal.quoteId(),
                    deal.status().apiValue(),
                    deal.bookedAt().toString(),
                    new Amount(deal.sourceAmount().toPlainString(), deal.sourceCurrency()),
                    new Amount(deal.targetAmount().toPlainString(), deal.targetCurrency()),
                    deal.exchangeRate().toPlainString()
            );
        }
    }

    public record Amount(
            @JsonProperty("Amount") String amount,
            @JsonProperty("Currency") String currency
    ) {
    }
}
