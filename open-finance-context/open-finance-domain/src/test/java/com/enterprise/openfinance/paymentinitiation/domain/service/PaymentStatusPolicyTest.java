package com.enterprise.openfinance.paymentinitiation.domain.service;

import com.enterprise.openfinance.paymentinitiation.domain.model.PaymentStatus;
import com.enterprise.openfinance.paymentinitiation.domain.model.RiskAssessmentDecision;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

class PaymentStatusPolicyTest {

    private final PaymentStatusPolicy policy = new PaymentStatusPolicy();

    @Test
    void shouldReturnRejectedWhenRiskDecisionIsReject() {
        PaymentStatus status = policy.decide(
                LocalDate.parse("2026-02-09"),
                null,
                RiskAssessmentDecision.REJECT
        );

        assertThat(status).isEqualTo(PaymentStatus.REJECTED);
    }

    @Test
    void shouldReturnPendingForFutureDatedPaymentWhenRiskPasses() {
        PaymentStatus status = policy.decide(
                LocalDate.parse("2026-02-09"),
                LocalDate.parse("2026-02-10"),
                RiskAssessmentDecision.PASS
        );

        assertThat(status).isEqualTo(PaymentStatus.PENDING);
    }

    @Test
    void shouldReturnAcceptedSettlementInProcessForImmediatePayment() {
        PaymentStatus status = policy.decide(
                LocalDate.parse("2026-02-09"),
                LocalDate.parse("2026-02-09"),
                RiskAssessmentDecision.PASS
        );

        assertThat(status).isEqualTo(PaymentStatus.ACCEPTED_SETTLEMENT_IN_PROCESS);
    }
}
