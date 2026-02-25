package com.loanmanagement.security.zerotrust.domain.model;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * Security event in the system
 */
public record SecurityEvent(
    String eventType,
    String description,
    LocalDateTime timestamp,
    Map<String, Object> metadata
) {}