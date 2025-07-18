package com.bank.infrastructure.async;

import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import org.springframework.scheduling.annotation.Async;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.Authentication;
import org.springframework.beans.factory.annotation.Qualifier;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;

/**
 * Server-Sent Events Service for Real-time Banking Updates
 * 
 * Provides real-time event streaming to clients using SSE:
 * - Connection management with automatic cleanup
 * - User-specific event filtering and security
 * - Connection health monitoring with heartbeats
 * - Event serialization and formatting
 * - Error handling and reconnection support
 * - Resource management and memory cleanup
 */
@Service
public class ServerSentEventService {
    
    private final ObjectMapper objectMapper;
    private final ScheduledExecutorService scheduledExecutor;
    
    // Active SSE connections mapped by connection ID
    private final Map<String, SseConnection> activeConnections = new ConcurrentHashMap<>();
    
    // Default connection timeout (5 minutes)
    private static final Duration DEFAULT_TIMEOUT = Duration.ofMinutes(5);
    
    // Heartbeat interval (30 seconds)
    private static final Duration HEARTBEAT_INTERVAL = Duration.ofSeconds(30);
    
    public ServerSentEventService(ObjectMapper objectMapper, 
                                @Qualifier("scheduledVirtualThreadExecutor") ScheduledExecutorService scheduledExecutor) {
        this.objectMapper = objectMapper;
        this.scheduledExecutor = scheduledExecutor;
        
        // Start connection cleanup task
        startConnectionCleanupTask();
    }
    
    /**
     * Create new SSE connection for real-time events
     */
    public SseEmitter createEventStream(String resourceId, String eventType, 
                                      Duration timeout, EventFilter eventFilter) {
        String connectionId = UUID.randomUUID().toString();
        String userId = getCurrentUserId();
        
        // Create SSE emitter with timeout
        SseEmitter emitter = new SseEmitter(timeout.toMillis());
        
        // Create connection metadata
        SseConnection connection = SseConnection.builder()
            .connectionId(connectionId)
            .userId(userId)
            .resourceId(resourceId)
            .eventType(eventType)
            .emitter(emitter)
            .eventFilter(eventFilter)
            .createdAt(Instant.now())
            .lastHeartbeat(Instant.now())
            .active(true)
            .build();
        
        // Store connection
        activeConnections.put(connectionId, connection);
        
        // Setup emitter callbacks
        setupEmitterCallbacks(emitter, connectionId);
        
        // Send initial connection event
        sendConnectionEstablished(connection);
        
        // Start heartbeat for this connection
        startHeartbeat(connection);
        
        return emitter;
    }
    
    /**
     * Broadcast event to all matching connections
     */
    @Async("sseExecutor")
    public CompletableFuture<Void> broadcastEvent(BankingEvent event) {
        return CompletableFuture.runAsync(() -> {
            try {
                String eventJson = objectMapper.writeValueAsString(event);
                
                activeConnections.values().parallelStream()
                    .filter(connection -> connection.isActive())
                    .filter(connection -> shouldReceiveEvent(connection, event))
                    .forEach(connection -> sendEventToConnection(connection, event, eventJson));
                    
            } catch (Exception e) {
                System.err.println("Failed to broadcast event: " + e.getMessage());
            }
        });
    }
    
    /**
     * Send event to specific user connections
     */
    @Async("sseExecutor")
    public CompletableFuture<Void> sendEventToUser(String userId, BankingEvent event) {
        return CompletableFuture.runAsync(() -> {
            try {
                String eventJson = objectMapper.writeValueAsString(event);
                
                activeConnections.values().stream()
                    .filter(connection -> connection.isActive())
                    .filter(connection -> userId.equals(connection.getUserId()))
                    .filter(connection -> shouldReceiveEvent(connection, event))
                    .forEach(connection -> sendEventToConnection(connection, event, eventJson));
                    
            } catch (Exception e) {
                System.err.println("Failed to send event to user " + userId + ": " + e.getMessage());
            }
        });
    }
    
    /**
     * Send event to specific resource connections
     */
    @Async("sseExecutor")
    public CompletableFuture<Void> sendEventToResource(String resourceId, BankingEvent event) {
        return CompletableFuture.runAsync(() -> {
            try {
                String eventJson = objectMapper.writeValueAsString(event);
                
                activeConnections.values().stream()
                    .filter(connection -> connection.isActive())
                    .filter(connection -> resourceId.equals(connection.getResourceId()))
                    .filter(connection -> shouldReceiveEvent(connection, event))
                    .forEach(connection -> sendEventToConnection(connection, event, eventJson));
                    
            } catch (Exception e) {
                System.err.println("Failed to send event to resource " + resourceId + ": " + e.getMessage());
            }
        });
    }
    
