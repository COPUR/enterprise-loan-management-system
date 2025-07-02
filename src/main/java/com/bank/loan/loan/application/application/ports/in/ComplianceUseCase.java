package com.bank.loanmanagement.loan.application.ports.in;

import com.bank.loanmanagement.loan.application.commands.*;
import com.bank.loanmanagement.loan.application.results.*;

/**
 * Compliance Use Case (Hexagonal Architecture - Inbound Port)
 * Defines compliance-related business operations
 */
public interface ComplianceUseCase {
    
    /**
     * Perform compliance check
     */
    ComplianceCheckResult performComplianceCheck(ComplianceCheckCommand command);
    
    /**
     * Generate regulatory report
     */
    RegulatoryReportResult generateRegulatoryReport(GenerateReportCommand command);
    
    /**
     * Audit transaction
     */
    AuditResult auditTransaction(AuditTransactionCommand command);
}