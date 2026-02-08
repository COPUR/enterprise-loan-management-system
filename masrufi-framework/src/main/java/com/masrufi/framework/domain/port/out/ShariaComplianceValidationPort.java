package com.masrufi.framework.domain.port.out;

import com.masrufi.framework.domain.model.CreateMurabahaCommand;
import com.masrufi.framework.domain.model.ShariaComplianceResult;

/**
 * Port for Sharia compliance validation
 * 
 * @author MasruFi Development Team
 * @version 1.0.0
 */
public interface ShariaComplianceValidationPort {

    /**
     * Validate Murabaha compliance with Sharia principles
     */
    ShariaComplianceResult validateMurabahaCompliance(CreateMurabahaCommand command);
}