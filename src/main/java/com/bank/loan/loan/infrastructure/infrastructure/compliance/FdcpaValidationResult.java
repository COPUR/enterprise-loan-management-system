package com.bank.loanmanagement.loan.infrastructure.compliance;

import java.util.List;

public class FdcpaValidationResult {
    public boolean isCompliant() { return true; }
    public List<String> getViolations() { return List.of(); }
    public List<String> getWarnings() { return List.of(); }
}