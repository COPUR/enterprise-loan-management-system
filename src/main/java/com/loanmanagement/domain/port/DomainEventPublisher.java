// domain/port/DomainEventPublisher.java
package com.loanmanagement.domain.port;

import com.loanmanagement.domain.event.DomainEvent;
import java.util.List;

public interface DomainEventPublisher {
    void publish(DomainEvent event);
    void publishAll(List<DomainEvent> events);
}