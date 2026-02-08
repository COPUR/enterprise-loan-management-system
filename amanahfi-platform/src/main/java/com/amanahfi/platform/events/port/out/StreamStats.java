package com.amanahfi.platform.events.port.out;

import lombok.Builder;
import lombok.Value;

import java.time.Instant;

/**
 * Statistics for event streams
 */
@Value
@Builder
public class StreamStats {
    String streamName;
    long totalEvents;
    long eventsSentToday;
    long failedEvents;
    double avgLatencyMs;
    Instant lastEventTime;
    boolean isHealthy;
}