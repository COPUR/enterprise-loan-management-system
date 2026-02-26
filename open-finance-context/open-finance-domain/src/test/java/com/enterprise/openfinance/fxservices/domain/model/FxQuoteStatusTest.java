package com.enterprise.openfinance.fxservices.domain.model;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@Tag("unit")
class FxQuoteStatusTest {

    @Test
    void shouldExposeApiValuesAndTerminalFlags() {
        assertThat(FxQuoteStatus.QUOTED.apiValue()).isEqualTo("Quoted");
        assertThat(FxQuoteStatus.BOOKED.apiValue()).isEqualTo("Booked");
        assertThat(FxQuoteStatus.EXPIRED.apiValue()).isEqualTo("Expired");

        assertThat(FxQuoteStatus.QUOTED.isTerminal()).isFalse();
        assertThat(FxQuoteStatus.BOOKED.isTerminal()).isTrue();
        assertThat(FxQuoteStatus.EXPIRED.isTerminal()).isTrue();
    }
}
