package com.enterprise.openfinance.uc05.domain.model;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CorporateAccountSnapshotTest {

    @Test
    void shouldCreateMasterAndVirtualAccounts() {
        CorporateAccountSnapshot master = new CorporateAccountSnapshot(
                "ACC-M-001",
                "CORP-001",
                null,
                "AE210001000000123456789",
                "AED",
                "Enabled",
                "Current",
                false
        );

        CorporateAccountSnapshot virtual = new CorporateAccountSnapshot(
                "ACC-V-101",
                "CORP-001",
                "ACC-M-001",
                "AE430001000000000000999",
                "AED",
                "Enabled",
                "Virtual",
                true
        );

        assertThat(master.virtual()).isFalse();
        assertThat(master.masterAccountId()).isNull();
        assertThat(master.maskedIban()).contains("****");
        assertThat(virtual.virtual()).isTrue();
        assertThat(virtual.masterAccountId()).isEqualTo("ACC-M-001");
    }

    @Test
    void shouldRejectInvalidAccountShape() {
        assertThatThrownBy(() -> new CorporateAccountSnapshot(
                "ACC-V-101",
                "CORP-001",
                null,
                "AE430001000000000000999",
                "AED",
                "Enabled",
                "Virtual",
                true
        )).isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("masterAccountId");

        assertThatThrownBy(() -> new CorporateAccountSnapshot(
                "",
                "CORP-001",
                null,
                "AE210001000000123456789",
                "AED",
                "Enabled",
                "Current",
                false
        )).isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("accountId");
    }
}
