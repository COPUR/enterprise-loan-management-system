package com.enterprise.openfinance.dynamiconboarding.domain.query;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@Tag("unit")
class GetOnboardingAccountQueryTest {

    @Test
    void shouldCreateAndNormalizeQuery() {
        GetOnboardingAccountQuery query = new GetOnboardingAccountQuery(" ACC-001 ", " TPP-001 ", " ix-dynamic-onboarding-1 ");

        assertThat(query.accountId()).isEqualTo("ACC-001");
        assertThat(query.tppId()).isEqualTo("TPP-001");
        assertThat(query.interactionId()).isEqualTo("ix-dynamic-onboarding-1");
    }

    @Test
    void shouldRejectInvalidQuery() {
        assertThatThrownBy(() -> new GetOnboardingAccountQuery(" ", "TPP-001", "ix"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("accountId");
        assertThatThrownBy(() -> new GetOnboardingAccountQuery("ACC-001", " ", "ix"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("tppId");
        assertThatThrownBy(() -> new GetOnboardingAccountQuery("ACC-001", "TPP-001", " "))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("interactionId");
    }
}
