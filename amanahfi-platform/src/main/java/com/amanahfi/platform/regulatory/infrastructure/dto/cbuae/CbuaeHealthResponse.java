package com.amanahfi.platform.regulatory.infrastructure.dto.cbuae;

import lombok.Data;

/**
 * DTO for CBUAE health check response
 */
@Data
public class CbuaeHealthResponse {
    private boolean healthy;
    private String version;
    private String environment;
}