package com.enterprise.openfinance.insurancequotes.infrastructure.rest.dto;

import com.enterprise.openfinance.insurancequotes.domain.model.MotorInsuranceQuote;
import com.enterprise.openfinance.insurancequotes.domain.model.MotorQuoteItemResult;
import com.enterprise.openfinance.insurancequotes.domain.model.MotorQuoteResult;
import com.fasterxml.jackson.annotation.JsonProperty;

public record MotorQuoteResponse(
        @JsonProperty("Data") Data data,
        @JsonProperty("Links") Links links
) {

    public static MotorQuoteResponse from(MotorQuoteResult result, String self) {
        return new MotorQuoteResponse(
                new Data(QuoteData.from(result.quote())),
                new Links(self)
        );
    }

    public static MotorQuoteResponse from(MotorQuoteItemResult result, String self) {
        return new MotorQuoteResponse(
                new Data(QuoteData.from(result.quote())),
                new Links(self)
        );
    }

    public record Data(
            @JsonProperty("Quote") QuoteData quote
    ) {
    }

    public record Links(
            @JsonProperty("Self") String self
    ) {
    }

    public record QuoteData(
            @JsonProperty("QuoteId") String quoteId,
            @JsonProperty("Status") String status,
            @JsonProperty("Premium") Premium premium,
            @JsonProperty("ValidUntil") String validUntil,
            @JsonProperty("Vehicle") Vehicle vehicle,
            @JsonProperty("Driver") Driver driver,
            @JsonProperty("PolicyId") String policyId,
            @JsonProperty("PolicyNumber") String policyNumber,
            @JsonProperty("CertificateId") String certificateId,
            @JsonProperty("PaymentReference") String paymentReference
    ) {

        static QuoteData from(MotorInsuranceQuote quote) {
            return new QuoteData(
                    quote.quoteId(),
                    quote.status().apiValue(),
                    new Premium(quote.premiumAmount().toPlainString(), quote.currency()),
                    quote.validUntil().toString(),
                    new Vehicle(quote.vehicleMake(), quote.vehicleModel(), quote.vehicleYear()),
                    new Driver(quote.driverAge(), quote.licenseDurationYears()),
                    quote.policyId(),
                    quote.policyNumber(),
                    quote.certificateId(),
                    quote.paymentReference()
            );
        }
    }

    public record Premium(
            @JsonProperty("Amount") String amount,
            @JsonProperty("Currency") String currency
    ) {
    }

    public record Vehicle(
            @JsonProperty("Make") String make,
            @JsonProperty("Model") String model,
            @JsonProperty("Year") int year
    ) {
    }

    public record Driver(
            @JsonProperty("Age") int age,
            @JsonProperty("LicenseDuration") int licenseDuration
    ) {
    }
}
