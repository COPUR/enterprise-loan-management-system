package com.enterprise.openfinance.personalfinancialdata.infrastructure.rest.dto;

import com.enterprise.openfinance.personalfinancialdata.domain.model.AccountSnapshot;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public record AccountsResponse(
        @JsonProperty("Data") Data data,
        @JsonProperty("Links") Links links,
        @JsonProperty("Meta") Meta meta
) {

    public static AccountsResponse from(List<AccountSnapshot> accounts, String selfLink) {
        List<AccountData> accountData = accounts.stream()
                .map(account -> new AccountData(
                        account.accountId(),
                        account.maskedIban(),
                        account.currency(),
                        account.accountType(),
                        account.status(),
                        account.schemeName(),
                        account.name()
                ))
                .toList();
        return new AccountsResponse(new Data(accountData), new Links(selfLink), new Meta(accountData.size()));
    }

    public record Data(
            @JsonProperty("Account") List<AccountData> accounts
    ) {
    }

    public record AccountData(
            @JsonProperty("AccountId") String accountId,
            @JsonProperty("IBAN") String iban,
            @JsonProperty("Currency") String currency,
            @JsonProperty("AccountType") String accountType,
            @JsonProperty("Status") String status,
            @JsonProperty("SchemeName") String schemeName,
            @JsonProperty("Name") String name
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
