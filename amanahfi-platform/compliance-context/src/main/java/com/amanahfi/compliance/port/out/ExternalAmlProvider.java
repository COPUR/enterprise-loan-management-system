package com.amanahfi.compliance.port.out;

import com.amanahfi.compliance.domain.check.AmlScreeningResult;
import com.amanahfi.compliance.domain.check.CheckType;

/**
 * Port for external AML screening providers
 * Integrates with third-party compliance services
 */
public interface ExternalAmlProvider {

    /**
     * Performs AML screening for given entity
     */
    AmlScreeningResult performScreening(String entityId, CheckType checkType);

    /**
     * Performs enhanced due diligence screening
     */
    AmlScreeningResult performEnhancedDueDiligence(String entityId, String additionalContext);

    /**
     * Checks if provider is available
     */
    boolean isAvailable();

    /**
     * Gets provider name/identifier
     */
    String getProviderName();
}