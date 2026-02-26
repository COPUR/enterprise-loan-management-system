package com.enterprise.openfinance.payeeverification.domain.service;

import com.enterprise.openfinance.payeeverification.domain.model.NameMatchDecision;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@Tag("unit")
class ConfirmationDecisionPolicyTest {

    @Test
    void shouldMapScoresToExpectedDecisions() {
        ConfirmationDecisionPolicy policy = new ConfirmationDecisionPolicy(85);

        assertThat(policy.decide(100)).isEqualTo(NameMatchDecision.MATCH);
        assertThat(policy.decide(92)).isEqualTo(NameMatchDecision.CLOSE_MATCH);
        assertThat(policy.decide(85)).isEqualTo(NameMatchDecision.CLOSE_MATCH);
        assertThat(policy.decide(84)).isEqualTo(NameMatchDecision.NO_MATCH);
    }

    @Test
    void shouldRejectOutOfRangeScores() {
        ConfirmationDecisionPolicy policy = new ConfirmationDecisionPolicy(85);

        assertThatThrownBy(() -> policy.decide(-1))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("score must be between 0 and 100");

        assertThatThrownBy(() -> policy.decide(101))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("score must be between 0 and 100");
    }
}
