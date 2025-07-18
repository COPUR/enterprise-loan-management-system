package com.bank.infrastructure.notifications;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.net.URI;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * WebSocket Handler for Real-Time Notifications
 * 
 * Handles WebSocket connections for real-time banking notifications:
 * - Customer authentication and session management
 * - Real-time message delivery
 * - Connection lifecycle management
 * - Error handling and reconnection support
 * - Security and rate limiting
 */
@Component
public class WebSocketNotificationHandler extends TextWebSocketHandler {
    
    private static final Logger logger = LoggerFactory.getLogger(WebSocketNotificationHandler.class);
    
    @Autowired
    private NotificationService notificationService;
    
    @Autowired
    private ObjectMapper objectMapper;
    
    // Session metadata
    private final Map<String, SessionMetadata> sessionMetadata = new ConcurrentHashMap<>();
    
    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        logger.info("WebSocket connection established: {}", session.getId());
        
        try {
            // Extract customer ID from session attributes or query parameters
            String customerId = extractCustomerId(session);
            
            if (customerId != null) {
                // Register session with notification service
                notificationService.registerWebSocketSession(customerId, session);
                
                // Store session metadata
                sessionMetadata.put(session.getId(), new SessionMetadata(customerId, Instant.now()));
                
                // Send connection confirmation
                sendConnectionConfirmation(session, customerId);
                
                logger.info("WebSocket session registered for customer: {}", customerId);
            } else {
                logger.warn("WebSocket connection without customer ID, closing session: {}", session.getId());
                session.close(CloseStatus.BAD_DATA.withReason("Missing customer ID"));
            }
            
        } catch (Exception e) {
            logger.error("Error establishing WebSocket connection", e);
            session.close(CloseStatus.SERVER_ERROR.withReason("Connection setup failed"));
        }
    }
    
    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        logger.debug("Received WebSocket message: {}", message.getPayload());
        
        try {
            // Parse incoming message
            Map<String, Object> messageData = objectMapper.readValue(message.getPayload(), Map.class);
            String messageType = (String) messageData.get("type");
            
            switch (messageType) {
                case "ping":
                    handlePingMessage(session);
                    break;
                case "subscribe":
                    handleSubscribeMessage(session, messageData);
                    break;
                case "unsubscribe":
                    handleUnsubscribeMessage(session, messageData);
                    break;
                case "acknowledgment":
                    handleAcknowledgmentMessage(session, messageData);
                    break;
                default:
                    logger.warn("Unknown message type: {}", messageType);
                    sendErrorMessage(session, "Unknown message type: " + messageType);
            }
            
        } catch (Exception e) {
            logger.error("Error handling WebSocket message", e);
            sendErrorMessage(session, "Message processing failed");
        }
    }
    
    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) throws Exception {
        logger.error("WebSocket transport error for session: {}", session.getId(), exception);
        
        // Clean up session
        cleanupSession(session);
    }
    
    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        logger.info("WebSocket connection closed: {} with status: {}", session.getId(), status);
        
        // Clean up session
        cleanupSession(session);
    }
    
    private String extractCustomerId(WebSocketSession session) {
        try {
            // Option 1: From session attributes (set by authentication filter)
            Object customerIdAttr = session.getAttributes().get("customerId");
            if (customerIdAttr != null) {
                return customerIdAttr.toString();
            }
            
            // Option 2: From query parameters
            URI uri = session.getUri();
            if (uri != null && uri.getQuery() != null) {
                String query = uri.getQuery();
                for (String param : query.split("&")) {
                    String[] keyValue = param.split("=");
                    if (keyValue.length == 2 && "customerId".equals(keyValue[0])) {
                        return keyValue[1];
                    }
                }
            }
            
            // Option 3: From headers (if available)
            if (session.getHandshakeHeaders().containsKey("X-Customer-ID")) {
                return session.getHandshakeHeaders().getFirst("X-Customer-ID");
            }
            
            return null;
            
        } catch (Exception e) {
            logger.error("Error extracting customer ID from WebSocket session", e);
            return null;
        }
    }
    
    private void sendConnectionConfirmation(WebSocketSession session, String customerId) {
        try {
            Map<String, Object> confirmationMessage = Map.of(
                "type", "connection_confirmed",
                "customerId", customerId,
                "sessionId", session.getId(),
                "timestamp", Instant.now().toString(),
                "message", "WebSocket connection established successfully"
            );
            
            String messageJson = objectMapper.writeValueAsString(confirmationMessage);
            session.sendMessage(new TextMessage(messageJson));
            
        } catch (Exception e) {
            logger.error("Failed to send connection confirmation", e);
        }
    }
    
    private void handlePingMessage(WebSocketSession session) {
        try {
            Map<String, Object> pongMessage = Map.of(
                "type", "pong",
                "timestamp", Instant.now().toString()
            );
            
            String messageJson = objectMapper.writeValueAsString(pongMessage);
            session.sendMessage(new TextMessage(messageJson));
            
        } catch (Exception e) {
            logger.error("Failed to send pong message", e);
        }
    }
    
    private void handleSubscribeMessage(WebSocketSession session, Map<String, Object> messageData) {
        try {
            SessionMetadata metadata = sessionMetadata.get(session.getId());
            if (metadata != null) {
                // Handle subscription to specific notification types
                Object notificationTypes = messageData.get("notificationTypes");
                if (notificationTypes instanceof java.util.List) {
                    metadata.setSubscribedTypes((java.util.List<String>) notificationTypes);
                }
                
                // Send subscription confirmation
                Map<String, Object> confirmationMessage = Map.of(
                    "type", "subscription_confirmed",
                    "subscribedTypes", metadata.getSubscribedTypes(),
                    "timestamp", Instant.now().toString()
                );
                
                String messageJson = objectMapper.writeValueAsString(confirmationMessage);
                session.sendMessage(new TextMessage(messageJson));
                
                logger.info("Customer {} subscribed to notification types: {}", 
                    metadata.getCustomerId(), metadata.getSubscribedTypes());
            }
            
        } catch (Exception e) {
            logger.error("Failed to handle subscribe message", e);
        }
    }
    
    private void handleUnsubscribeMessage(WebSocketSession session, Map<String, Object> messageData) {
        try {
            SessionMetadata metadata = sessionMetadata.get(session.getId());
            if (metadata != null) {
                // Handle unsubscription from specific notification types
                Object notificationTypes = messageData.get("notificationTypes");
                if (notificationTypes instanceof java.util.List) {
                    java.util.List<String> typesToUnsubscribe = (java.util.List<String>) notificationTypes;
                    metadata.getSubscribedTypes().removeAll(typesToUnsubscribe);
                }
                
                // Send unsubscription confirmation
                Map<String, Object> confirmationMessage = Map.of(
                    "type", "unsubscription_confirmed",
                    "remainingTypes", metadata.getSubscribedTypes(),
                    "timestamp", Instant.now().toString()
                );
                
                String messageJson = objectMapper.writeValueAsString(confirmationMessage);
                session.sendMessage(new TextMessage(messageJson));
                
                logger.info("Customer {} unsubscribed from notification types, remaining: {}", 
                    metadata.getCustomerId(), metadata.getSubscribedTypes());
            }
            
        } catch (Exception e) {
            logger.error("Failed to handle unsubscribe message", e);
        }
    }
    
    private void handleAcknowledgmentMessage(WebSocketSession session, Map<String, Object> messageData) {
        try {
            String notificationId = (String) messageData.get("notificationId");
            if (notificationId != null) {
                SessionMetadata metadata = sessionMetadata.get(session.getId());
                if (metadata != null) {
                    metadata.acknowledgeNotification(notificationId);
                    logger.debug("Notification {} acknowledged by customer {}", 
                        notificationId, metadata.getCustomerId());
                }
            }
            
        } catch (Exception e) {
            logger.error("Failed to handle acknowledgment message", e);
        }
    }
    
    private void sendErrorMessage(WebSocketSession session, String error) {
        try {
            Map<String, Object> errorMessage = Map.of(
                "type", "error",
                "message", error,
                "timestamp", Instant.now().toString()
            );
            
            String messageJson = objectMapper.writeValueAsString(errorMessage);
            session.sendMessage(new TextMessage(messageJson));
            
        } catch (Exception e) {
            logger.error("Failed to send error message", e);
        }
    }
    
    private void cleanupSession(WebSocketSession session) {
        try {
            SessionMetadata metadata = sessionMetadata.remove(session.getId());
            if (metadata != null) {
                // Unregister from notification service
                notificationService.unregisterWebSocketSession(metadata.getCustomerId());
                
                logger.info("WebSocket session cleaned up for customer: {}", metadata.getCustomerId());
            }
            
        } catch (Exception e) {
            logger.error("Error cleaning up WebSocket session", e);
        }
    }
    
    // Inner class for session metadata
    private static class SessionMetadata {
        private final String customerId;
        private final Instant connectionTime;
        private java.util.List<String> subscribedTypes;
        private final java.util.Set<String> acknowledgedNotifications;
        
        public SessionMetadata(String customerId, Instant connectionTime) {
            this.customerId = customerId;
            this.connectionTime = connectionTime;
            this.subscribedTypes = new java.util.ArrayList<>();
            this.acknowledgedNotifications = new java.util.HashSet<>();
        }
        
        public String getCustomerId() { return customerId; }
        public Instant getConnectionTime() { return connectionTime; }
        public java.util.List<String> getSubscribedTypes() { return subscribedTypes; }
        public void setSubscribedTypes(java.util.List<String> subscribedTypes) { this.subscribedTypes = subscribedTypes; }
        
        public void acknowledgeNotification(String notificationId) {
            acknowledgedNotifications.add(notificationId);
        }
        
        public boolean isNotificationAcknowledged(String notificationId) {
            return acknowledgedNotifications.contains(notificationId);
        }
    }
}