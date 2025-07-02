package com.bank.loanmanagement.loan.application.results;

import java.util.List;
import java.util.Map;

public record ComplianceInfo(
    String jurisdiction,
    List<String> applicableRegulations,
    Boolean tilaCompliant,
    Boolean respaCompliant,
    Boolean fdcpaCompliant,
    Map<String, String> complianceNotes,
    String riskAssessment
) {}