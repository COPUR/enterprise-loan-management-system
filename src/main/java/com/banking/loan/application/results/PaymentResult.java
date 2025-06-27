package com.banking.loan.application.results;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record PaymentResult(
    String paymentId,
    String loanId,
    BigDecimal amount,
    String status,
    LocalDateTime processedAt,
    String transactionReference,
    String message
) {}