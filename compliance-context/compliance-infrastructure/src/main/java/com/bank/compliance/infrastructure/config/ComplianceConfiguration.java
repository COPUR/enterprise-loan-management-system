package com.bank.compliance.infrastructure.config;

import com.bank.compliance.application.ComplianceScreeningService;
import com.bank.compliance.domain.port.out.ComplianceResultRepository;
import com.bank.compliance.domain.service.ComplianceRuleService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ComplianceConfiguration {

    @Bean
    ComplianceRuleService complianceRuleService() {
        return new ComplianceRuleService();
    }

    @Bean
    ComplianceScreeningService complianceScreeningService(
            ComplianceRuleService complianceRuleService,
            ComplianceResultRepository complianceResultRepository
    ) {
        return new ComplianceScreeningService(complianceRuleService, complianceResultRepository);
    }
}
