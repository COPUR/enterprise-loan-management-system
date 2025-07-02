package com.bank.loanmanagement.loan.infrastructure.messaging;

import java.time.Instant;

public class DeadLetterEvent {
    public static DeadLetterEventBuilder builder() { return null; }
    public static class DeadLetterEventBuilder { public DeadLetterEventBuilder originalEventType(String type) { return null; } public DeadLetterEventBuilder originalEventId(String id) { return null; } public DeadLetterEventBuilder originalAggregateId(String id) { return null; } public DeadLetterEventBuilder errorMessage(String message) { return null; } public DeadLetterEventBuilder errorTimestamp(Instant timestamp) { return null; } public DeadLetterEventBuilder retryCount(int count) { return null; } public DeadLetterEventBuilder maxRetries(int retries) { return null; } public DeadLetterEvent build() { return null; } }
}