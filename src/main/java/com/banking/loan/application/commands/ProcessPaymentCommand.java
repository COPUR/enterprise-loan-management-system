package com.banking.loan.application.commands;

import java.math.BigDecimal;

public record ProcessPaymentCommand(
    String loanId,
    BigDecimal amount,
    String paymentMethod,
    String paymentReference,
    String notes,
    String customerId,
    String paymentChannel,
    String paidBy,
    String correlationId,
    boolean fraudCheckRequired
) {}