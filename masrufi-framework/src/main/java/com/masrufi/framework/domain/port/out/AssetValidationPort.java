package com.masrufi.framework.domain.port.out;

import com.masrufi.framework.domain.model.Money;

/**
 * Port for asset validation
 * 
 * @author MasruFi Development Team
 * @version 1.0.0
 */
public interface AssetValidationPort {

    /**
     * Check if an asset is permissible under Sharia law
     */
    boolean isAssetPermissible(String assetDescription);

    /**
     * Validate asset value
     */
    boolean validateAssetValue(Money assetValue);
}