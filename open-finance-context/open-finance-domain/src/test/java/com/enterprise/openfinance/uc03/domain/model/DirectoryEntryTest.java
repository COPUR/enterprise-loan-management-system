package com.enterprise.openfinance.uc03.domain.model;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DirectoryEntryTest {

    @Test
    void shouldNormalizeDirectoryEntryFields() {
        DirectoryEntry entry = new DirectoryEntry(" iban ", " GB82 WEST 1234 ", "  Al Tareq Trading LLC  ", AccountStatus.ACTIVE);

        assertThat(entry.schemeName()).isEqualTo("IBAN");
        assertThat(entry.identification()).isEqualTo("GB82WEST1234");
        assertThat(entry.legalName()).isEqualTo("Al Tareq Trading LLC");
        assertThat(entry.accountStatus()).isEqualTo(AccountStatus.ACTIVE);
    }

    @Test
    void shouldRejectMissingMandatoryFields() {
        assertThatThrownBy(() -> new DirectoryEntry(" ", "ID", "Name", AccountStatus.ACTIVE))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("schemeName is required");

        assertThatThrownBy(() -> new DirectoryEntry("IBAN", " ", "Name", AccountStatus.ACTIVE))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("identification is required");

        assertThatThrownBy(() -> new DirectoryEntry("IBAN", "ID", " ", AccountStatus.ACTIVE))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("legalName is required");

        assertThatThrownBy(() -> new DirectoryEntry("IBAN", "ID", "Name", null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("accountStatus is required");
    }
}
