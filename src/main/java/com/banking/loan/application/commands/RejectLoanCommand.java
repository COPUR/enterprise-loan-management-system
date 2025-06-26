package com.banking.loan.application.commands;

public record RejectLoanCommand(
    String loanId,
    String rejecterId,
    String rejectionReason,
    String additionalNotes
) {}