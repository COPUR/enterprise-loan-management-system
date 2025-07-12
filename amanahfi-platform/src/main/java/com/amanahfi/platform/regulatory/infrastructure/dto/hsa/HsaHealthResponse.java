package com.amanahfi.platform.regulatory.infrastructure.dto.hsa;

import lombok.Data;

/**
 * DTO for HSA health check response
 */
@Data
public class HsaHealthResponse {
    private boolean available;
    private String shariaFrameworkVersion;
    private String supportedLanguages;
}