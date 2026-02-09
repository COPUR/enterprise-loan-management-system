package com.enterprise.openfinance.uc03.domain.model;

import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ConfirmationAuditRecordTest {

    @Test
    void shouldCreateAuditRecordWhenValid() {
        Instant now = Instant.parse("2026-02-09T10:15:30Z");
        ConfirmationAuditRecord record = new ConfirmationAuditRecord(
                "TPP-1",
                "ix-1",
                "IBAN",
                "GB82WEST12345698765432",
                "Al Tareq Trading LLC",
                AccountStatus.ACTIVE,
                NameMatchDecision.MATCH,
                100,
                false,
                now
        );

        assertThat(record.tppId()).isEqualTo("TPP-1");
        assertThat(record.interactionId()).isEqualTo("ix-1");
        assertThat(record.accountStatus()).isEqualTo(AccountStatus.ACTIVE);
        assertThat(record.nameMatched()).isEqualTo(NameMatchDecision.MATCH);
        assertThat(record.matchScore()).isEqualTo(100);
        assertThat(record.timestamp()).isEqualTo(now);
    }

    @Test
    void shouldRejectInvalidAuditRecordFields() {
        Instant now = Instant.parse("2026-02-09T10:15:30Z");

        assertThatThrownBy(() -> new ConfirmationAuditRecord(
                " ",
                "ix-1",
                "IBAN",
                "ID",
                "Name",
                AccountStatus.ACTIVE,
                NameMatchDecision.MATCH,
                100,
                false,
                now
        )).isInstanceOf(IllegalArgumentException.class).hasMessage("tppId is required");

        assertThatThrownBy(() -> new ConfirmationAuditRecord(
                "TPP-1",
                " ",
                "IBAN",
                "ID",
                "Name",
                AccountStatus.ACTIVE,
                NameMatchDecision.MATCH,
                100,
                false,
                now
        )).isInstanceOf(IllegalArgumentException.class).hasMessage("interactionId is required");

        assertThatThrownBy(() -> new ConfirmationAuditRecord(
                "TPP-1",
                "ix-1",
                " ",
                "ID",
                "Name",
                AccountStatus.ACTIVE,
                NameMatchDecision.MATCH,
                100,
                false,
                now
        )).isInstanceOf(IllegalArgumentException.class).hasMessage("schemeName is required");

        assertThatThrownBy(() -> new ConfirmationAuditRecord(
                "TPP-1",
                "ix-1",
                "IBAN",
                " ",
                "Name",
                AccountStatus.ACTIVE,
                NameMatchDecision.MATCH,
                100,
                false,
                now
        )).isInstanceOf(IllegalArgumentException.class).hasMessage("identification is required");

        assertThatThrownBy(() -> new ConfirmationAuditRecord(
                "TPP-1",
                "ix-1",
                "IBAN",
                "ID",
                " ",
                AccountStatus.ACTIVE,
                NameMatchDecision.MATCH,
                100,
                false,
                now
        )).isInstanceOf(IllegalArgumentException.class).hasMessage("requestedName is required");

        assertThatThrownBy(() -> new ConfirmationAuditRecord(
                "TPP-1",
                "ix-1",
                "IBAN",
                "ID",
                "Name",
                null,
                NameMatchDecision.MATCH,
                100,
                false,
                now
        )).isInstanceOf(IllegalArgumentException.class).hasMessage("accountStatus is required");

        assertThatThrownBy(() -> new ConfirmationAuditRecord(
                "TPP-1",
                "ix-1",
                "IBAN",
                "ID",
                "Name",
                AccountStatus.ACTIVE,
                null,
                100,
                false,
                now
        )).isInstanceOf(IllegalArgumentException.class).hasMessage("nameMatched is required");

        assertThatThrownBy(() -> new ConfirmationAuditRecord(
                "TPP-1",
                "ix-1",
                "IBAN",
                "ID",
                "Name",
                AccountStatus.ACTIVE,
                NameMatchDecision.MATCH,
                100,
                false,
                null
        )).isInstanceOf(IllegalArgumentException.class).hasMessage("timestamp is required");
    }

    @Test
    void shouldRejectOutOfRangeMatchScore() {
        Instant now = Instant.parse("2026-02-09T10:15:30Z");

        assertThatThrownBy(() -> new ConfirmationAuditRecord(
                "TPP-1",
                "ix-1",
                "IBAN",
                "ID",
                "Name",
                AccountStatus.ACTIVE,
                NameMatchDecision.MATCH,
                -1,
                false,
                now
        )).isInstanceOf(IllegalArgumentException.class).hasMessage("matchScore must be between 0 and 100");

        assertThatThrownBy(() -> new ConfirmationAuditRecord(
                "TPP-1",
                "ix-1",
                "IBAN",
                "ID",
                "Name",
                AccountStatus.ACTIVE,
                NameMatchDecision.MATCH,
                101,
                false,
                now
        )).isInstanceOf(IllegalArgumentException.class).hasMessage("matchScore must be between 0 and 100");
    }
}
