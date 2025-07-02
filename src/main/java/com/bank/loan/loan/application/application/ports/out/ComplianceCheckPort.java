package com.bank.loanmanagement.loan.application.ports.out;

import com.bank.loanmanagement.loan.domain.loan.ComplianceCheck;
import com.bank.loanmanagement.loan.application.results.ComplianceCheckResult;
import java.util.Map;

/**
 * Outbound Port for Compliance Checking (Hexagonal Architecture)
 * External compliance service integration
 */
public interface ComplianceCheckPort {
    
    /**
     * Perform compliance check for loan application
     */
    ComplianceCheck performComplianceCheck(
        String loanId,
        String customerId,
        Map<String, Object> loanData
    );
    
    /**
     * Perform final compliance check for loan approval
     */
    ComplianceCheckResult performFinalCheck(String loanId, String correlationId);
    
    /**
     * Check specific regulation compliance
     */
    boolean isCompliantWith(String regulation, Map<String, Object> data);
    
    /**
     * Get compliance status
     */
    ComplianceCheck getComplianceStatus(String checkId);
}