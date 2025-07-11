package com.masrufi.framework;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.NestedConfigurationProperty;

import java.util.List;
import java.util.Map;

/**
 * Configuration properties for MasruFi Framework
 * 
 * This class defines all configurable aspects of the MasruFi Framework
 * allowing fine-grained control over Islamic finance capabilities,
 * integration settings, and operational parameters.
 * 
 * Configuration Example:
 * ```yaml
 * masrufi:
 *   framework:
 *     enabled: true
 *     islamic-finance:
 *       enabled: true
 *       supported-models:
 *         - MURABAHA
 *         - MUSHARAKAH
 *         - IJARAH
 *         - SALAM
 *         - ISTISNA
 *         - QARD_HASSAN
 *     uae-cryptocurrency:
 *       enabled: true
 *       supported-currencies:
 *         - UAE-CBDC
 *         - ADIB-DD
 *         - ENBD-DC
 *     sharia-compliance:
 *       enabled: true
 *       strict-mode: true
 * ```
 */
@Data
@ConfigurationProperties(prefix = "masrufi.framework")
public class MasrufiFrameworkProperties {

    /**
     * Enable/disable the entire MasruFi Framework
     */
    private boolean enabled = false;

    /**
     * Framework version information
     */
    private String version = "1.0.0";

    /**
     * Integration mode with host enterprise system
     */
    private IntegrationMode integrationMode = IntegrationMode.EXTENSION;

    /**
     * Islamic Finance module configuration
     */
    @NestedConfigurationProperty
    private IslamicFinanceProperties islamicFinance = new IslamicFinanceProperties();

    /**
     * UAE Cryptocurrency integration configuration
     */
    @NestedConfigurationProperty
    private UAECryptocurrencyProperties uaeCryptocurrency = new UAECryptocurrencyProperties();

    /**
     * Sharia Compliance configuration
     */
    @NestedConfigurationProperty
    private ShariaComplianceProperties shariaCompliance = new ShariaComplianceProperties();

    /**
     * Monitoring and observability configuration
     */
    @NestedConfigurationProperty
    private MonitoringProperties monitoring = new MonitoringProperties();

    /**
     * Enterprise system integration configuration
     */
    @NestedConfigurationProperty
    private EnterpriseIntegrationProperties enterpriseIntegration = new EnterpriseIntegrationProperties();

    /**
     * Islamic Finance specific properties
     */
    @Data
    public static class IslamicFinanceProperties {
        /**
         * Enable Islamic Finance module
         */
        private boolean enabled = true;

        /**
         * Supported Islamic finance models
         */
        private List<IslamicFinanceModel> supportedModels = List.of(
            IslamicFinanceModel.MURABAHA,
            IslamicFinanceModel.MUSHARAKAH,
            IslamicFinanceModel.IJARAH,
            IslamicFinanceModel.SALAM,
            IslamicFinanceModel.ISTISNA,
            IslamicFinanceModel.QARD_HASSAN
        );

        /**
         * Default currency for Islamic finance operations
         */
        private String defaultCurrency = "AED";

        /**
         * Supported jurisdictions for Islamic finance
         */
        private List<String> supportedJurisdictions = List.of(
            "UAE", "SAUDI_ARABIA", "QATAR", "KUWAIT", 
            "BAHRAIN", "OMAN", "TURKEY", "PAKISTAN"
        );

        /**
         * Business rule engine configuration
         */
        private BusinessRuleEngineProperties businessRules = new BusinessRuleEngineProperties();
    }

    /**
     * UAE Cryptocurrency integration properties
     */
    @Data
    public static class UAECryptocurrencyProperties {
        /**
         * Enable UAE cryptocurrency integration
         */
        private boolean enabled = true;

        /**
         * Supported UAE digital currencies
         */
        private List<String> supportedCurrencies = List.of(
            "UAE-CBDC",    // UAE Central Bank Digital Currency
            "ADIB-DD",     // Abu Dhabi Islamic Bank Digital Dirham
            "ENBD-DC",     // Emirates NBD Digital Currency
            "FAB-DT",      // First Abu Dhabi Bank Digital Token
            "CBD-DD",      // Commercial Bank of Dubai Digital Dirham
            "RAK-DC",      // RAK Bank Digital Currency
            "MASHREQ-DC"   // Mashreq Bank Digital Currency
        );

        /**
         * Blockchain network configuration
         */
        private String networkType = "UAE_GOVERNMENT_BLOCKCHAIN";
        
        /**
         * Smart contract deployment settings
         */
        private SmartContractProperties smartContract = new SmartContractProperties();
    }

    /**
     * Sharia Compliance properties
     */
    @Data
    public static class ShariaComplianceProperties {
        /**
         * Enable Sharia compliance validation
         */
        private boolean enabled = true;

