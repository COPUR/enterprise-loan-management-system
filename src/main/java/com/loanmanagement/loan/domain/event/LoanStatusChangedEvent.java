package com.loanmanagement.loan.domain.event;

import java.time.LocalDateTime;

public record LoanStatusChangedEvent(
        Long loanId,
        Long customerId,
        String previousStatus,
        String newStatus,
        String reason,
        LocalDateTime occurredAt
) {
    public LoanStatusChangedEvent(Long loanId, Long customerId, 
                                String previousStatus, String newStatus) {
        this(loanId, customerId, previousStatus, newStatus, null, LocalDateTime.now());
    }
    
    public LoanStatusChangedEvent(Long loanId, Long customerId, 
                                String previousStatus, String newStatus, String reason) {
        this(loanId, customerId, previousStatus, newStatus, reason, LocalDateTime.now());
    }
}