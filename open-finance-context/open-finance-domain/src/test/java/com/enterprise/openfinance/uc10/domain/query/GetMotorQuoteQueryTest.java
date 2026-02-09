package com.enterprise.openfinance.uc10.domain.query;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@Tag("unit")
class GetMotorQuoteQueryTest {

    @Test
    void shouldCreateAndNormalizeQuery() {
        GetMotorQuoteQuery query = new GetMotorQuoteQuery(" Q-1 ", " TPP-001 ", " ix-1 ");

        assertThat(query.quoteId()).isEqualTo("Q-1");
        assertThat(query.tppId()).isEqualTo("TPP-001");
        assertThat(query.interactionId()).isEqualTo("ix-1");
    }

    @Test
    void shouldRejectInvalidQuery() {
        assertThatThrownBy(() -> new GetMotorQuoteQuery(" ", "TPP-001", "ix-1"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new GetMotorQuoteQuery("Q-1", " ", "ix-1"))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
