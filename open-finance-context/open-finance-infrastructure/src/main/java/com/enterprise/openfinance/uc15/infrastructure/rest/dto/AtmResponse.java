package com.enterprise.openfinance.uc15.infrastructure.rest.dto;

import com.enterprise.openfinance.uc15.domain.model.AtmListResult;
import com.enterprise.openfinance.uc15.domain.model.AtmLocation;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public record AtmResponse(
        @JsonProperty("Data") Data data,
        @JsonProperty("Links") Links links,
        @JsonProperty("Meta") Meta meta
) {

    public static AtmResponse from(AtmListResult result, String self) {
        List<AtmData> atms = result.atms().stream()
                .map(AtmData::from)
                .toList();
        return new AtmResponse(
                new Data(atms),
                new Links(self),
                new Meta(atms.size())
        );
    }

    public record Data(
            @JsonProperty("ATM") List<AtmData> atm
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

    public record AtmData(
            @JsonProperty("AtmId") String atmId,
            @JsonProperty("Name") String name,
            @JsonProperty("Status") String status,
            @JsonProperty("Latitude") double latitude,
            @JsonProperty("Longitude") double longitude,
            @JsonProperty("Address") String address,
            @JsonProperty("City") String city,
            @JsonProperty("Country") String country,
            @JsonProperty("Accessibility") String accessibility,
            @JsonProperty("Services") List<String> services,
            @JsonProperty("UpdatedAt") String updatedAt
    ) {

        static AtmData from(AtmLocation location) {
            return new AtmData(
                    location.atmId(),
                    location.name(),
                    location.status().apiValue(),
                    location.latitude(),
                    location.longitude(),
                    location.address(),
                    location.city(),
                    location.country(),
                    location.accessibility(),
                    location.services(),
                    location.updatedAt().toString()
            );
        }
    }
}
