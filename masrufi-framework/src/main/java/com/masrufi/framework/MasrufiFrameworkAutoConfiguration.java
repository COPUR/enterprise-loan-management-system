package com.masrufi.framework;

import com.masrufi.framework.config.IslamicFinanceConfiguration;
import com.masrufi.framework.config.ShariaComplianceConfiguration;
import com.masrufi.framework.config.UAECryptocurrencyConfiguration;
import com.masrufi.framework.domain.service.*;
import com.masrufi.framework.infrastructure.integration.EnterpriseLoanSystemIntegration;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Import;

/**
 * Auto-configuration for MasruFi Framework Islamic Finance Module
 * 
 * This auto-configuration enables Islamic finance capabilities as an extension
 * to existing enterprise loan management systems with high cohesion and 
 * minimal coupling to the host system.
 * 
 * Features Enabled:
 * - Islamic Finance Models (Murabaha, Musharakah, Ijarah, Salam, Istisna, Qard Hassan)
 * - Sharia Compliance Validation
 * - UAE Cryptocurrency Integration
 * - Business Rule Engine for Islamic Finance
 * - Event-Driven Architecture Support
 * - RESTful API Endpoints for Islamic Finance
 * 
 * Activation:
 * Add to application.properties/yml:
 * masrufi.framework.enabled=true
 * masrufi.framework.islamic-finance.enabled=true
 * 
 * Integration:
 * The framework integrates with existing loan management systems through
 * well-defined interfaces without modifying core business logic.
 * 
 * @author MasruFi Development Team
 * @version 1.0.0
 * @since 2024
 */
@Slf4j
@AutoConfiguration
@ConditionalOnProperty(
    prefix = "masrufi.framework", 
    name = "enabled", 
    havingValue = "true",
    matchIfMissing = false
)
@EnableConfigurationProperties({
    MasrufiFrameworkProperties.class
})
@Import({
    IslamicFinanceConfiguration.class,
    ShariaComplianceConfiguration.class,
    UAECryptocurrencyConfiguration.class
})
@ComponentScan(basePackages = {
    "com.masrufi.framework.domain",
    "com.masrufi.framework.infrastructure",
    "com.masrufi.framework.api",
    "com.masrufi.framework.application"
})
public class MasrufiFrameworkAutoConfiguration {

    /**
     * Main entry point for MasruFi Framework integration
     */
    @Bean
    @ConditionalOnProperty(
        prefix = "masrufi.framework.islamic-finance",
        name = "enabled",
        havingValue = "true",
        matchIfMissing = true
    )
    public MasrufiFrameworkFacade masrufiFrameworkFacade(
            MurabahaService murabahaService,
            MusharakahService musharakahService,
            IjarahService ijarahService,
            SalamService salamService,
            IstisnaService istisnaService,
            QardHassanService qardHassanService,
            ShariaComplianceService shariaComplianceService,
            EnterpriseLoanSystemIntegration enterpriseIntegration) {
        
        log.info("🕌 Initializing MasruFi Framework Islamic Finance Module");
        log.info("📋 Islamic Finance Models Available:");
        log.info("   • Murabaha (Cost-plus financing)");
        log.info("   • Musharakah (Partnership financing)");
        log.info("   • Ijarah (Lease financing)");
        log.info("   • Salam (Forward sale financing)");
        log.info("   • Istisna (Manufacturing financing)");
        log.info("   • Qard Hassan (Benevolent loan)");
        
        return new MasrufiFrameworkFacade(
            murabahaService,
            musharakahService,
            ijarahService,
            salamService,
            istisnaService,
            qardHassanService,
            shariaComplianceService,
            enterpriseIntegration
        );
    }

    /**
     * Enterprise system integration service
     */
    @Bean
    public EnterpriseLoanSystemIntegration enterpriseLoanSystemIntegration() {
        log.info("🔗 Configuring Enterprise Loan System Integration");
        return new EnterpriseLoanSystemIntegration();
    }

    /**
     * Islamic Finance health indicator
     */
    @Bean
    @ConditionalOnProperty(
        prefix = "masrufi.framework.monitoring",
        name = "enabled",
        havingValue = "true",
        matchIfMissing = true
    )
    public IslamicFinanceHealthIndicator islamicFinanceHealthIndicator(
            MasrufiFrameworkFacade masrufiFramework) {
        log.info("📊 Enabling Islamic Finance Health Monitoring");
        return new IslamicFinanceHealthIndicator(masrufiFramework);
    }
    
