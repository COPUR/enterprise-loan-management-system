package com.enterprise.openfinance.uc06.domain.port.out;

import com.enterprise.openfinance.uc06.domain.model.PaymentInitiation;
import com.enterprise.openfinance.uc06.domain.model.RiskAssessmentDecision;

public interface RiskAssessmentPort {
    RiskAssessmentDecision assess(PaymentInitiation initiation, String tppId);
}
