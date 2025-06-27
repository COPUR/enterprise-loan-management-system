package com.banking.loan.application.commands;

import java.util.List;

/**
 * Command for rejecting loan applications
 * Following DDD command pattern with all required fields
 */
public record RejectLoanCommand(
    String loanId,
    String rejecterId,
    String rejectionReason,
    String additionalNotes,
    List<String> rejectionReasons,
    String rejectedBy,
    String correlationId
) {}