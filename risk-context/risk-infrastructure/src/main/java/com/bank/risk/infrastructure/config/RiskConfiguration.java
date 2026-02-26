package com.bank.risk.infrastructure.config;

import com.bank.risk.application.RiskAssessmentService;
import com.bank.risk.domain.port.out.RiskAssessmentRepository;
import com.bank.risk.domain.service.RiskPolicyService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RiskConfiguration {

    @Bean
    RiskPolicyService riskPolicyService() {
        return new RiskPolicyService();
    }

    @Bean
    RiskAssessmentService riskAssessmentService(
            RiskPolicyService riskPolicyService,
            RiskAssessmentRepository riskAssessmentRepository
    ) {
        return new RiskAssessmentService(riskPolicyService, riskAssessmentRepository);
    }
}
