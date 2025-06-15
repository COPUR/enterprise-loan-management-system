
package com.bank.loanmanagement.sharedkernel.domain;

import java.util.List;

public interface AggregateRoot {
    List<DomainEvent> getDomainEvents();
    void clearDomainEvents();
}
