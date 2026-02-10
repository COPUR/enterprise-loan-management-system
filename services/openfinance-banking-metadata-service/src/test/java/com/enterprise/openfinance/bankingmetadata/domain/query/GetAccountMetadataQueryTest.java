package com.enterprise.openfinance.bankingmetadata.domain.query;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

class GetAccountMetadataQueryTest {

    @Test
    void shouldRejectBlankFields() {
        assertThatThrownBy(() -> new GetAccountMetadataQuery("", "TPP-001", "ACC-001", "ix-1"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("consentId");

        assertThatThrownBy(() -> new GetAccountMetadataQuery("CONS", "", "ACC-001", "ix-1"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("tppId");

        assertThatThrownBy(() -> new GetAccountMetadataQuery("CONS", "TPP-001", "", "ix-1"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("accountId");

        assertThatThrownBy(() -> new GetAccountMetadataQuery("CONS", "TPP-001", "ACC-001", ""))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("interactionId");
    }
}
