package com.enterprise.openfinance.consentauthorization.jwtlifecycle.infrastructure.rest.dto;

import com.enterprise.openfinance.consentauthorization.jwtlifecycle.domain.model.InternalTokenIssueResult;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

class InternalTokenResponseTest {

    @Test
    void fromShouldMapTokenIssueResult() {
        Instant issuedAt = Instant.parse("2026-02-25T00:00:00Z");
        Instant expiresAt = issuedAt.plusSeconds(600);
        InternalTokenIssueResult result = new InternalTokenIssueResult(
                "jwt-token",
                "Bearer",
                600,
                "jti-1",
                issuedAt,
                expiresAt
        );

        InternalTokenResponse response = InternalTokenResponse.from(result);

        assertThat(response.token()).isEqualTo("jwt-token");
        assertThat(response.tokenType()).isEqualTo("Bearer");
        assertThat(response.expiresIn()).isEqualTo(600);
        assertThat(response.issuedAt()).isEqualTo(issuedAt);
        assertThat(response.expiresAt()).isEqualTo(expiresAt);
    }
}
