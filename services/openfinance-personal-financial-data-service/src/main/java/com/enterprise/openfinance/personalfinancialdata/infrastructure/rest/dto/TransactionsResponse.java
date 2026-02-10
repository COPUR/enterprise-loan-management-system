package com.enterprise.openfinance.personalfinancialdata.infrastructure.rest.dto;

import com.enterprise.openfinance.personalfinancialdata.domain.model.PagedResult;
import com.enterprise.openfinance.personalfinancialdata.domain.model.TransactionSnapshot;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.math.RoundingMode;
import java.util.List;

public record TransactionsResponse(
        @JsonProperty("Data") Data data,
        @JsonProperty("Links") Links links,
        @JsonProperty("Meta") Meta meta
) {

    public static TransactionsResponse from(PagedResult<TransactionSnapshot> page,
                                            String selfLink,
                                            String nextLink) {
        List<TransactionData> txData = page.items().stream()
                .map(tx -> new TransactionData(
                        tx.transactionId(),
                        new AmountData(tx.amount().setScale(2, RoundingMode.HALF_UP).toPlainString(), tx.currency()),
                        tx.creditDebitIndicator(),
                        tx.status(),
                        tx.bookingDateTime().toString(),
                        tx.valueDateTime().toString(),
                        tx.merchantName()
                ))
                .toList();

        return new TransactionsResponse(
                new Data(txData),
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
            @JsonProperty("Amount") AmountData amount,
            @JsonProperty("CreditDebitIndicator") String creditDebitIndicator,
            @JsonProperty("Status") String status,
            @JsonProperty("BookingDateTime") String bookingDateTime,
            @JsonProperty("ValueDateTime") String valueDateTime,
            @JsonProperty("MerchantName") String merchantName
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
