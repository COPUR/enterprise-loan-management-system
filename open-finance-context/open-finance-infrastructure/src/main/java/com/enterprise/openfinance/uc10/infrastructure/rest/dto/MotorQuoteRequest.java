package com.enterprise.openfinance.uc10.infrastructure.rest.dto;

import com.enterprise.openfinance.uc10.domain.command.CreateMotorQuoteCommand;
import com.fasterxml.jackson.annotation.JsonProperty;

public record MotorQuoteRequest(
        @JsonProperty("Data") Data data
) {

    public CreateMotorQuoteCommand toCommand(String tppId, String interactionId) {
        if (data == null || data.vehicleDetails == null || data.driverDetails == null) {
            throw new IllegalArgumentException("Request Data is required");
        }
        if (data.vehicleDetails.year == null) {
            throw new IllegalArgumentException("VehicleDetails.Year is required");
        }
        if (data.driverDetails.age == null) {
            throw new IllegalArgumentException("DriverDetails.Age is required");
        }
        if (data.driverDetails.licenseDuration == null) {
            throw new IllegalArgumentException("DriverDetails.LicenseDuration is required");
        }
        return new CreateMotorQuoteCommand(
                tppId,
                interactionId,
                data.vehicleDetails.make,
                data.vehicleDetails.model,
                data.vehicleDetails.year,
                data.driverDetails.age,
                data.driverDetails.licenseDuration
        );
    }

    public record Data(
            @JsonProperty("VehicleDetails") VehicleDetails vehicleDetails,
            @JsonProperty("DriverDetails") DriverDetails driverDetails
    ) {
    }

    public record VehicleDetails(
            @JsonProperty("Make") String make,
            @JsonProperty("Model") String model,
            @JsonProperty("Year") Integer year
    ) {
    }

    public record DriverDetails(
            @JsonProperty("Age") Integer age,
            @JsonProperty("LicenseDuration") Integer licenseDuration
    ) {
    }
}
