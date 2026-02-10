package com.enterprise.openfinance.businessfinancialdata.infrastructure.rest.dto;

import com.enterprise.openfinance.businessfinancialdata.domain.model.CorporateBalanceListResult;
import com.enterprise.openfinance.businessfinancialdata.domain.model.CorporateBalanceSnapshot;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public record CorporateBalancesResponse(
        @JsonProperty("Data") Data data,
        @JsonProperty("Links") Links links,
        @JsonProperty("Meta") Meta meta
) {

    public static CorporateBalancesResponse from(String masterAccountId, CorporateBalanceListResult result) {
        List<BalanceData> balances = result.balances().stream()
                .map(balance -> toBalanceData(balance, result.masked()))
                .toList();

        return new CorporateBalancesResponse(
                new Data(balances),
                new Links("/open-finance/v1/corporate/accounts/" + masterAccountId + "/balances"),
                new Meta(balances.size())
        );
    }

    private static BalanceData toBalanceData(CorporateBalanceSnapshot balance, boolean masked) {
        String amount = masked ? "****" : balance.formattedAmount();
        return new BalanceData(
                balance.accountId(),
                balance.balanceType(),
                new AmountData(amount, balance.currency()),
                balance.asOf().toString()
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
