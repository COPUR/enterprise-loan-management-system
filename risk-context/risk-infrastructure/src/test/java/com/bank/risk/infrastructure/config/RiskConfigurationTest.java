package com.bank.risk.infrastructure.config;

import com.bank.risk.application.RiskAssessmentService;
import com.bank.risk.domain.port.out.RiskAssessmentRepository;
import com.bank.risk.domain.service.RiskPolicyService;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.mock;

class RiskConfigurationTest {

    private final RiskConfiguration configuration = new RiskConfiguration();

    @Test
    void shouldCreateRiskPolicyServiceBean() {
        RiskPolicyService service = configuration.riskPolicyService();

        assertNotNull(service);
    }

    @Test
    void shouldCreateRiskAssessmentServiceBean() {
        RiskPolicyService policyService = configuration.riskPolicyService();
        RiskAssessmentRepository repository = mock(RiskAssessmentRepository.class);

        RiskAssessmentService service = configuration.riskAssessmentService(policyService, repository);

        assertNotNull(service);
    }
}
