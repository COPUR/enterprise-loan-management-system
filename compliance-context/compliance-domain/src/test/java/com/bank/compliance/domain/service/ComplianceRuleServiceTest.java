package com.bank.compliance.domain.service;

import com.bank.compliance.domain.ComplianceDecision;
import com.bank.compliance.domain.command.ComplianceScreeningCommand;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

class ComplianceRuleServiceTest {

    private final ComplianceRuleService service = new ComplianceRuleService();

    @Test
    void shouldFailWhenSanctionsHit() {
        ComplianceScreeningCommand command = new ComplianceScreeningCommand("TX-1", "C1", new BigDecimal("200"), true, true, false);

        var result = service.screen(command);

        assertThat(result.getDecision()).isEqualTo(ComplianceDecision.FAIL);
        assertThat(result.getReasons()).contains("SANCTIONS_HIT");
    }

    @Test
    void shouldFailWhenKycMissing() {
        ComplianceScreeningCommand command = new ComplianceScreeningCommand("TX-2", "C1", new BigDecimal("200"), false, false, false);

        var result = service.screen(command);

        assertThat(result.getDecision()).isEqualTo(ComplianceDecision.FAIL);
        assertThat(result.getReasons()).contains("KYC_NOT_VERIFIED");
    }

    @Test
    void shouldReviewPepHighValueAndPassNormalFlow() {
        var review = service.screen(new ComplianceScreeningCommand("TX-3", "C1", new BigDecimal("12000"), false, true, true));
        var pass = service.screen(new ComplianceScreeningCommand("TX-4", "C1", new BigDecimal("900"), false, true, false));

        assertThat(review.getDecision()).isEqualTo(ComplianceDecision.REVIEW);
        assertThat(pass.getDecision()).isEqualTo(ComplianceDecision.PASS);
        assertThat(pass.getReasons()).contains("COMPLIANT");
    }
}
