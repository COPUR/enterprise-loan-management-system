package com.enterprise.openfinance.consentauthorization.domain.model;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ConsentStatusTest {

    @Test
    void shouldAllowDataAccessOnlyForAuthorizedStatus() {
        assertThat(ConsentStatus.PENDING.allowsDataAccess()).isFalse();
        assertThat(ConsentStatus.AUTHORIZED.allowsDataAccess()).isTrue();
        assertThat(ConsentStatus.REVOKED.allowsDataAccess()).isFalse();
        assertThat(ConsentStatus.EXPIRED.allowsDataAccess()).isFalse();
    }
}
