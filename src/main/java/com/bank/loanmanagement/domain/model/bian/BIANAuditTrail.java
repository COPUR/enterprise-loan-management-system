package com.bank.loanmanagement.domain.model.bian;

import lombok.Data;
import lombok.Builder;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Data
@Builder
public class BIANAuditTrail {
    private String trailId;
    private List<AuditEntry> entries;
    private LocalDateTime createdAt;
    private String createdBy;
    
    @Data
    @Builder
    public static class AuditEntry {
        private String eventId;
        private String eventType;
        private LocalDateTime timestamp;
        private String userId;
        private Map<String, Object> data;
    }
}