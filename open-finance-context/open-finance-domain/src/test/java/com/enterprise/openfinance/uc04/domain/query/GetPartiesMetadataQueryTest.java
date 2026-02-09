package com.enterprise.openfinance.uc04.domain.query;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

class GetPartiesMetadataQueryTest {

    @Test
    void shouldRejectBlankFields() {
        assertThatThrownBy(() -> new GetPartiesMetadataQuery("", "TPP-001", "ACC-001", "ix-1"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("consentId");

        assertThatThrownBy(() -> new GetPartiesMetadataQuery("CONS", "", "ACC-001", "ix-1"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("tppId");

        assertThatThrownBy(() -> new GetPartiesMetadataQuery("CONS", "TPP-001", "", "ix-1"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("accountId");

        assertThatThrownBy(() -> new GetPartiesMetadataQuery("CONS", "TPP-001", "ACC-001", ""))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("interactionId");
    }
}
