package com.amanahfi.platform.regulatory.infrastructure.dto.hsa;

import lombok.Builder;
import lombok.Data;

/**
 * DTO for HSA Sharia violation notification
 */
@Data
@Builder
public class HsaShariaViolationNotification {
    private String notificationId;
    private String violationType;
    private String severity;
    private String description;
    private String[] affectedProducts;
    private String[] correctiveActions;
    private String shariaJustification;
}