package com.amanahfi.platform.regulatory.infrastructure.dto.vara;

import lombok.Data;

/**
 * DTO for VARA API response
 */
@Data
public class VaraApiResponse {
    private String submissionReference;
    private String status;
    private String message;
    private String nextReviewDate;
}