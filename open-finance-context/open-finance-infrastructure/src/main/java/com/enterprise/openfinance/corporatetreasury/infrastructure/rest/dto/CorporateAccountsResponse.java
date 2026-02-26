package com.enterprise.openfinance.corporatetreasury.infrastructure.rest.dto;

import com.enterprise.openfinance.corporatetreasury.domain.model.CorporateAccountListResult;
import com.enterprise.openfinance.corporatetreasury.domain.model.CorporateAccountSnapshot;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public record CorporateAccountsResponse(
        @JsonProperty("Data") Data data,
        @JsonProperty("Links") Links links,
        @JsonProperty("Meta") Meta meta
) {

    public static CorporateAccountsResponse from(CorporateAccountListResult result, String selfLink) {
        List<AccountData> accounts = result.accounts().stream()
                .map(CorporateAccountsResponse::toAccountData)
                .toList();

        return new CorporateAccountsResponse(
                new Data(accounts),
                new Links(selfLink),
                new Meta(accounts.size())
        );
    }

    private static AccountData toAccountData(CorporateAccountSnapshot account) {
        return new AccountData(
                account.accountId(),
                account.masterAccountId(),
                account.maskedIban(),
                account.currency(),
                account.status(),
                account.accountType(),
                account.virtual()
        );
    }

    public record Data(
            @JsonProperty("Account") List<AccountData> accounts
    ) {
    }

    public record AccountData(
            @JsonProperty("AccountId") String accountId,
            @JsonInclude(JsonInclude.Include.NON_NULL)
            @JsonProperty("MasterAccountId") String masterAccountId,
            @JsonProperty("IBAN") String iban,
            @JsonProperty("Currency") String currency,
            @JsonProperty("Status") String status,
            @JsonProperty("AccountType") String accountType,
            @JsonProperty("Virtual") boolean virtual
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
