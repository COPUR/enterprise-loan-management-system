package com.banking.loan.domain.shared;

import java.time.LocalDateTime;
import java.util.Map;

public record EventMetadata(
    String eventId,
    String eventType,
    String aggregateId,
    String aggregateType,
    Long version,
    LocalDateTime timestamp,
    String correlationId,
    String causationId,
    String userId,
    Map<String, String> additionalMetadata
) {}