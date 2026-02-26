package com.enterprise.openfinance.corporatetreasury.domain.query;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

class GetCorporateBalancesQueryTest {

    @Test
    void shouldRejectBlankFields() {
        assertThatThrownBy(() -> new GetCorporateBalancesQuery("", "TPP-001", "ACC-M-001", "ix-1"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("consentId");

        assertThatThrownBy(() -> new GetCorporateBalancesQuery("CONS", "", "ACC-M-001", "ix-1"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("tppId");

        assertThatThrownBy(() -> new GetCorporateBalancesQuery("CONS", "TPP-001", "", "ix-1"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("masterAccountId");

        assertThatThrownBy(() -> new GetCorporateBalancesQuery("CONS", "TPP-001", "ACC-M-001", ""))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("interactionId");
    }
}
