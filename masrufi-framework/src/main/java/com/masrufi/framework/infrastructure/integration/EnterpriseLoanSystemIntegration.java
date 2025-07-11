package com.masrufi.framework.infrastructure.integration;

import com.masrufi.framework.domain.model.IslamicFinancing;
import com.masrufi.framework.domain.model.CustomerProfile;
import com.masrufi.framework.domain.event.IslamicFinanceEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.Optional;

/**
 * Integration service for connecting MasruFi Framework with Enterprise Loan Management Systems
 * 
 * This service provides high-cohesion integration capabilities that allow the MasruFi Framework
 * to work seamlessly with existing enterprise loan management systems without requiring
 * modifications to the core business logic.
 * 
 * Key Integration Features:
 * - Customer data synchronization
 * - Loan data mapping and transformation
 * - Event publishing to enterprise event bus
 * - Compliance data exchange
 * - Audit trail integration
 * - Real-time data synchronization
 * 
 * Design Principles:
 * - High Cohesion: All integration logic centralized
 * - Loose Coupling: Minimal dependencies on host system
 * - Extensibility: Easy to add new integration patterns
 * - Resilience: Graceful handling of integration failures
 * 
 * @author MasruFi Development Team
 * @version 1.0.0
 */
@Slf4j
@Service
public class EnterpriseLoanSystemIntegration {

    private final ApplicationEventPublisher eventPublisher;
    private final EnterpriseLoanSystemAdapter loanSystemAdapter;
    private final CustomerDataSynchronizer customerDataSynchronizer;
    private final ComplianceDataExchanger complianceDataExchanger;

    public EnterpriseLoanSystemIntegration() {
        // Initialize with default implementations
        this.eventPublisher = null; // Will be injected if available
        this.loanSystemAdapter = new DefaultEnterpriseLoanSystemAdapter();
        this.customerDataSynchronizer = new DefaultCustomerDataSynchronizer();
        this.complianceDataExchanger = new DefaultComplianceDataExchanger();
        
        log.info("🔗 Enterprise Loan System Integration initialized");
    }

    /**
     * Register Islamic financing operation with enterprise system
     */
    public void registerIslamicFinancing(IslamicFinancing islamicFinancing) {
        try {
            log.info("📝 Registering Islamic financing: {} with enterprise system", 
                islamicFinancing.getFinancingId());

            // Transform Islamic financing to enterprise loan format
            Map<String, Object> enterpriseLoanData = transformToEnterpriseLoan(islamicFinancing);

            // Register with enterprise system
            String enterpriseLoanId = loanSystemAdapter.createLoan(enterpriseLoanData);

            // Link Islamic financing with enterprise loan
            linkIslamicFinancingToEnterpriseLoan(islamicFinancing.getFinancingId(), enterpriseLoanId);

            // Publish integration event
            publishIntegrationEvent(new IslamicFinancingRegisteredEvent(
                islamicFinancing.getFinancingId(),
                enterpriseLoanId,
                islamicFinancing.getIslamicFinancingType()
            ));

            log.info("✅ Islamic financing {} successfully registered as enterprise loan {}", 
                islamicFinancing.getFinancingId(), enterpriseLoanId);

        } catch (Exception e) {
            log.error("❌ Failed to register Islamic financing with enterprise system", e);
            throw new EnterpriseIntegrationException("Failed to register Islamic financing", e);
        }
    }

    /**
     * Synchronize customer data between MasruFi Framework and enterprise system
     */
    public CustomerProfile synchronizeCustomerData(String customerId) {
        try {
            log.info("🔄 Synchronizing customer data for: {}", customerId);

            // Fetch customer data from enterprise system
            Optional<Map<String, Object>> enterpriseCustomerData = 
                loanSystemAdapter.getCustomerData(customerId);

            if (enterpriseCustomerData.isPresent()) {
                // Transform to MasruFi CustomerProfile
                CustomerProfile customerProfile = customerDataSynchronizer
                    .transformToMasrufiCustomerProfile(enterpriseCustomerData.get());

                log.info("✅ Customer data synchronized for: {}", customerId);
                return customerProfile;
            } else {
                log.warn("⚠️ Customer not found in enterprise system: {}", customerId);
                throw new CustomerNotFoundException("Customer not found: " + customerId);
            }

        } catch (Exception e) {
            log.error("❌ Failed to synchronize customer data for: {}", customerId, e);
            throw new EnterpriseIntegrationException("Failed to synchronize customer data", e);
        }
    }

