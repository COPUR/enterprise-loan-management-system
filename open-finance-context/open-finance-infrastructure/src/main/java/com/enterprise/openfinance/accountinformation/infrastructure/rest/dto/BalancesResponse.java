package com.enterprise.openfinance.accountinformation.infrastructure.rest.dto;

import com.enterprise.openfinance.accountinformation.domain.model.BalanceSnapshot;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.math.RoundingMode;
import java.util.List;

public record BalancesResponse(
        @JsonProperty("Data") Data data,
        @JsonProperty("Links") Links links,
        @JsonProperty("Meta") Meta meta
) {

    public static BalancesResponse from(String accountId, List<BalanceSnapshot> balances) {
        List<BalanceData> data = balances.stream()
                .map(balance -> new BalanceData(
                        balance.accountId(),
                        balance.balanceType(),
                        new AmountData(balance.amount().setScale(2, RoundingMode.HALF_UP).toPlainString(), balance.currency()),
                        balance.asOf().toString()
                ))
                .toList();

        return new BalancesResponse(
                new Data(data),
                new Links("/open-finance/v1/accounts/" + accountId + "/balances"),
                new Meta(data.size())
        );
    }

    public record Data(
            @JsonProperty("Balance") List<BalanceData> balances
    ) {
    }

    public record BalanceData(
            @JsonProperty("AccountId") String accountId,
            @JsonProperty("Type") String type,
            @JsonProperty("Amount") AmountData amount,
            @JsonProperty("LastChangeDateTime") String lastChangeDateTime
    ) {
    }

    public record AmountData(
            @JsonProperty("Amount") String amount,
            @JsonProperty("Currency") String currency
    ) {
    }

    public record Links(
            @JsonProperty("Self") String self
    ) {
    }

    public record Meta(
            @JsonProperty("TotalRecords") int totalRecords
    ) {
    }
}
