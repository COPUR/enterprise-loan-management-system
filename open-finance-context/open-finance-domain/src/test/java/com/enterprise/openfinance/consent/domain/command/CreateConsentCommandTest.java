package com.enterprise.openfinance.consent.domain.command;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CreateConsentCommandTest {

    private static final Instant EXPIRES_AT = Instant.parse("2026-12-31T00:00:00Z");

    @Test
    void shouldCreateCommandWhenAllFieldsAreValid() {
        assertThatCode(() -> new CreateConsentCommand(
                "CUST-1",
                "TPP-1",
                Set.of("ReadAccounts"),
                "Personal financial management",
                EXPIRES_AT
        )).doesNotThrowAnyException();
    }

    @Test
    void shouldRejectBlankCustomerId() {
        assertThatThrownBy(() -> new CreateConsentCommand(
                " ",
                "TPP-1",
                Set.of("ReadAccounts"),
                "PFM",
                EXPIRES_AT
        )).isInstanceOf(IllegalArgumentException.class)
                .hasMessage("customerId is required");
    }

    @Test
    void shouldRejectBlankParticipantId() {
        assertThatThrownBy(() -> new CreateConsentCommand(
                "CUST-1",
                "\n",
                Set.of("ReadAccounts"),
                "PFM",
                EXPIRES_AT
        )).isInstanceOf(IllegalArgumentException.class)
                .hasMessage("participantId is required");
    }

    @Test
    void shouldRejectEmptyScopes() {
        assertThatThrownBy(() -> new CreateConsentCommand(
                "CUST-1",
                "TPP-1",
                Set.of(),
                "PFM",
                EXPIRES_AT
        )).isInstanceOf(IllegalArgumentException.class)
                .hasMessage("at least one scope is required");
    }

    @Test
    void shouldRejectBlankPurpose() {
        assertThatThrownBy(() -> new CreateConsentCommand(
                "CUST-1",
                "TPP-1",
                Set.of("ReadAccounts"),
                " ",
                EXPIRES_AT
        )).isInstanceOf(IllegalArgumentException.class)
                .hasMessage("purpose is required");
    }

    @Test
    void shouldRejectNullExpiry() {
        assertThatThrownBy(() -> new CreateConsentCommand(
                "CUST-1",
                "TPP-1",
                Set.of("ReadAccounts"),
                "PFM",
                null
        )).isInstanceOf(IllegalArgumentException.class)
                .hasMessage("expiresAt is required");
    }
}
