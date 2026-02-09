package com.enterprise.openfinance.uc02.infrastructure.rest.dto;

import com.enterprise.openfinance.uc02.domain.model.AccountSnapshot;
import com.fasterxml.jackson.annotation.JsonProperty;

public record AccountResponse(
        @JsonProperty("Data") Data data,
        @JsonProperty("Links") Links links
) {

    public static AccountResponse from(AccountSnapshot account) {
        return new AccountResponse(
                new Data(new AccountData(
                        account.accountId(),
                        account.maskedIban(),
                        account.currency(),
                        account.accountType(),
                        account.status(),
                        account.schemeName(),
                        account.name()
                )),
                new Links("/open-finance/v1/accounts/" + account.accountId())
        );
    }

    public record Data(
            @JsonProperty("Account") AccountData account
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
}
