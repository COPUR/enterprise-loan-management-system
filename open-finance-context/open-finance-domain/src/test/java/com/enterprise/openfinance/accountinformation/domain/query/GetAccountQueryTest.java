package com.enterprise.openfinance.accountinformation.domain.query;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class GetAccountQueryTest {

    @Test
    void shouldCreateQuery() {
        GetAccountQuery query = new GetAccountQuery("CONS-1", "TPP-1", "ACC-1", "ix-1");

        assertThat(query.accountId()).isEqualTo("ACC-1");
    }

    @Test
    void shouldRejectInvalidQuery() {
        assertThatThrownBy(() -> new GetAccountQuery("CONS-1", "TPP-1", "", "ix-1"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("accountId");

        assertThatThrownBy(() -> new GetAccountQuery("", "TPP-1", "ACC-1", "ix-1"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("consentId");

        assertThatThrownBy(() -> new GetAccountQuery("CONS-1", "", "ACC-1", "ix-1"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("tppId");

        assertThatThrownBy(() -> new GetAccountQuery("CONS-1", "TPP-1", "ACC-1", ""))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("interactionId");
    }
}
