package com.loanmanagement.infrastructure.event;

import com.loanmanagement.domain.event.DomainEvent;
import com.loanmanagement.domain.port.DomainEventPublisher;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Logger;
import java.util.logging.Level;

/**
 * Infrastructure adapter for domain event publishing following hexagonal architecture.
 * This implementation serves as a fallback while Spring Framework dependencies are resolved.
 *
 * Follows DDD principles by:
 * - Implementing the domain port interface
 * - Separating infrastructure concerns from domain logic
 * - Maintaining event publishing semantics
 *
 * Supports 12-Factor App principles by:
 * - Treating logs as event streams
 * - Explicit dependency management
 * - Clean separation of concerns
 *
 * Note: This is a fallback implementation. To restore Spring functionality:
 * 1. Run: ./gradlew clean build --refresh-dependencies
 * 2. Verify Spring Framework dependencies in build.gradle
 * 3. Add @Component annotation once Spring context is available
 */
public class SpringDomainEventPublisher implements DomainEventPublisher {
    
    private static final Logger logger = Logger.getLogger(SpringDomainEventPublisher.class.getName());

    // Thread-safe collection for event listeners
    private final List<DomainEventListener> listeners = new CopyOnWriteArrayList<>();

    // Event statistics for monitoring (12-Factor: metrics) - using atomic operations
    private final AtomicLong eventsPublished = new AtomicLong(0);
    private final AtomicLong eventsProcessed = new AtomicLong(0);

    /**
     * Default constructor for dependency injection compatibility.
     * Once Spring is available, this will be replaced with constructor injection.
     */
    public SpringDomainEventPublisher() {
        logger.info("Domain event publisher initialized in fallback mode");
    }

    /**
     * Publishes a single domain event following DDD principles.
     * Implements thread-safe processing to avoid blocking domain operations.
     */
    @Override
    public void publish(DomainEvent event) {
        if (event == null) {
            logger.warning("Attempted to publish null domain event");
            return;
        }

        eventsPublished.incrementAndGet();
        logger.info(String.format("Publishing domain event: %s [ID: %s]",
            event.getClass().getSimpleName(),
            event.getEventId()));

        // Process event with error handling following clean code principles
        processEvent(event);
    }

    /**
     * Publishes multiple domain events in batch for efficiency.
     * Follows 12-Factor principle of treating logs as event streams.
     */
    @Override
    public void publishAll(List<DomainEvent> events) {
        if (events == null || events.isEmpty()) {
            logger.info("No domain events to publish");
            return;
        }

        logger.info(String.format("Publishing batch of %d domain events", events.size()));

        // Process each event with individual error handling
        events.forEach(this::publish);

        logger.info(String.format("Completed publishing batch of %d domain events", events.size()));
    }

    /**
     * Adds an event listener for processing domain events.
     * Supports hexagonal architecture by allowing multiple adapters to subscribe.
     */
    public void addListener(DomainEventListener listener) {
        if (listener != null) {
            listeners.add(listener);
            logger.info(String.format("Added domain event listener: %s",
                listener.getClass().getSimpleName()));
        }
    }

    /**
     * Removes an event listener.
     * Provides clean lifecycle management following clean code principles.
     */
    public void removeListener(DomainEventListener listener) {
        if (listeners.remove(listener)) {
            logger.info(String.format("Removed domain event listener: %s",
                listener.getClass().getSimpleName()));
        }
    }

    /**
     * Returns the number of registered listeners for monitoring.
     * Supports 12-Factor principle of disposability and monitoring.
     */
    public int getListenerCount() {
        return listeners.size();
    }

    /**
     * Returns event publishing statistics for monitoring.
     * Follows 12-Factor principle of treating logs as event streams.
     */
    public EventStatistics getStatistics() {
        return new EventStatistics(eventsPublished.get(), eventsProcessed.get(), listeners.size());
    }

    /**
     * Processes a single event with error handling and logging.
     * Private method following clean code principles of small, focused methods.
     */
    private void processEvent(DomainEvent event) {
        try {
            // Notify all registered listeners
            for (DomainEventListener listener : listeners) {
                try {
                    listener.handle(event);
                    eventsProcessed.incrementAndGet();
                } catch (Exception e) {
                    logger.log(Level.SEVERE,
                        String.format("Error processing event %s with listener %s",
                            event.getClass().getSimpleName(),
                            listener.getClass().getSimpleName()), e);
                }
            }

            logger.fine(String.format("Successfully processed event: %s",
                event.getClass().getSimpleName()));

        } catch (Exception e) {
            logger.log(Level.SEVERE,
                String.format("Critical error processing domain event: %s",
                    event.getClass().getSimpleName()), e);
        }
    }

    /**
     * Interface for domain event listeners.
     * Follows hexagonal architecture by defining clear contracts.
     */
    public interface DomainEventListener {
        void handle(DomainEvent event);
    }

    /**
     * Value object for event publishing statistics.
     * Follows DDD principles with immutable value objects.
     */
    public static class EventStatistics {
        private final long eventsPublished;
        private final long eventsProcessed;
        private final int activeListeners;

        public EventStatistics(long eventsPublished, long eventsProcessed, int activeListeners) {
            this.eventsPublished = eventsPublished;
            this.eventsProcessed = eventsProcessed;
            this.activeListeners = activeListeners;
        }

        public long getEventsPublished() { return eventsPublished; }
        public long getEventsProcessed() { return eventsProcessed; }
        public int getActiveListeners() { return activeListeners; }

        @Override
        public String toString() {
            return String.format("EventStatistics{published=%d, processed=%d, listeners=%d}",
                eventsPublished, eventsProcessed, activeListeners);
        }
    }
}

/*
 * SPRING FRAMEWORK RESTORATION GUIDE:
 *
 * Once Spring dependencies are resolved, restore this structure:
 *
 * @Component
 * public class SpringDomainEventPublisher implements DomainEventPublisher {
 *
 *     private final ApplicationEventPublisher eventPublisher;
 *
 *     public SpringDomainEventPublisher(ApplicationEventPublisher eventPublisher) {
 *         this.eventPublisher = eventPublisher;
 *     }
 *
 *     @Override
 *     public void publish(DomainEvent event) {
 *         eventPublisher.publishEvent(event);
 *     }
 *
 *     @Override
 *     public void publishAll(List<DomainEvent> events) {
 *         events.forEach(eventPublisher::publishEvent);
 *     }
 * }
 *
 * ARCHITECTURAL BENEFITS:
 * - 12-Factor: Explicit dependency management, logs as event streams, disposability
 * - DDD: Clean separation between domain events and infrastructure concerns
 * - Hexagonal: Infrastructure adapter that implements domain port interface
 * - Clean Code: Thread-safe operations, error handling, comprehensive logging
 */
