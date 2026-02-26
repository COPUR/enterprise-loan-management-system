package com.enterprise.openfinance.insurancequotes.infrastructure.persistence;

import com.enterprise.openfinance.insurancequotes.domain.model.MotorInsuranceQuote;
import com.enterprise.openfinance.insurancequotes.domain.model.QuoteStatus;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

@Tag("unit")
class GeneratedPolicyIssuanceAdapterTest {

    @Test
    void shouldGeneratePolicyIdentifiers() {
        GeneratedPolicyIssuanceAdapter adapter = new GeneratedPolicyIssuanceAdapter();
        var issued = adapter.issuePolicy(sampleQuote(), "PAY-1", "ix-1", Instant.parse("2026-02-09T10:00:00Z"));

        assertThat(issued.policyId()).startsWith("POL-MTR-");
        assertThat(issued.policyNumber()).contains("MTR-");
        assertThat(issued.certificateId()).startsWith("CERT-");
    }

    private static MotorInsuranceQuote sampleQuote() {
        return new MotorInsuranceQuote(
                "Q-1AAAAAA",
                "TPP-001",
                "TOYOTA",
                "CAMRY",
                2023,
                35,
                10,
                new BigDecimal("1000.00"),
                "AED",
                QuoteStatus.QUOTED,
                Instant.parse("2026-02-09T10:30:00Z"),
                "hash",
                null,
                null,
                null,
                null,
                Instant.parse("2026-02-09T10:00:00Z"),
                Instant.parse("2026-02-09T10:00:00Z")
        );
    }
}
