package com.enterprise.openfinance.uc06.domain.model;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class PaymentStatusTest {

    @Test
    void shouldExposeApiValues() {
        assertThat(PaymentStatus.PENDING.apiValue()).isEqualTo("Pending");
        assertThat(PaymentStatus.ACCEPTED_SETTLEMENT_IN_PROCESS.apiValue()).isEqualTo("AcceptedSettlementInProcess");
        assertThat(PaymentStatus.REJECTED.apiValue()).isEqualTo("Rejected");
    }
}
