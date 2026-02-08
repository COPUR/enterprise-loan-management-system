package com.masrufi.framework.domain.port.out;

import com.masrufi.framework.domain.model.CustomerProfile;

/**
 * Port for customer validation
 * 
 * @author MasruFi Development Team
 * @version 1.0.0
 */
public interface CustomerValidationPort {

    /**
     * Check if customer is eligible for Islamic financing
     */
    boolean isEligibleForIslamicFinancing(CustomerProfile customerProfile);
}