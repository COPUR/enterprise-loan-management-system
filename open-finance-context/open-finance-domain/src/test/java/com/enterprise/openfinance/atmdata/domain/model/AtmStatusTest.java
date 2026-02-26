package com.enterprise.openfinance.atmdata.domain.model;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class AtmStatusTest {

    @Test
    void shouldExposeApiValues() {
        assertThat(AtmStatus.IN_SERVICE.apiValue()).isEqualTo("InService");
        assertThat(AtmStatus.OUT_OF_SERVICE.apiValue()).isEqualTo("OutOfService");
    }
}
