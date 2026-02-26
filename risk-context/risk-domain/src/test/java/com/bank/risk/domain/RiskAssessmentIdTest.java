package com.bank.risk.domain;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RiskAssessmentIdTest {

    @Test
    void shouldGenerateAndCompareByValue() {
        RiskAssessmentId generated = RiskAssessmentId.generate();
        RiskAssessmentId same = RiskAssessmentId.of(generated.getValue());
        RiskAssessmentId different = RiskAssessmentId.of("RISK-fixed");

        assertThat(generated.getValue()).startsWith("RISK-");
        assertThat(generated).isEqualTo(same);
        assertThat(generated.hashCode()).isEqualTo(same.hashCode());
        assertThat(generated).isNotEqualTo(different);
        assertThat(generated).isNotEqualTo(null);
        assertThat(generated).isNotEqualTo("other");
    }

    @Test
    void shouldRejectBlankValue() {
        assertThatThrownBy(() -> RiskAssessmentId.of(" "))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("cannot be blank");
    }
}
