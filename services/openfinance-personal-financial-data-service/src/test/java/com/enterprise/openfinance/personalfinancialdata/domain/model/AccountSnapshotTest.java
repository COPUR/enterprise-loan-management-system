package com.enterprise.openfinance.personalfinancialdata.domain.model;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AccountSnapshotTest {

    @Test
    void shouldMaskIbanForDataMinimization() {
        AccountSnapshot snapshot = new AccountSnapshot(
                "ACC-001",
                "PSU-001",
                "AE210001000000123456789",
                "AED",
                "Current",
                "Enabled",
                "IBAN",
                "Primary Account"
        );

        assertThat(snapshot.maskedIban())
                .startsWith("AE21")
                .endsWith("6789")
                .contains("*");
    }

    @Test
    void shouldReturnFallbackMaskForShortIban() {
        AccountSnapshot snapshot = new AccountSnapshot(
                "ACC-001",
                "PSU-001",
                "AE12",
                "AED",
                "Current",
                "Enabled",
                "IBAN",
                "Primary Account"
        );

        assertThat(snapshot.maskedIban()).isEqualTo("****");
    }

    @Test
    void shouldRejectInvalidAccountData() {
        assertThatThrownBy(() -> new AccountSnapshot(
                "",
                "PSU-001",
                "AE210001000000123456789",
                "AED",
                "Current",
                "Enabled",
                "IBAN",
                "Primary Account"
        )).isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("accountId");

        assertThatThrownBy(() -> new AccountSnapshot(
                "ACC-001",
                "",
                "AE210001000000123456789",
                "AED",
                "Current",
                "Enabled",
                "IBAN",
                "Primary Account"
        )).isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("psuId");

        assertThatThrownBy(() -> new AccountSnapshot(
                "ACC-001",
                "PSU-001",
                "",
                "AED",
                "Current",
                "Enabled",
                "IBAN",
                "Primary Account"
        )).isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("iban");

        assertThatThrownBy(() -> new AccountSnapshot(
                "ACC-001",
                "PSU-001",
                "AE210001000000123456789",
                "",
                "Current",
                "Enabled",
                "IBAN",
                "Primary Account"
        )).isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("currency");

        assertThatThrownBy(() -> new AccountSnapshot(
                "ACC-001",
                "PSU-001",
                "AE210001000000123456789",
                "AED",
                "",
                "Enabled",
                "IBAN",
                "Primary Account"
        )).isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("accountType");

        assertThatThrownBy(() -> new AccountSnapshot(
                "ACC-001",
                "PSU-001",
                "AE210001000000123456789",
                "AED",
                "Current",
                "",
                "IBAN",
                "Primary Account"
        )).isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("status");

        assertThatThrownBy(() -> new AccountSnapshot(
                "ACC-001",
                "PSU-001",
                "AE210001000000123456789",
                "AED",
                "Current",
                "Enabled",
                "",
                "Primary Account"
        )).isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("schemeName");
    }
}
