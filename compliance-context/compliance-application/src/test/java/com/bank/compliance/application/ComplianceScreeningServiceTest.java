package com.bank.compliance.application;

import com.bank.compliance.domain.ComplianceDecision;
import com.bank.compliance.domain.ComplianceResult;
import com.bank.compliance.domain.command.ComplianceScreeningCommand;
import com.bank.compliance.domain.port.out.ComplianceResultRepository;
import com.bank.compliance.domain.service.ComplianceRuleService;
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
class ComplianceScreeningServiceTest {

    @Mock
    private ComplianceRuleService ruleService;

    @Mock
    private ComplianceResultRepository repository;

    @InjectMocks
    private ComplianceScreeningService service;

    @Test
    void shouldReturnExistingResultWhenAlreadyScreened() {
        ComplianceScreeningCommand command = new ComplianceScreeningCommand("TX-1", "C1", new BigDecimal("100"), false, true, false);
        ComplianceResult existing = ComplianceResult.create("TX-1", "C1", ComplianceDecision.PASS, List.of("COMPLIANT"));

        when(repository.findByTransactionId("TX-1")).thenReturn(Optional.of(existing));

        ComplianceResult result = service.screen(command);

        assertThat(result).isEqualTo(existing);
        verify(ruleService, never()).screen(any());
        verify(repository, never()).save(any());
    }

    @Test
    void shouldEvaluateAndPersistWhenNoExistingResult() {
        ComplianceScreeningCommand command = new ComplianceScreeningCommand("TX-2", "C1", new BigDecimal("11000"), false, true, true);
        ComplianceResult evaluated = ComplianceResult.create("TX-2", "C1", ComplianceDecision.REVIEW, List.of("PEP_HIGH_VALUE_REVIEW"));

        when(repository.findByTransactionId("TX-2")).thenReturn(Optional.empty());
        when(ruleService.screen(command)).thenReturn(evaluated);
        when(repository.save(evaluated)).thenReturn(evaluated);

        ComplianceResult result = service.screen(command);

        assertThat(result).isEqualTo(evaluated);
        verify(ruleService).screen(command);
        verify(repository).save(evaluated);
    }

    @Test
    void findByTransactionIdShouldDelegate() {
        when(repository.findByTransactionId("TX-3")).thenReturn(Optional.empty());

        assertThat(service.findByTransactionId("TX-3")).isEmpty();
        verify(repository).findByTransactionId("TX-3");
    }
}
