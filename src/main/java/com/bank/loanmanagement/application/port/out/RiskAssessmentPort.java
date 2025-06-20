package com.bank.loanmanagement.application.port.out;

import com.bank.loanmanagement.domain.customer.CustomerId;
import com.bank.loanmanagement.domain.loan.CustomerFinancialProfile;
import com.bank.loanmanagement.domain.loan.RiskAssessment;

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