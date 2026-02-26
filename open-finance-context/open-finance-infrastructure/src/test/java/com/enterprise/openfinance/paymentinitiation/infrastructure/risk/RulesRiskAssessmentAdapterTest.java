package com.enterprise.openfinance.paymentinitiation.infrastructure.risk;

import com.enterprise.openfinance.paymentinitiation.domain.model.PaymentInitiation;
import com.enterprise.openfinance.paymentinitiation.domain.model.RiskAssessmentDecision;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

@Tag("unit")
class RulesRiskAssessmentAdapterTest {

    private final RulesRiskAssessmentAdapter adapter = new RulesRiskAssessmentAdapter();

    @Test
    void shouldReturnRejectForSanctionsName() {
        RiskAssessmentDecision decision = adapter.assess(
                initiation("TEST_SANCTION_LIST"),
                "TPP-001"
        );

        assertThat(decision).isEqualTo(RiskAssessmentDecision.REJECT);
    }

    @Test
    void shouldReturnPassForRegularName() {
        RiskAssessmentDecision decision = adapter.assess(
                initiation("Vendor LLC"),
                "TPP-001"
        );

        assertThat(decision).isEqualTo(RiskAssessmentDecision.PASS);
    }

    private static PaymentInitiation initiation(String creditorName) {
        return new PaymentInitiation(
                "INSTR-001",
                "E2E-001",
                "ACC-DEBTOR-001",
                new BigDecimal("100.00"),
                "AED",
                "IBAN",
                "AE120001000000123456789",
                creditorName,
                null
        );
    }
}
