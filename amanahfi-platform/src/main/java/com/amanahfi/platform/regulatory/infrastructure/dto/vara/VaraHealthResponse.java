package com.amanahfi.platform.regulatory.infrastructure.dto.vara;

import lombok.Data;

/**
 * DTO for VARA health check response
 */
@Data
public class VaraHealthResponse {
    private boolean operational;
    private String apiVersion;
    private String regulatoryFrameworkVersion;
}