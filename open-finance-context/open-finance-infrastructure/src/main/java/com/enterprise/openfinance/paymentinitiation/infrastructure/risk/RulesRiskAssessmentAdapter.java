package com.enterprise.openfinance.paymentinitiation.infrastructure.risk;

import com.enterprise.openfinance.paymentinitiation.domain.model.PaymentInitiation;
import com.enterprise.openfinance.paymentinitiation.domain.model.RiskAssessmentDecision;
import com.enterprise.openfinance.paymentinitiation.domain.port.out.RiskAssessmentPort;
import org.springframework.stereotype.Component;

import java.util.Locale;

@Component
public class RulesRiskAssessmentAdapter implements RiskAssessmentPort {

    @Override
    public RiskAssessmentDecision assess(PaymentInitiation initiation, String tppId) {
        String normalizedName = initiation.creditorName().toUpperCase(Locale.ROOT);
        if (normalizedName.contains("TEST_SANCTION_LIST")) {
            return RiskAssessmentDecision.REJECT;
        }
        return RiskAssessmentDecision.PASS;
    }
}
