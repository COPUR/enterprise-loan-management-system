package com.enterprise.openfinance.uc10.infrastructure.rest.dto;

import com.enterprise.openfinance.uc10.domain.command.AcceptMotorQuoteCommand;
import com.fasterxml.jackson.annotation.JsonProperty;

public record MotorQuoteActionRequest(
        @JsonProperty("Data") Data data
) {

    public AcceptMotorQuoteCommand toCommand(String tppId,
                                             String quoteId,
                                             String idempotencyKey,
                                             String interactionId) {
        if (data == null) {
            throw new IllegalArgumentException("Request Data is required");
        }

        Integer vehicleYear = data.vehicleDetails == null ? null : data.vehicleDetails.year;
        Integer driverAge = data.driverDetails == null ? null : data.driverDetails.age;
        Integer licenseDuration = data.driverDetails == null ? null : data.driverDetails.licenseDuration;

        return new AcceptMotorQuoteCommand(
                tppId,
                quoteId,
                idempotencyKey,
                interactionId,
                data.action,
                data.paymentReference,
                data.vehicleDetails == null ? null : data.vehicleDetails.make,
                data.vehicleDetails == null ? null : data.vehicleDetails.model,
                vehicleYear,
                driverAge,
                licenseDuration
        );
    }

    public record Data(
            @JsonProperty("Action") String action,
            @JsonProperty("PaymentReference") String paymentReference,
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
