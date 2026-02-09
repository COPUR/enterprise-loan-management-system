package com.enterprise.openfinance.uc04.infrastructure.rest.dto;

import com.enterprise.openfinance.uc04.domain.model.MetadataQueryResult;
import com.enterprise.openfinance.uc04.domain.model.TransactionMetadata;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.math.RoundingMode;
import java.util.List;

public record TransactionMetadataResponse(
        @JsonProperty("Data") Data data,
        @JsonProperty("Links") Links links,
        @JsonProperty("Meta") Meta meta
) {

    public static TransactionMetadataResponse from(MetadataQueryResult<TransactionMetadata> page,
                                                   String selfLink,
                                                   String nextLink) {
        List<TransactionData> transactionData = page.items().stream()
                .map(tx -> new TransactionData(
                        tx.transactionId(),
                        tx.accountId(),
                        tx.bookingDateTime().toString(),
                        new AmountData(tx.amount().setScale(2, RoundingMode.HALF_UP).toPlainString(), tx.currency()),
                        new AmountData(tx.chargeAmount().setScale(2, RoundingMode.HALF_UP).toPlainString(), tx.currency()),
                        new MerchantDetails(
                                tx.merchantName(),
                                tx.categoryCode(),
                                new GeoLocationData(tx.geoLocation().latitude(), tx.geoLocation().longitude())
                        ),
                        tx.fxDetails() == null
                                ? null
                                : new FxDetailsData(
                                        tx.fxDetails().exchangeRate().setScale(6, RoundingMode.HALF_UP).toPlainString(),
                                        tx.fxDetails().originalCurrency(),
                                        tx.fxDetails().targetCurrency()
                                )
                ))
                .toList();

        return new TransactionMetadataResponse(
                new Data(transactionData),
                new Links(selfLink, nextLink),
                new Meta(page.page(), page.pageSize(), page.totalPages(), page.totalRecords())
        );
    }

    public record Data(
            @JsonProperty("Transaction") List<TransactionData> transactions
    ) {
    }

    public record TransactionData(
            @JsonProperty("TransactionId") String transactionId,
            @JsonProperty("AccountId") String accountId,
            @JsonProperty("BookingDateTime") String bookingDateTime,
            @JsonProperty("Amount") AmountData amount,
            @JsonProperty("ChargeAmount") AmountData chargeAmount,
            @JsonProperty("MerchantDetails") MerchantDetails merchantDetails,
            @JsonInclude(JsonInclude.Include.NON_NULL)
            @JsonProperty("FxDetails") FxDetailsData fxDetails
    ) {
    }

    public record AmountData(
            @JsonProperty("Amount") String amount,
            @JsonProperty("Currency") String currency
    ) {
    }

    public record MerchantDetails(
            @JsonProperty("Name") String name,
            @JsonProperty("CategoryCode") String categoryCode,
            @JsonProperty("GeoLocation") GeoLocationData geoLocation
    ) {
    }

    public record GeoLocationData(
            @JsonProperty("Latitude") double latitude,
            @JsonProperty("Longitude") double longitude
    ) {
    }

    public record FxDetailsData(
            @JsonProperty("ExchangeRate") String exchangeRate,
            @JsonProperty("OriginalCurrency") String originalCurrency,
            @JsonProperty("TargetCurrency") String targetCurrency
    ) {
    }

    public record Links(
            @JsonProperty("Self") String self,
            @JsonInclude(JsonInclude.Include.NON_NULL)
            @JsonProperty("Next") String next
    ) {
    }

    public record Meta(
            @JsonProperty("Page") int page,
            @JsonProperty("PageSize") int pageSize,
            @JsonProperty("TotalPages") int totalPages,
            @JsonProperty("TotalRecords") long totalRecords
    ) {
    }
}
