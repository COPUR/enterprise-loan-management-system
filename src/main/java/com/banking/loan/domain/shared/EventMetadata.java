package com.banking.loan.domain.shared;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Collections;

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
) {
    public static EventMetadata empty() {
        return new EventMetadata(null, null, null, null, null, null, null, null, null, Collections.emptyMap());
    }
    
    public static EventMetadata of(String eventId, String eventType) {
        return new EventMetadata(eventId, eventType, null, null, null, LocalDateTime.now(), null, null, null, Collections.emptyMap());
    }
}