package com.loanmanagement.sharedkernel.domain.model;

import java.util.List;
import java.util.ArrayList;
import com.loanmanagement.sharedkernel.domain.event.DomainEvent;

/**
 * Base class for aggregate roots in DDD.
 * Provides domain event management functionality.
 */
public abstract class AggregateRoot<T> extends Entity<T> {

    private final List<DomainEvent> domainEvents = new ArrayList<>();

    protected AggregateRoot(T id) {
        super(id);
    }

    protected void addDomainEvent(DomainEvent event) {
        if (event != null) {
            domainEvents.add(event);
        }
    }

    public List<DomainEvent> getAndClearEvents() {
        List<DomainEvent> events = new ArrayList<>(domainEvents);
        domainEvents.clear();
        return events;
    }

    public List<DomainEvent> getDomainEvents() {
        return new ArrayList<>(domainEvents);
    }

    public void clearDomainEvents() {
        domainEvents.clear();
    }
}
