package com.amanahfi.platform.regulatory.infrastructure.dto.hsa;

import lombok.Builder;
import lombok.Data;

import java.util.Map;

/**
 * DTO for HSA fatwa validation request
 */
@Data
@Builder
public class HsaFatwaValidationRequest {
    private String requestId;
    private String productType;
    private Map<String, Object> productDetails;
    private String urgency;
}