package com.bank.risk.domain;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RiskAssessmentTest {

    @Test
    void shouldCreateAssessmentAndExposeBehavior() {
        RiskAssessment assessment = RiskAssessment.create(
                "TX-1",
                new BigDecimal("500"),
                "AED",
                55,
                RiskDecision.REVIEW,
                List.of("MEDIUM_VELOCITY")
        );

        assertThat(assessment.getId().getValue()).startsWith("RISK-");
        assertThat(assessment.getTransactionId()).isEqualTo("TX-1");
        assertThat(assessment.getScore()).isEqualTo(55);
        assertThat(assessment.requiresManualReview()).isTrue();
        assertThat(assessment.isBlocked()).isFalse();
    }

    @Test
    void shouldRejectInvalidScoreAndIdentifiers() {
        assertThatThrownBy(() -> RiskAssessment.create("", new BigDecimal("10"), "AED", 10, RiskDecision.ALLOW, List.of()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("transactionId");

        assertThatThrownBy(() -> RiskAssessment.create("TX-1", new BigDecimal("10"), "AED", 101, RiskDecision.BLOCK, List.of("x")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("score");
    }
}
