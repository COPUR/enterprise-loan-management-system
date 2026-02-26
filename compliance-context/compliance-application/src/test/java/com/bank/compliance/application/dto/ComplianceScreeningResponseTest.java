package com.bank.compliance.application.dto;

import com.bank.compliance.domain.ComplianceDecision;
import com.bank.compliance.domain.ComplianceResult;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ComplianceScreeningResponseTest {

    @Test
    void shouldMapFromDomain() {
        ComplianceResult result = ComplianceResult.create("TX-1", "C-1", ComplianceDecision.REVIEW, List.of("PEP_HIGH_VALUE_REVIEW"));

        ComplianceScreeningResponse response = ComplianceScreeningResponse.from(result);

        assertThat(response.screeningId()).isEqualTo(result.getId().getValue());
        assertThat(response.transactionId()).isEqualTo("TX-1");
        assertThat(response.decision()).isEqualTo("REVIEW");
    }
}
