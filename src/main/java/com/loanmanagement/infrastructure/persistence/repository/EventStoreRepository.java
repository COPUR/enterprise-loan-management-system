
// infrastructure/persistence/repository/EventStoreRepository.java
package com.loanmanagement.infrastructure.persistence.repository;

import com.loanmanagement.domain.event.DomainEvent;
import com.loanmanagement.infrastructure.persistence.entity.EventStoreEntity;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public class EventStoreRepository {
    
    private final SpringDataEventStoreRepository repository;
    private final ObjectMapper objectMapper;
    
    public EventStoreRepository(SpringDataEventStoreRepository repository, ObjectMapper objectMapper) {
        this.repository = repository;
        this.objectMapper = objectMapper;
    }
    
    public void save(DomainEvent event) {
        try {
            EventStoreEntity entity = new EventStoreEntity();
            entity.setEventId(event.getEventId());
            entity.setAggregateId(event.getAggregateId());
            entity.setAggregateType(getAggregateType(event));
            entity.setEventType(event.getEventType());
            entity.setEventData(objectMapper.writeValueAsString(event));
            entity.setOccurredOn(event.getOccurredOn());
            entity.setVersion(event.getVersion());
            
            repository.save(entity);
        } catch (Exception e) {
            throw new EventStorageException("Failed to store event", e);
        }
    }
    
    public List<DomainEvent> findByAggregateId(String aggregateId) {
        return repository.findByAggregateIdOrderByVersion(aggregateId).stream()
            .map(this::deserialize)
            .toList();
    }
    
    private String getAggregateType(DomainEvent event) {
        if (event.getEventType().startsWith("Loan")) return "Loan";
        if (event.getEventType().startsWith("Credit")) return "Customer";
        if (event.getEventType().startsWith("Payment")) return "Loan";
        return "Unknown";
    }
    
    private DomainEvent deserialize(EventStoreEntity entity) {
        try {
            Class<?> eventClass = Class.forName(
                "com.loanmanagement.domain.event." + entity.getEventType()
            );
            return (DomainEvent) objectMapper.readValue(entity.getEventData(), eventClass);
        } catch (Exception e) {
            throw new EventDeserializationException("Failed to deserialize event", e);
        }
    }
}