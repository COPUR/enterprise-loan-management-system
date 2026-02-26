package com.bank.risk.application;

import com.bank.risk.domain.RiskAssessment;
import com.bank.risk.domain.RiskDecision;
import com.bank.risk.domain.command.RiskEvaluationCommand;
import com.bank.risk.domain.port.out.RiskAssessmentRepository;
import com.bank.risk.domain.service.RiskPolicyService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RiskAssessmentServiceTest {

    @Mock
    private RiskPolicyService policyService;

    @Mock
    private RiskAssessmentRepository repository;

    @InjectMocks
    private RiskAssessmentService service;

    @Test
    void shouldReturnExistingAssessmentWhenTransactionAlreadyAssessed() {
        RiskEvaluationCommand command = new RiskEvaluationCommand("TX-1", new BigDecimal("100"), "AED", false, 10);
        RiskAssessment existing = RiskAssessment.create("TX-1", new BigDecimal("100"), "AED", 10, RiskDecision.ALLOW, List.of());

        when(repository.findByTransactionId("TX-1")).thenReturn(Optional.of(existing));

        RiskAssessment result = service.assess(command);

        assertThat(result).isEqualTo(existing);
        verify(policyService, never()).evaluate(any());
        verify(repository, never()).save(any());
    }

    @Test
    void shouldEvaluateAndPersistWhenNoExistingAssessment() {
        RiskEvaluationCommand command = new RiskEvaluationCommand("TX-2", new BigDecimal("200"), "AED", true, 60);
        RiskAssessment assessed = RiskAssessment.create("TX-2", new BigDecimal("200"), "AED", 85, RiskDecision.BLOCK, List.of("HIGH_RISK_COUNTRY"));

        when(repository.findByTransactionId("TX-2")).thenReturn(Optional.empty());
        when(policyService.evaluate(command)).thenReturn(assessed);
        when(repository.save(assessed)).thenReturn(assessed);

        RiskAssessment result = service.assess(command);

        assertThat(result).isEqualTo(assessed);
        verify(policyService).evaluate(command);
        verify(repository).save(assessed);
    }

    @Test
    void findByTransactionIdShouldDelegateToRepository() {
        when(repository.findByTransactionId("TX-3")).thenReturn(Optional.empty());

        assertThat(service.findByTransactionId("TX-3")).isEmpty();
        verify(repository).findByTransactionId("TX-3");
    }
}
