package com.bank.compliance.infrastructure.config;

import com.bank.compliance.application.ComplianceScreeningService;
import com.bank.compliance.domain.port.out.ComplianceResultRepository;
import com.bank.compliance.domain.service.ComplianceRuleService;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.mock;

class ComplianceConfigurationTest {

    private final ComplianceConfiguration configuration = new ComplianceConfiguration();

    @Test
    void shouldCreateComplianceRuleServiceBean() {
        ComplianceRuleService service = configuration.complianceRuleService();

        assertNotNull(service);
    }

    @Test
    void shouldCreateComplianceScreeningServiceBean() {
        ComplianceRuleService ruleService = configuration.complianceRuleService();
        ComplianceResultRepository repository = mock(ComplianceResultRepository.class);

        ComplianceScreeningService service = configuration.complianceScreeningService(ruleService, repository);

        assertNotNull(service);
    }
}