    /**
     * Close specific connection
     */
    public void closeConnection(String connectionId) {
        SseConnection connection = activeConnections.get(connectionId);
        if (connection != null) {
            closeConnectionInternal(connection);
        }
    }
    
    /**
     * Close all connections for a user
     */
    public void closeUserConnections(String userId) {
        activeConnections.values().stream()
            .filter(connection -> userId.equals(connection.getUserId()))
            .forEach(this::closeConnectionInternal);
    }
    
    /**
     * Get connection statistics
     */
    public ConnectionStats getConnectionStats() {
        long totalConnections = activeConnections.size();
        long activeConnections = this.activeConnections.values().stream()
            .mapToLong(connection -> connection.isActive() ? 1 : 0)
            .sum();
        
        Map<String, Long> connectionsByEventType = this.activeConnections.values().stream()
            .filter(SseConnection::isActive)
            .collect(java.util.stream.Collectors.groupingBy(
                SseConnection::getEventType,
                java.util.stream.Collectors.counting()
            ));
        
        return ConnectionStats.builder()
            .totalConnections(totalConnections)
            .activeConnections(activeConnections)
            .connectionsByEventType(connectionsByEventType)
            .build();
    }
    
    /**
     * Setup emitter callbacks for connection lifecycle
     */
    private void setupEmitterCallbacks(SseEmitter emitter, String connectionId) {
        emitter.onCompletion(() -> {
            SseConnection connection = activeConnections.get(connectionId);
            if (connection != null) {
                connection.setActive(false);
                activeConnections.remove(connectionId);
            }
        });
        
        emitter.onTimeout(() -> {
            SseConnection connection = activeConnections.get(connectionId);
            if (connection != null) {
                closeConnectionInternal(connection);
            }
        });
        
        emitter.onError(throwable -> {
            SseConnection connection = activeConnections.get(connectionId);
            if (connection != null) {
                System.err.println("SSE connection error for " + connectionId + ": " + throwable.getMessage());
                closeConnectionInternal(connection);
            }
        });
    }
    
    /**
     * Send connection established event
     */
    private void sendConnectionEstablished(SseConnection connection) {
        try {
            SseEmitter.SseEventBuilder eventBuilder = SseEmitter.event()
                .name("connected")
                .id(UUID.randomUUID().toString())
                .data(Map.of(
                    "connectionId", connection.getConnectionId(),
                    "message", "Connected to event stream for: " + connection.getResourceId(),
                    "timestamp", Instant.now().toString()
                ));
            
            connection.getEmitter().send(eventBuilder);
        } catch (Exception e) {
            System.err.println("Failed to send connection established event: " + e.getMessage());
            closeConnectionInternal(connection);
        }
    }
    
    /**
     * Start heartbeat for connection
     */
    private void startHeartbeat(SseConnection connection) {
        scheduledExecutor.scheduleAtFixedRate(
            () -> sendHeartbeat(connection),
            HEARTBEAT_INTERVAL.toSeconds(),
            HEARTBEAT_INTERVAL.toSeconds(),
            TimeUnit.SECONDS
        );
    }
    
    /**
     * Send heartbeat to connection
     */
    private void sendHeartbeat(SseConnection connection) {
        if (!connection.isActive()) {
            return;
        }
        
        try {
            SseEmitter.SseEventBuilder eventBuilder = SseEmitter.event()
                .name("heartbeat")
                .data(Map.of(
                    "timestamp", Instant.now().toString(),
                    "connectionId", connection.getConnectionId()
                ));
            
            connection.getEmitter().send(eventBuilder);
            connection.setLastHeartbeat(Instant.now());
            
        } catch (Exception e) {
            System.err.println("Heartbeat failed for connection " + connection.getConnectionId());
            closeConnectionInternal(connection);
        }
    }
    
    /**
     * Check if connection should receive specific event
     */
    private boolean shouldReceiveEvent(SseConnection connection, BankingEvent event) {
        // Check event type filter
        if (!connection.getEventType().equals("all") && 
            !connection.getEventType().equals(event.getEventType())) {
            return false;
        }
        
        // Apply custom event filter if present
        if (connection.getEventFilter() != null) {
            return connection.getEventFilter().shouldReceive(event, connection);
        }
        
        return true;
    }
    
