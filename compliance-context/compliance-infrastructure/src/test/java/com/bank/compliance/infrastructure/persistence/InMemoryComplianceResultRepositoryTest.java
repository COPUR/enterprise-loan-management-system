package com.bank.compliance.infrastructure.persistence;

import com.bank.compliance.domain.ComplianceDecision;
import com.bank.compliance.domain.ComplianceResult;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class InMemoryComplianceResultRepositoryTest {

    private final InMemoryComplianceResultRepository repository = new InMemoryComplianceResultRepository();

    @Test
    void shouldSaveAndRetrieveByTransactionId() {
        ComplianceResult result = ComplianceResult.create("TX-1", "C1", ComplianceDecision.PASS, List.of("COMPLIANT"));

        repository.save(result);

        assertThat(repository.findByTransactionId("TX-1")).isPresent().get().isEqualTo(result);
        assertThat(repository.findByTransactionId("UNKNOWN")).isEmpty();
    }
}
