package com.enterprise.openfinance.bankingmetadata.infrastructure.rest.dto;

import com.enterprise.openfinance.bankingmetadata.domain.model.MetadataListResult;
import com.enterprise.openfinance.bankingmetadata.domain.model.PartyMetadata;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public record PartiesMetadataResponse(
        @JsonProperty("Data") Data data,
        @JsonProperty("Links") Links links,
        @JsonProperty("Meta") Meta meta
) {

    public static PartiesMetadataResponse from(String accountId, MetadataListResult<PartyMetadata> result) {
        List<PartyData> partyData = result.items().stream()
                .map(party -> new PartyData(
                        party.accountId(),
                        party.fullLegalName(),
                        party.kycStatus(),
                        party.relationshipStartDate().toString()
                ))
                .toList();

        return new PartiesMetadataResponse(
                new Data(partyData),
                new Links("/open-finance/v1/metadata/accounts/" + accountId + "/parties"),
                new Meta(partyData.size())
        );
    }

    public record Data(
            @JsonProperty("Party") List<PartyData> parties
    ) {
    }

    public record PartyData(
            @JsonProperty("AccountId") String accountId,
            @JsonProperty("FullLegalName") String fullLegalName,
            @JsonProperty("KYCStatus") String kycStatus,
            @JsonProperty("RelationshipStartDate") String relationshipStartDate
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
