package com.banking.loan.infrastructure.adapters.in.dto;

public record RejectLoanRequest(
    String rejecterId,
    String rejectionReason,
    String additionalNotes
) {}