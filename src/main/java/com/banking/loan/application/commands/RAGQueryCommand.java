package com.banking.loan.application.commands;

import java.util.List;

public record RAGQueryCommand(
    String query,
    String context,
    List<String> documentIds,
    String queryType,
    Integer maxResults
) {}