package com.bank.loanmanagement.loan.application.results;

import java.time.LocalDateTime;
import java.util.List;

public record KYCVerificationResult(
    String customerId,
    String verificationId,
    String status,
    String riskLevel,
    List<String> verifiedDocuments,
    LocalDateTime completedAt,
    String message
) {}