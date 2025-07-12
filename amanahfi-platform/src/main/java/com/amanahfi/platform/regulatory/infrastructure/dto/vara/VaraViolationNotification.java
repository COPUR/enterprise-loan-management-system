package com.amanahfi.platform.regulatory.infrastructure.dto.vara;

import lombok.Builder;
import lombok.Data;

import java.time.Instant;

/**
 * DTO for VARA violation notification
 */
@Data
@Builder
public class VaraViolationNotification {
    private String notificationId;
    private String violationType;
    private String severity;
    private String description;
    private String[] affectedAssets;
    private String[] immediateActions;
    private Instant detectedAt;
}