    /**
     * Send event to specific connection
     */
    private void sendEventToConnection(SseConnection connection, BankingEvent event, String eventJson) {
        try {
            SseEmitter.SseEventBuilder eventBuilder = SseEmitter.event()
                .name(event.getEventType())
                .id(event.getEventId())
                .data(eventJson);
            
            connection.getEmitter().send(eventBuilder);
            
        } catch (Exception e) {
            System.err.println("Failed to send event to connection " + connection.getConnectionId() + ": " + e.getMessage());
            closeConnectionInternal(connection);
        }
    }
    
    /**
     * Close connection internally
     */
    private void closeConnectionInternal(SseConnection connection) {
        try {
            connection.setActive(false);
            connection.getEmitter().complete();
            activeConnections.remove(connection.getConnectionId());
        } catch (Exception e) {
            System.err.println("Error closing SSE connection: " + e.getMessage());
        }
    }
    
    /**
     * Start periodic connection cleanup task
     */
    private void startConnectionCleanupTask() {
        scheduledExecutor.scheduleAtFixedRate(
            this::cleanupInactiveConnections,
            1, // Initial delay
            5, // Period
            TimeUnit.MINUTES
        );
    }
    
    /**
     * Clean up inactive connections
     */
    private void cleanupInactiveConnections() {
        Instant cutoff = Instant.now().minus(Duration.ofMinutes(10));
        
        activeConnections.values().removeIf(connection -> {
            if (!connection.isActive() || connection.getLastHeartbeat().isBefore(cutoff)) {
                closeConnectionInternal(connection);
                return true;
            }
            return false;
        });
    }
    
    /**
     * Get current authenticated user ID
     */
    private String getCurrentUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return auth != null ? auth.getName() : "anonymous";
    }
    
    // DTOs and Value Objects
    
    @lombok.Builder
    @lombok.Data
    public static class SseConnection {
        private String connectionId;
        private String userId;
        private String resourceId;
        private String eventType;
        private SseEmitter emitter;
        private EventFilter eventFilter;
        private Instant createdAt;
        private Instant lastHeartbeat;
        private boolean active;
        
        public void setLastHeartbeat(Instant lastHeartbeat) {
            this.lastHeartbeat = lastHeartbeat;
        }
        
        public void setActive(boolean active) {
            this.active = active;
        }
    }
    
    @lombok.Builder
    @lombok.Data
    public static class BankingEvent {
        private String eventId;
        private String eventType;
        private String resourceId;
        private String userId;
        private Object data;
        private Instant timestamp;
        private Map<String, Object> metadata;
    }
    
    @lombok.Builder
    @lombok.Data
    public static class ConnectionStats {
        private long totalConnections;
        private long activeConnections;
        private Map<String, Long> connectionsByEventType;
    }
    
    /**
     * Interface for filtering events per connection
     */
    @FunctionalInterface
    public interface EventFilter {
        boolean shouldReceive(BankingEvent event, SseConnection connection);
    }
    
    /**
     * Predefined event filters for common use cases
     */
    public static class EventFilters {
        
        public static EventFilter forCustomer(String customerId) {
            return (event, connection) -> 
                customerId.equals(event.getMetadata().get("customerId"));
        }
        
        public static EventFilter forLoan(String loanId) {
            return (event, connection) -> 
                loanId.equals(event.getResourceId()) || 
                loanId.equals(event.getMetadata().get("loanId"));
        }
        
        public static EventFilter forPayment(String paymentId) {
            return (event, connection) -> 
                paymentId.equals(event.getResourceId()) || 
                paymentId.equals(event.getMetadata().get("paymentId"));
        }
        
        public static EventFilter forUser(String userId) {
            return (event, connection) -> 
                userId.equals(event.getUserId()) ||
                userId.equals(connection.getUserId());
        }
        
        public static EventFilter highPriorityOnly() {
            return (event, connection) -> {
                Object priority = event.getMetadata().get("priority");
                return "HIGH".equals(priority) || "CRITICAL".equals(priority);
            };
        }
        
        public static EventFilter combine(EventFilter... filters) {
            return (event, connection) -> {
                for (EventFilter filter : filters) {
                    if (!filter.shouldReceive(event, connection)) {
                        return false;
                    }
                }
                return true;
            };
        }
    }
}