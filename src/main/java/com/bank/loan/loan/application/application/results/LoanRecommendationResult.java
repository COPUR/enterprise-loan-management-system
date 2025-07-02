package com.bank.loanmanagement.loan.application.results;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record LoanRecommendationResult(
    String customerId,
    List<LoanRecommendation> recommendations,
    Double confidenceScore,
    LocalDateTime generatedAt,
    String modelUsed
) {
    public record LoanRecommendation(
        String loanType,
        BigDecimal recommendedAmount,
        BigDecimal interestRate,
        Integer termInMonths,
        String reason,
        Double confidence
    ) {}
}