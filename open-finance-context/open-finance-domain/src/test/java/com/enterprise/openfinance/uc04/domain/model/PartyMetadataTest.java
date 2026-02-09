package com.enterprise.openfinance.uc04.domain.model;

import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PartyMetadataTest {

    @Test
    void shouldTrimFieldsAndKeepRelationshipDate() {
        PartyMetadata metadata = new PartyMetadata(
                " ACC-001 ",
                " Al Tareq Trading LLC ",
                " verified ",
                Instant.parse("2018-05-01T00:00:00Z")
        );

        assertThat(metadata.accountId()).isEqualTo("ACC-001");
        assertThat(metadata.fullLegalName()).isEqualTo("Al Tareq Trading LLC");
        assertThat(metadata.kycStatus()).isEqualTo("verified");
        assertThat(metadata.relationshipStartDate()).isEqualTo(Instant.parse("2018-05-01T00:00:00Z"));
    }

    @Test
    void shouldRejectInvalidConstruction() {
        assertThatThrownBy(() -> new PartyMetadata("", "Legal", "verified", Instant.now()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("accountId");

        assertThatThrownBy(() -> new PartyMetadata("ACC-001", "", "verified", Instant.now()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("fullLegalName");

        assertThatThrownBy(() -> new PartyMetadata("ACC-001", "Legal", "", Instant.now()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("kycStatus");

        assertThatThrownBy(() -> new PartyMetadata("ACC-001", "Legal", "verified", null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("relationshipStartDate");
    }
}