        /**
         * Strict compliance mode (rejects non-compliant operations)
         */
        private boolean strictMode = true;

        /**
         * Sharia board configuration
         */
        private String shariaBoard = "UAE_HIGHER_SHARIA_AUTHORITY";

        /**
         * Compliance validation rules
         */
        private ComplianceRulesProperties rules = new ComplianceRulesProperties();
    }

    /**
     * Monitoring and observability properties
     */
    @Data
    public static class MonitoringProperties {
        /**
         * Enable monitoring capabilities
         */
        private boolean enabled = true;

        /**
         * Enable metrics collection
         */
        private boolean metricsEnabled = true;

        /**
         * Enable health checks
         */
        private boolean healthChecksEnabled = true;

        /**
         * Enable audit logging
         */
        private boolean auditLoggingEnabled = true;
    }

    /**
     * Enterprise system integration properties
     */
    @Data
    public static class EnterpriseIntegrationProperties {
        /**
         * Host system API base URL
         */
        private String hostSystemBaseUrl = "http://localhost:8080";

        /**
         * Integration authentication method
         */
        private AuthenticationMethod authenticationMethod = AuthenticationMethod.JWT;

        /**
         * Event publishing configuration
         */
        private EventPublishingProperties eventPublishing = new EventPublishingProperties();

        /**
         * Data synchronization settings
         */
        private DataSyncProperties dataSync = new DataSyncProperties();
    }

    /**
     * Business Rule Engine properties
     */
    @Data
    public static class BusinessRuleEngineProperties {
        /**
         * Enable business rule engine
         */
        private boolean enabled = true;

        /**
         * Hot reload of business rules
         */
        private boolean hotReloadEnabled = true;

        /**
         * Rule validation on startup
         */
        private boolean validateOnStartup = true;
    }

    /**
     * Smart Contract properties
     */
    @Data
    public static class SmartContractProperties {
        /**
         * Enable smart contract deployment
         */
        private boolean enabled = true;

        /**
         * Gas limit for smart contract operations
         */
        private long gasLimit = 500000L;

        /**
         * Gas price in wei
         */
        private long gasPrice = 20000000000L; // 20 Gwei
    }

    /**
     * Compliance Rules properties
     */
    @Data
    public static class ComplianceRulesProperties {
        /**
         * Validate against Riba (interest) prohibition
         */
        private boolean validateRiba = true;

        /**
         * Validate against Gharar (uncertainty) prohibition
         */
        private boolean validateGharar = true;

        /**
         * Validate asset backing requirements
         */
        private boolean validateAssetBacking = true;

        /**
         * Maximum profit margin allowed (as percentage)
         */
        private double maxProfitMargin = 30.0;
    }

    /**
     * Event Publishing properties
     */
    @Data
    public static class EventPublishingProperties {
        /**
         * Enable event publishing to host system
         */
        private boolean enabled = true;

        /**
         * Event topic prefix
         */
        private String topicPrefix = "masrufi.events";

        /**
         * Batch size for event publishing
         */
        private int batchSize = 100;
    }

    /**
     * Data Synchronization properties
     */
    @Data
    public static class DataSyncProperties {
        /**
         * Enable data synchronization with host system
         */
        private boolean enabled = true;

        /**
         * Sync interval in seconds
         */
        private int syncIntervalSeconds = 300; // 5 minutes

        /**
         * Conflict resolution strategy
         */
        private ConflictResolutionStrategy conflictResolution = ConflictResolutionStrategy.HOST_WINS;
    }

    /**
     * Supported Islamic Finance Models
     */
    public enum IslamicFinanceModel {
        MURABAHA,      // Cost-plus financing
        MUSHARAKAH,    // Partnership financing
        IJARAH,        // Lease financing
        SALAM,         // Forward sale financing
        ISTISNA,       // Manufacturing financing
        QARD_HASSAN    // Benevolent loan
    }

    /**
     * Integration modes with host enterprise system
     */
    public enum IntegrationMode {
        EXTENSION,     // Extends existing system capabilities
        REPLACEMENT,   // Replaces specific modules
        STANDALONE     // Operates independently
    }

    /**
     * Authentication methods for enterprise integration
     */
    public enum AuthenticationMethod {
        JWT,           // JSON Web Token
        OAUTH2,        // OAuth 2.0
        API_KEY,       // API Key based
        MUTUAL_TLS     // Mutual TLS
    }

    /**
     * Conflict resolution strategies for data synchronization
     */
    public enum ConflictResolutionStrategy {
        HOST_WINS,     // Host system data takes precedence
        MASRUFI_WINS,  // MasruFi framework data takes precedence
        TIMESTAMP,     // Latest timestamp wins
        MANUAL         // Manual conflict resolution required
    }
}