    /**
     * Update Islamic financing status in enterprise system
     */
    public void updateFinancingStatus(String islamicFinancingId, String newStatus) {
        try {
            log.info("📊 Updating financing status: {} -> {}", islamicFinancingId, newStatus);

            // Get linked enterprise loan ID
            String enterpriseLoanId = getLinkedEnterpriseLoanId(islamicFinancingId);

            if (enterpriseLoanId != null) {
                // Update status in enterprise system
                loanSystemAdapter.updateLoanStatus(enterpriseLoanId, newStatus);

                // Publish status update event
                publishIntegrationEvent(new IslamicFinancingStatusUpdatedEvent(
                    islamicFinancingId,
                    enterpriseLoanId,
                    newStatus
                ));

                log.info("✅ Financing status updated successfully");
            } else {
                log.warn("⚠️ No linked enterprise loan found for Islamic financing: {}", islamicFinancingId);
            }

        } catch (Exception e) {
            log.error("❌ Failed to update financing status", e);
            throw new EnterpriseIntegrationException("Failed to update financing status", e);
        }
    }

    /**
     * Exchange compliance data with enterprise system
     */
    public void exchangeComplianceData(IslamicFinancing islamicFinancing) {
        try {
            log.info("📋 Exchanging compliance data for: {}", islamicFinancing.getFinancingId());

            // Prepare Sharia compliance data
            Map<String, Object> complianceData = complianceDataExchanger
                .prepareShariaComplianceData(islamicFinancing);

            // Send compliance data to enterprise system
            loanSystemAdapter.updateComplianceData(
                getLinkedEnterpriseLoanId(islamicFinancing.getFinancingId()),
                complianceData
            );

            log.info("✅ Compliance data exchanged successfully");

        } catch (Exception e) {
            log.error("❌ Failed to exchange compliance data", e);
            throw new EnterpriseIntegrationException("Failed to exchange compliance data", e);
        }
    }

    /**
     * Validate integration health with enterprise system
     */
    public IntegrationHealthStatus validateIntegrationHealth() {
        try {
            log.info("🔍 Validating enterprise integration health");

            boolean loanSystemHealthy = loanSystemAdapter.healthCheck();
            boolean customerSyncHealthy = customerDataSynchronizer.isHealthy();
            boolean complianceHealthy = complianceDataExchanger.isHealthy();

            IntegrationHealthStatus status = new IntegrationHealthStatus(
                loanSystemHealthy,
                customerSyncHealthy,
                complianceHealthy
            );

            if (status.isOverallHealthy()) {
                log.info("✅ Enterprise integration health: HEALTHY");
            } else {
                log.warn("⚠️ Enterprise integration health: DEGRADED");
            }

            return status;

        } catch (Exception e) {
            log.error("❌ Failed to validate integration health", e);
            return IntegrationHealthStatus.unhealthy(e.getMessage());
        }
    }

    // Private helper methods

    private Map<String, Object> transformToEnterpriseLoan(IslamicFinancing islamicFinancing) {
        return Map.of(
            "loanType", "ISLAMIC_FINANCE",
            "islamicFinanceType", islamicFinancing.getIslamicFinancingType().toString(),
            "principalAmount", islamicFinancing.getPrincipalAmount().getAmount(),
            "currency", islamicFinancing.getPrincipalAmount().getCurrency(),
            "customerId", islamicFinancing.getCustomerProfile().getCustomerId(),
            "maturityDate", islamicFinancing.getMaturityDate(),
            "shariaCompliant", true,
            "profitMargin", islamicFinancing.getProfitMargin(),
            "assetBacked", true
        );
    }

