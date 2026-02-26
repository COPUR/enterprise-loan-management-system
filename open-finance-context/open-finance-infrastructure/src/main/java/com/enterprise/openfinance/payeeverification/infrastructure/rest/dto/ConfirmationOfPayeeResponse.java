package com.enterprise.openfinance.payeeverification.infrastructure.rest.dto;

import com.enterprise.openfinance.payeeverification.domain.model.AccountStatus;
import com.enterprise.openfinance.payeeverification.domain.model.ConfirmationResult;
import com.fasterxml.jackson.annotation.JsonProperty;

public record ConfirmationOfPayeeResponse(
        @JsonProperty("Data") Data data
) {
    public static ConfirmationOfPayeeResponse from(ConfirmationResult result) {
        return new ConfirmationOfPayeeResponse(
                new Data(
                        toApiStatus(result.accountStatus()),
                        result.nameMatched().apiValue(),
                        result.matchedName()
                )
        );
    }

    private static String toApiStatus(AccountStatus accountStatus) {
        return switch (accountStatus) {
            case ACTIVE -> "Active";
            case CLOSED -> "Closed";
            case DECEASED -> "Deceased";
            case UNKNOWN -> "Unknown";
        };
    }

    public record Data(
            @JsonProperty("AccountStatus") String accountStatus,
            @JsonProperty("NameMatched") String nameMatched,
            @JsonProperty("MatchedName") String matchedName
    ) {
    }
}
