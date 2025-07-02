package com.bank.loanmanagement.loan.application.results;

import java.time.LocalDateTime;
import java.util.List;

public record RAGQueryResult(
    String queryId,
    String query,
    String answer,
    List<String> sourceDocuments,
    Double confidence,
    LocalDateTime processedAt,
    String model
) {}