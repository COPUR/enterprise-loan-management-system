package com.bank.risk.infrastructure.persistence;

import com.bank.risk.domain.RiskAssessment;
import com.bank.risk.domain.RiskDecision;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class InMemoryRiskAssessmentRepositoryTest {

    private final InMemoryRiskAssessmentRepository repository = new InMemoryRiskAssessmentRepository();

    @Test
    void shouldSaveAndFindByTransactionId() {
        RiskAssessment assessment = RiskAssessment.create("TX-1", new BigDecimal("10"), "AED", 20, RiskDecision.ALLOW, List.of());

        repository.save(assessment);

        assertThat(repository.findByTransactionId("TX-1"))
                .isPresent()
                .get()
                .isEqualTo(assessment);
        assertThat(repository.findByTransactionId("UNKNOWN")).isEmpty();
    }
}
