package com.bank.loanmanagement.loan.application.port.out;

import com.bank.loanmanagement.loan.domain.customer.CustomerId;
import com.bank.loanmanagement.loan.domain.loan.CustomerFinancialProfile;
import com.bank.loanmanagement.loan.domain.loan.RiskAssessment;

/**
 * Port for risk assessment services
 * Hexagonal architecture - outbound port
 */
public interface RiskAssessmentPort {

    /**
     * Assess customer risk for loan approval
     */
    RiskAssessment assessRisk(CustomerId customerId, CustomerFinancialProfile profile);
}