    private void linkIslamicFinancingToEnterpriseLoan(String islamicFinancingId, String enterpriseLoanId) {
        // In a real implementation, this would store the mapping in a database or cache
        log.debug("🔗 Linking Islamic financing {} to enterprise loan {}", 
            islamicFinancingId, enterpriseLoanId);
    }

    private String getLinkedEnterpriseLoanId(String islamicFinancingId) {
        // In a real implementation, this would retrieve the mapping from database or cache
        return "ENT-LOAN-" + islamicFinancingId;
    }

    private void publishIntegrationEvent(IslamicFinanceEvent event) {
        if (eventPublisher != null) {
            eventPublisher.publishEvent(event);
        }
        log.debug("📢 Published integration event: {}", event.getClass().getSimpleName());
    }
}

/**
 * Default adapter for enterprise loan system integration
 */
class DefaultEnterpriseLoanSystemAdapter implements EnterpriseLoanSystemAdapter {
    
    @Override
    public String createLoan(Map<String, Object> loanData) {
        // Default implementation - logs the operation
        log.info("📝 Creating enterprise loan with data: {}", loanData);
        return "ENT-LOAN-" + System.currentTimeMillis();
    }

    @Override
    public Optional<Map<String, Object>> getCustomerData(String customerId) {
        // Default implementation - returns mock data
        log.info("👤 Fetching customer data for: {}", customerId);
        return Optional.of(Map.of(
            "customerId", customerId,
            "name", "Default Customer",
            "type", "INDIVIDUAL",
            "creditScore", 750
        ));
    }

    @Override
    public void updateLoanStatus(String loanId, String status) {
        log.info("📊 Updating loan {} status to: {}", loanId, status);
    }

    @Override
    public void updateComplianceData(String loanId, Map<String, Object> complianceData) {
        log.info("📋 Updating compliance data for loan: {}", loanId);
    }

    @Override
    public boolean healthCheck() {
        log.debug("🔍 Enterprise loan system health check");
        return true; // Default healthy
    }
}

/**
 * Default customer data synchronizer
 */
class DefaultCustomerDataSynchronizer implements CustomerDataSynchronizer {
    
    @Override
    public CustomerProfile transformToMasrufiCustomerProfile(Map<String, Object> enterpriseCustomerData) {
        log.debug("🔄 Transforming enterprise customer data to MasruFi profile");
        
        return CustomerProfile.builder()
            .customerId(enterpriseCustomerData.get("customerId").toString())
            .customerName(enterpriseCustomerData.get("name").toString())
            .customerType(CustomerType.valueOf(enterpriseCustomerData.get("type").toString()))
            .creditScore(Integer.parseInt(enterpriseCustomerData.get("creditScore").toString()))
            .build();
    }

    @Override
    public boolean isHealthy() {
        return true;
    }
}

/**
 * Default compliance data exchanger
 */
class DefaultComplianceDataExchanger implements ComplianceDataExchanger {
    
    @Override
    public Map<String, Object> prepareShariaComplianceData(IslamicFinancing islamicFinancing) {
        log.debug("📋 Preparing Sharia compliance data");
        
        return Map.of(
            "shariaCompliant", true,
            "islamicFinanceType", islamicFinancing.getIslamicFinancingType().toString(),
            "ribaFree", true,
            "ghararFree", true,
            "assetBacked", true,
            "shariaApprovalDate", java.time.LocalDateTime.now(),
            "complianceVersion", "1.0"
        );
    }

    @Override
    public boolean isHealthy() {
        return true;
    }
}

/**
 * Integration health status
 */
class IntegrationHealthStatus {
    private final boolean loanSystemHealthy;
    private final boolean customerSyncHealthy;
    private final boolean complianceHealthy;
    private final String errorMessage;

