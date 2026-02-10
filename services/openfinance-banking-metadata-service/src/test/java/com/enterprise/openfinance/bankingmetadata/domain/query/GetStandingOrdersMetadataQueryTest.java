package com.enterprise.openfinance.bankingmetadata.domain.query;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class GetStandingOrdersMetadataQueryTest {

    @Test
    void shouldResolveDefaultsAndOptionalAccountId() {
        GetStandingOrdersMetadataQuery query = new GetStandingOrdersMetadataQuery(
                " CONS-META-001 ",
                " TPP-001 ",
                " ",
                " ix-1 ",
                null,
                null
        );

        assertThat(query.accountId()).isNull();
        assertThat(query.resolvePage()).isEqualTo(1);
        assertThat(query.resolvePageSize(100, 200)).isEqualTo(100);
    }

    @Test
    void shouldRejectInvalidPaging() {
        assertThatThrownBy(() -> new GetStandingOrdersMetadataQuery(
                "CONS-META-001",
                "TPP-001",
                null,
                "ix-1",
                0,
                100
        )).isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("page");

        assertThatThrownBy(() -> new GetStandingOrdersMetadataQuery(
                "CONS-META-001",
                "TPP-001",
                null,
                "ix-1",
                1,
                0
        )).isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("pageSize");
    }
}
