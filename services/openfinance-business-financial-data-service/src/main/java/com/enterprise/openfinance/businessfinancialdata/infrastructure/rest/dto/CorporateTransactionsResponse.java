package com.enterprise.openfinance.businessfinancialdata.infrastructure.rest.dto;

import com.enterprise.openfinance.businessfinancialdata.domain.model.CorporatePagedResult;
import com.enterprise.openfinance.businessfinancialdata.domain.model.CorporateTransactionSnapshot;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public record CorporateTransactionsResponse(
        @JsonProperty("Data") Data data,
        @JsonProperty("Links") Links links,
        @JsonProperty("Meta") Meta meta
) {

    public static CorporateTransactionsResponse from(CorporatePagedResult<CorporateTransactionSnapshot> page,
                                                     String selfLink,
                                                     String nextLink) {
        List<TransactionData> transactions = page.items().stream()
                .map(CorporateTransactionsResponse::toTransactionData)
                .toList();

        return new CorporateTransactionsResponse(
                new Data(transactions),
                new Links(selfLink, nextLink),
                new Meta(page.page(), page.pageSize(), page.totalPages(), page.totalRecords())
        );
    }

    private static TransactionData toTransactionData(CorporateTransactionSnapshot tx) {
        return new TransactionData(
                tx.transactionId(),
                tx.accountId(),
                new AmountData(tx.amount().setScale(2, java.math.RoundingMode.HALF_UP).toPlainString(), tx.currency()),
                tx.bookingDateTime().toString(),
                tx.transactionCode(),
                tx.proprietaryCode(),
                tx.description()
        );
    }

    public record Data(
            @JsonProperty("Transaction") List<TransactionData> transactions
    ) {
    }

    public record TransactionData(
            @JsonProperty("TransactionId") String transactionId,
            @JsonProperty("AccountId") String accountId,
            @JsonProperty("Amount") AmountData amount,
            @JsonProperty("BookingDateTime") String bookingDateTime,
            @JsonProperty("TransactionCode") String transactionCode,
            @JsonInclude(JsonInclude.Include.NON_NULL)
            @JsonProperty("ProprietaryCode") String proprietaryCode,
            @JsonInclude(JsonInclude.Include.NON_NULL)
            @JsonProperty("Description") String description
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
