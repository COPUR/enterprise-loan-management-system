package com.amanahfi.platform.regulatory.infrastructure.dto.cbuae;

import lombok.Data;

/**
 * DTO for CBUAE API response
 */
@Data
public class CbuaeApiResponse {
    private String referenceNumber;
    private String status;
    private String message;
    private String timestamp;
}