package com.amanahfi.compliance.domain.check;

import java.time.LocalDateTime;

public class ComplianceCheckArchivedEvent {
    private final String checkId;
    private final String archiveReason;
    private final LocalDateTime timestamp;

    public ComplianceCheckArchivedEvent(String checkId, String archiveReason) {
        this.checkId = checkId;
        this.archiveReason = archiveReason;
        this.timestamp = LocalDateTime.now();
    }

    public String getCheckId() { return checkId; }
    public String getArchiveReason() { return archiveReason; }
    public LocalDateTime getTimestamp() { return timestamp; }
}