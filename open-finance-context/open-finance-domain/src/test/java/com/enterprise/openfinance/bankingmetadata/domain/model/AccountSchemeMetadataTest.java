package com.enterprise.openfinance.bankingmetadata.domain.model;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AccountSchemeMetadataTest {

    @Test
    void shouldExposeSchemeDetails() {
        AccountSchemeMetadata metadata = new AccountSchemeMetadata(" ACC-001 ", " IBAN ", " MOB-123 ");

        assertThat(metadata.accountId()).isEqualTo("ACC-001");
        assertThat(metadata.schemeName()).isEqualTo("IBAN");
        assertThat(metadata.secondaryIdentification()).isEqualTo("MOB-123");
    }

    @Test
    void shouldAllowNullSecondaryIdentification() {
        AccountSchemeMetadata metadata = new AccountSchemeMetadata("ACC-001", "IBAN", null);

        assertThat(metadata.secondaryIdentification()).isNull();
    }

    @Test
    void shouldRejectInvalidValues() {
        assertThatThrownBy(() -> new AccountSchemeMetadata("", "IBAN", null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("accountId");

        assertThatThrownBy(() -> new AccountSchemeMetadata("ACC-001", "", null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("schemeName");
    }
}
