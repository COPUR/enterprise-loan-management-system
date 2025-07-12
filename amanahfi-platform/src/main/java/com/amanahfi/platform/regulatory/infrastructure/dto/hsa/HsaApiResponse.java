package com.amanahfi.platform.regulatory.infrastructure.dto.hsa;

import lombok.Data;

/**
 * DTO for HSA API response
 */
@Data
public class HsaApiResponse {
    private String certificateNumber;
    private String status;
    private String message;
    private String fatwaReference;
    private String validUntilHijri;
}