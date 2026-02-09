package com.enterprise.openfinance.uc03.domain.model;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class NameMatchDecisionTest {

    @Test
    void shouldExposeApiValues() {
        assertThat(NameMatchDecision.MATCH.apiValue()).isEqualTo("Match");
        assertThat(NameMatchDecision.CLOSE_MATCH.apiValue()).isEqualTo("CloseMatch");
        assertThat(NameMatchDecision.NO_MATCH.apiValue()).isEqualTo("NoMatch");
        assertThat(NameMatchDecision.UNABLE_TO_CHECK.apiValue()).isEqualTo("UnableToCheck");
    }
}
