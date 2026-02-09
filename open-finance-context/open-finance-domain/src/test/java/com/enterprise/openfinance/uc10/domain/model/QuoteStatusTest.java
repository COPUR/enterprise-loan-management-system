package com.enterprise.openfinance.uc10.domain.model;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@Tag("unit")
class QuoteStatusTest {

    @Test
    void shouldExposeApiValuesAndTerminalFlags() {
        assertThat(QuoteStatus.QUOTED.apiValue()).isEqualTo("Quoted");
        assertThat(QuoteStatus.ACCEPTED.apiValue()).isEqualTo("Accepted");
        assertThat(QuoteStatus.EXPIRED.apiValue()).isEqualTo("Expired");

        assertThat(QuoteStatus.QUOTED.isTerminal()).isFalse();
        assertThat(QuoteStatus.ACCEPTED.isTerminal()).isTrue();
        assertThat(QuoteStatus.EXPIRED.isTerminal()).isTrue();
    }
}
