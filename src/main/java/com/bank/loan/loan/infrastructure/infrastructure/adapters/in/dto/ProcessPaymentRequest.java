package com.bank.loanmanagement.loan.infrastructure.adapters.in.dto;

import java.math.BigDecimal;

public record ProcessPaymentRequest(
    BigDecimal amount,
    String paymentMethod,
    String paymentReference,
    String notes,
    String paymentChannel
) {}