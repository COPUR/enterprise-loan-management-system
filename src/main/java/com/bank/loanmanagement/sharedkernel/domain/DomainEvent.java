
package com.bank.loanmanagement.sharedkernel.domain;

import java.time.LocalDateTime;

public interface DomainEvent {
    LocalDateTime getOccurredOn();
}
