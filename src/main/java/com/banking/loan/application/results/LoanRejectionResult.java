package com.banking.loan.application.results;

import java.time.LocalDateTime;

public record LoanRejectionResult(
    String loanId,
    String status,
    LocalDateTime rejectedAt,
    String rejecterId,
    String rejectionReason,
    String message
) {}