    /**
     * Configuration validation bean
     */
    @Bean
    public MasrufiFrameworkConfigurationValidator configurationValidator(
            MasrufiFrameworkProperties properties) {
        log.info("✅ Initializing MasruFi Framework Configuration Validator");
        return new MasrufiFrameworkConfigurationValidator(properties);
    }
}

/**
 * Main facade for MasruFi Framework operations
 */
class MasrufiFrameworkFacade {
    
    private final MurabahaService murabahaService;
    private final MusharakahService musharakahService;
    private final IjarahService ijarahService;
    private final SalamService salamService;
    private final IstisnaService istisnaService;
    private final QardHassanService qardHassanService;
    private final ShariaComplianceService shariaComplianceService;
    private final EnterpriseLoanSystemIntegration enterpriseIntegration;
    
    public MasrufiFrameworkFacade(
            MurabahaService murabahaService,
            MusharakahService musharakahService,
            IjarahService ijarahService,
            SalamService salamService,
            IstisnaService istisnaService,
            QardHassanService qardHassanService,
            ShariaComplianceService shariaComplianceService,
            EnterpriseLoanSystemIntegration enterpriseIntegration) {
        this.murabahaService = murabahaService;
        this.musharakahService = musharakahService;
        this.ijarahService = ijarahService;
        this.salamService = salamService;
        this.istisnaService = istisnaService;
        this.qardHassanService = qardHassanService;
        this.shariaComplianceService = shariaComplianceService;
        this.enterpriseIntegration = enterpriseIntegration;
    }
    
    // Getter methods for service access
    public MurabahaService getMurabahaService() { return murabahaService; }
    public MusharakahService getMusharakahService() { return musharakahService; }
    public IjarahService getIjarahService() { return ijarahService; }
    public SalamService getSalamService() { return salamService; }
    public IstisnaService getIstisnaService() { return istisnaService; }
    public QardHassanService getQardHassanService() { return qardHassanService; }
    public ShariaComplianceService getShariaComplianceService() { return shariaComplianceService; }
    public EnterpriseLoanSystemIntegration getEnterpriseIntegration() { return enterpriseIntegration; }
    
    /**
     * Check if MasruFi Framework is properly initialized
     */
    public boolean isFrameworkReady() {
        return murabahaService != null && 
               musharakahService != null && 
               ijarahService != null &&
               salamService != null &&
               istisnaService != null &&
               qardHassanService != null &&
               shariaComplianceService != null &&
               enterpriseIntegration != null;
    }
    
    /**
     * Get framework version information
     */
    public String getFrameworkInfo() {
        return "MasruFi Framework v1.0.0 - Islamic Finance Extension Module";
    }
}

/**
 * Health indicator for Islamic Finance module
 */
class IslamicFinanceHealthIndicator {
    
    private final MasrufiFrameworkFacade masrufiFramework;
    
    public IslamicFinanceHealthIndicator(MasrufiFrameworkFacade masrufiFramework) {
        this.masrufiFramework = masrufiFramework;
    }
    
    public boolean isHealthy() {
        return masrufiFramework.isFrameworkReady();
    }
    
    public String getHealthStatus() {
        if (isHealthy()) {
            return "🕌 MasruFi Framework: All Islamic Finance services operational";
        } else {
            return "❌ MasruFi Framework: Some Islamic Finance services unavailable";
        }
    }
}

/**
 * Configuration validator for MasruFi Framework
 */
class MasrufiFrameworkConfigurationValidator {
    
    private final MasrufiFrameworkProperties properties;
    
    public MasrufiFrameworkConfigurationValidator(MasrufiFrameworkProperties properties) {
        this.properties = properties;
        validateConfiguration();
    }
    
    private void validateConfiguration() {
        log.info("🔍 Validating MasruFi Framework Configuration");
        
        if (!properties.isEnabled()) {
            log.warn("⚠️ MasruFi Framework is disabled");
            return;
        }
        
        if (properties.getIslamicFinance().isEnabled()) {
            log.info("✅ Islamic Finance module enabled");
        }
        
        if (properties.getUaeCryptocurrency().isEnabled()) {
            log.info("✅ UAE Cryptocurrency integration enabled");
        }
        
        if (properties.getShariaCompliance().isEnabled()) {
            log.info("✅ Sharia Compliance validation enabled");
        }
        
        log.info("✅ MasruFi Framework configuration validation complete");
    }
}