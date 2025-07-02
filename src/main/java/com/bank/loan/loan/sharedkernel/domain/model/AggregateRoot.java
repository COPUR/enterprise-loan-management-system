package com.bank.loan.loan.sharedkernel.domain.model;

import com.bank.loan.loan.sharedkernel.domain.event.DomainEvent;
import com.bank.loan.loan.domain.shared.DomainId;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Base class for all aggregate roots in the domain
 * Implements domain event management and aggregate identity
 */
public abstract class AggregateRoot<ID extends DomainId> {
    
    private final ID id;
    private long version;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
    private final List<DomainEvent> domainEvents = new ArrayList<>();
    
    protected AggregateRoot(ID id) {
        this.id = id;
        this.version = 0L;
        this.createdAt = OffsetDateTime.now();
        this.updatedAt = OffsetDateTime.now();
    }
    
    public ID getId() {
        return id;
    }
    
    public long getVersion() {
        return version;
    }
    
    public void setVersion(long version) {
        this.version = version;
    }
    
    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }
    
    public void setCreatedAt(OffsetDateTime createdAt) {
        this.createdAt = createdAt;
    }
    
    public OffsetDateTime getUpdatedAt() {
        return updatedAt;
    }
    
    public void setUpdatedAt(OffsetDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
    
    protected void addDomainEvent(DomainEvent event) {
        this.domainEvents.add(event);
        this.updatedAt = OffsetDateTime.now();
    }
    
    public List<DomainEvent> getDomainEvents() {
        return Collections.unmodifiableList(domainEvents);
    }
    
    public void clearDomainEvents() {
        this.domainEvents.clear();
    }
    
    protected void incrementVersion() {
        this.version++;
        this.updatedAt = OffsetDateTime.now();
    }
    
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        AggregateRoot<?> that = (AggregateRoot<?>) obj;
        return id.equals(that.id);
    }
    
    @Override
    public int hashCode() {
        return id.hashCode();
    }
}