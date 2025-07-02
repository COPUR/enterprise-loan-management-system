//This can evolve into SAGA
package com.loanmanagement.application.query;

import com.loanmanagement.domain.event.DomainEvent;
import com.loanmanagement.infrastructure.persistence.repository.EventStoreRepository;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
public class LoanHistoryQueryHandler {

    private final EventStoreRepository eventStore;

    public LoanHistoryQueryHandler(EventStoreRepository eventStore) {
        this.eventStore = eventStore;
    }

    public LoanHistory getLoanHistory(Long loanId) {
        List<DomainEvent> events = eventStore.findByAggregateId(loanId.toString());

        return LoanHistory.fromEvents(events);
    }

    public CustomerActivityHistory getCustomerActivity(Long customerId) {
        // Query events related to customer
        // Build activity timeline
        return new CustomerActivityHistory(customerId, events);
    }
}