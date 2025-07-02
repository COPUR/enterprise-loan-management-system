package com.bank.loanmanagement.loan.application.commands;

import java.time.LocalDateTime;
import java.util.Map;

public record GenerateReportCommand(
    String reportType,
    LocalDateTime fromDate,
    LocalDateTime toDate,
    Map<String, Object> filters,
    String format,
    String requestedBy
) {}