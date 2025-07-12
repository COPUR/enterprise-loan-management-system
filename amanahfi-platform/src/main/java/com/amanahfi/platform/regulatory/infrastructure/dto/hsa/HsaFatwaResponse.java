package com.amanahfi.platform.regulatory.infrastructure.dto.hsa;

import lombok.Data;

/**
 * DTO for HSA fatwa validation response
 */
@Data
public class HsaFatwaResponse {
    private String fatwaNumber;
    private String status;
    private String ruling;
    private String justification;
    private String validUntilHijri;
}