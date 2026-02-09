package com.enterprise.openfinance.uc05.domain.query;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ListCorporateAccountsQueryTest {

    @Test
    void shouldResolveDefaults() {
        ListCorporateAccountsQuery query = new ListCorporateAccountsQuery(
                "CONS-TRSY-001",
                "TPP-001",
                "ix-1",
                null,
                ""
        );

        assertThat(query.resolveIncludeVirtual()).isFalse();
        assertThat(query.masterAccountId()).isNull();
    }

    @Test
    void shouldRejectBlankFields() {
        assertThatThrownBy(() -> new ListCorporateAccountsQuery("", "TPP-001", "ix-1", true, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("consentId");

        assertThatThrownBy(() -> new ListCorporateAccountsQuery("CONS", "", "ix-1", true, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("tppId");

        assertThatThrownBy(() -> new ListCorporateAccountsQuery("CONS", "TPP-001", "", true, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("interactionId");
    }
}
