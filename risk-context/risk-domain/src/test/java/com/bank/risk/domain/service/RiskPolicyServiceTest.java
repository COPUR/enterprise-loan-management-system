package com.bank.risk.domain.service;

import com.bank.risk.domain.RiskDecision;
import com.bank.risk.domain.command.RiskEvaluationCommand;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

class RiskPolicyServiceTest {

    private final RiskPolicyService service = new RiskPolicyService();

    @Test
    void shouldAllowLowRiskTransaction() {
        var command = new RiskEvaluationCommand("TX-LOW", new BigDecimal("100"), "AED", false, 5);

        var assessment = service.evaluate(command);

        assertThat(assessment.getDecision()).isEqualTo(RiskDecision.ALLOW);
        assertThat(assessment.getScore()).isLessThan(50);
    }

    @Test
    void shouldRequireReviewForMediumRiskTransaction() {
        var command = new RiskEvaluationCommand("TX-REV", new BigDecimal("12000"), "AED", false, 70);

        var assessment = service.evaluate(command);

        assertThat(assessment.getDecision()).isEqualTo(RiskDecision.REVIEW);
        assertThat(assessment.getReasons()).contains("HIGH_AMOUNT", "HIGH_VELOCITY");
    }

    @Test
    void shouldBlockForHighRiskSignals() {
        var command = new RiskEvaluationCommand("TX-BLOCK", new BigDecimal("60000"), "AED", true, 80);

        var assessment = service.evaluate(command);

        assertThat(assessment.getDecision()).isEqualTo(RiskDecision.BLOCK);
        assertThat(assessment.getScore()).isEqualTo(100);
        assertThat(assessment.isBlocked()).isTrue();
    }
}
