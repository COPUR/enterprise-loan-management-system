package com.amanahfi.platform.events.domain;

import lombok.Builder;
import lombok.Value;

import java.time.Instant;

/**
 * Snapshot of event store state
 */
@Value
@Builder
public class EventStoreSnapshot {
    EventStoreId eventStoreId;
    String aggregateId;
    String aggregateType;
    long version;
    int eventCount;
    Instant createdAt;
    Instant lastUpdated;
}