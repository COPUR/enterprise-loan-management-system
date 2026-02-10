package com.enterprise.openfinance.bankingmetadata.infrastructure.rest.dto;

import com.enterprise.openfinance.bankingmetadata.domain.model.MetadataQueryResult;
import com.enterprise.openfinance.bankingmetadata.domain.model.StandingOrderMetadata;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.math.RoundingMode;
import java.util.List;

public record StandingOrdersMetadataResponse(
        @JsonProperty("Data") Data data,
        @JsonProperty("Links") Links links,
        @JsonProperty("Meta") Meta meta
) {

    public static StandingOrdersMetadataResponse from(MetadataQueryResult<StandingOrderMetadata> page,
                                                      String selfLink,
                                                      String nextLink) {
        List<StandingOrderData> standingOrderData = page.items().stream()
                .map(order -> new StandingOrderData(
                        order.standingOrderId(),
                        order.accountId(),
                        order.frequency(),
                        order.firstPaymentDate().toString(),
                        order.finalPaymentDate() == null ? null : order.finalPaymentDate().toString(),
                        new AmountData(order.amount().setScale(2, RoundingMode.HALF_UP).toPlainString(), order.currency())
                ))
                .toList();

        return new StandingOrdersMetadataResponse(
                new Data(standingOrderData),
                new Links(selfLink, nextLink),
                new Meta(page.page(), page.pageSize(), page.totalPages(), page.totalRecords())
        );
    }

    public record Data(
            @JsonProperty("StandingOrder") List<StandingOrderData> standingOrders
    ) {
    }

    public record StandingOrderData(
            @JsonProperty("StandingOrderId") String standingOrderId,
            @JsonProperty("AccountId") String accountId,
            @JsonProperty("Frequency") String frequency,
            @JsonProperty("FirstPaymentDate") String firstPaymentDate,
            @JsonInclude(JsonInclude.Include.NON_NULL)
            @JsonProperty("FinalPaymentDate") String finalPaymentDate,
            @JsonProperty("InstructedAmount") AmountData instructedAmount
    ) {
    }

    public record AmountData(
            @JsonProperty("Amount") String amount,
            @JsonProperty("Currency") String currency
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
