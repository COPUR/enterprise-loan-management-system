package com.enterprise.openfinance.uc11.domain.model;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@Tag("unit")
class FxDealStatusTest {

    @Test
    void shouldExposeApiValues() {
        assertThat(FxDealStatus.BOOKED.apiValue()).isEqualTo("Booked");
    }
}
