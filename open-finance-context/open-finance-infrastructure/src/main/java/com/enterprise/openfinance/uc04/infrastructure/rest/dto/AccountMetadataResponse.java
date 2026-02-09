package com.enterprise.openfinance.uc04.infrastructure.rest.dto;

import com.enterprise.openfinance.uc04.domain.model.AccountSchemeMetadata;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

public record AccountMetadataResponse(
        @JsonProperty("Data") Data data,
        @JsonProperty("Links") Links links
) {

    public static AccountMetadataResponse from(AccountSchemeMetadata metadata) {
        return new AccountMetadataResponse(
                new Data(new AccountData(
                        metadata.accountId(),
                        metadata.schemeName(),
                        metadata.secondaryIdentification()
                )),
                new Links("/open-finance/v1/metadata/accounts/" + metadata.accountId())
        );
    }

    public record Data(
            @JsonProperty("Account") AccountData account
    ) {
    }

    public record AccountData(
            @JsonProperty("AccountId") String accountId,
            @JsonProperty("SchemeName") String schemeName,
            @JsonInclude(JsonInclude.Include.NON_NULL)
            @JsonProperty("SecondaryIdentification") String secondaryIdentification
    ) {
    }

    public record Links(
            @JsonProperty("Self") String self
    ) {
    }
}
