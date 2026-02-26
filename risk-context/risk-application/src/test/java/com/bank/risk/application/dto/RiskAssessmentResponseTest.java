package com.bank.risk.application.dto;

import com.bank.risk.domain.RiskAssessment;
import com.bank.risk.domain.RiskDecision;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class RiskAssessmentResponseTest {

    @Test
    void shouldMapFromDomain() {
        RiskAssessment assessment = RiskAssessment.create(
                "TX-1",
                new BigDecimal("50"),
                "AED",
                25,
                RiskDecision.ALLOW,
                List.of("COMPLIANT")
        );

        RiskAssessmentResponse response = RiskAssessmentResponse.from(assessment);

        assertThat(response.assessmentId()).isEqualTo(assessment.getId().getValue());
        assertThat(response.transactionId()).isEqualTo("TX-1");
        assertThat(response.decision()).isEqualTo("ALLOW");
    }
}
