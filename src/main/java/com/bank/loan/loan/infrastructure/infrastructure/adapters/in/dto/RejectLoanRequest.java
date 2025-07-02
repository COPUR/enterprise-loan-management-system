package com.bank.loanmanagement.loan.infrastructure.adapters.in.dto;

import java.util.List;

public record RejectLoanRequest(
    String rejecterId,
    String rejectionReason,
    String additionalNotes,
    List<String> rejectionReasons,
    String rejectionNotes
) {}