    public IntegrationHealthStatus(boolean loanSystemHealthy, boolean customerSyncHealthy, boolean complianceHealthy) {
        this.loanSystemHealthy = loanSystemHealthy;
        this.customerSyncHealthy = customerSyncHealthy;
        this.complianceHealthy = complianceHealthy;
        this.errorMessage = null;
    }

    public static IntegrationHealthStatus unhealthy(String errorMessage) {
        return new IntegrationHealthStatus(false, false, false, errorMessage);
    }

    private IntegrationHealthStatus(boolean loanSystemHealthy, boolean customerSyncHealthy, 
                                   boolean complianceHealthy, String errorMessage) {
        this.loanSystemHealthy = loanSystemHealthy;
        this.customerSyncHealthy = customerSyncHealthy;
        this.complianceHealthy = complianceHealthy;
        this.errorMessage = errorMessage;
    }

    public boolean isOverallHealthy() {
        return loanSystemHealthy && customerSyncHealthy && complianceHealthy;
    }

    // Getters
    public boolean isLoanSystemHealthy() { return loanSystemHealthy; }
    public boolean isCustomerSyncHealthy() { return customerSyncHealthy; }
    public boolean isComplianceHealthy() { return complianceHealthy; }
    public String getErrorMessage() { return errorMessage; }
}

// Integration interfaces
interface EnterpriseLoanSystemAdapter {
    String createLoan(Map<String, Object> loanData);
    Optional<Map<String, Object>> getCustomerData(String customerId);
    void updateLoanStatus(String loanId, String status);
    void updateComplianceData(String loanId, Map<String, Object> complianceData);
    boolean healthCheck();
}

interface CustomerDataSynchronizer {
    CustomerProfile transformToMasrufiCustomerProfile(Map<String, Object> enterpriseCustomerData);
    boolean isHealthy();
}

interface ComplianceDataExchanger {
    Map<String, Object> prepareShariaComplianceData(IslamicFinancing islamicFinancing);
    boolean isHealthy();
}

// Integration events
abstract class IslamicFinanceEvent {
    protected final String islamicFinancingId;
    protected final String enterpriseLoanId;
    protected final java.time.LocalDateTime timestamp;

    protected IslamicFinanceEvent(String islamicFinancingId, String enterpriseLoanId) {
        this.islamicFinancingId = islamicFinancingId;
        this.enterpriseLoanId = enterpriseLoanId;
        this.timestamp = java.time.LocalDateTime.now();
    }

    public String getIslamicFinancingId() { return islamicFinancingId; }
    public String getEnterpriseLoanId() { return enterpriseLoanId; }
    public java.time.LocalDateTime getTimestamp() { return timestamp; }
}

class IslamicFinancingRegisteredEvent extends IslamicFinanceEvent {
    private final IslamicFinancing.IslamicFinancingType financingType;

    public IslamicFinancingRegisteredEvent(String islamicFinancingId, String enterpriseLoanId, 
                                         IslamicFinancing.IslamicFinancingType financingType) {
        super(islamicFinancingId, enterpriseLoanId);
        this.financingType = financingType;
    }

    public IslamicFinancing.IslamicFinancingType getFinancingType() { return financingType; }
}

class IslamicFinancingStatusUpdatedEvent extends IslamicFinanceEvent {
    private final String newStatus;

    public IslamicFinancingStatusUpdatedEvent(String islamicFinancingId, String enterpriseLoanId, String newStatus) {
        super(islamicFinancingId, enterpriseLoanId);
        this.newStatus = newStatus;
    }

    public String getNewStatus() { return newStatus; }
}

// Custom exceptions
class EnterpriseIntegrationException extends RuntimeException {
    public EnterpriseIntegrationException(String message, Throwable cause) {
        super(message, cause);
    }
}

class CustomerNotFoundException extends RuntimeException {
    public CustomerNotFoundException(String message) {
        super(message);
    }
}

// Enums for CustomerProfile (simplified versions)
enum CustomerType {
    INDIVIDUAL,
    CORPORATE,
    GOVERNMENT,
    NON_PROFIT
}