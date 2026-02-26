package com.enterprise.openfinance.paymentinitiation.domain.port.out;

import com.enterprise.openfinance.paymentinitiation.domain.model.PaymentInitiation;
import com.enterprise.openfinance.paymentinitiation.domain.model.RiskAssessmentDecision;

public interface RiskAssessmentPort {
    RiskAssessmentDecision assess(PaymentInitiation initiation, String tppId);
}
