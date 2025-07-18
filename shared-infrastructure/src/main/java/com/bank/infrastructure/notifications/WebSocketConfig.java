package com.bank.infrastructure.notifications;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

/**
 * WebSocket Configuration for Real-Time Notifications
 * 
 * Configures WebSocket endpoints for real-time banking notifications:
 * - Customer notification streams
 * - Admin monitoring streams
 * - System status updates
 * - Cross-origin configuration
 * - Security settings
 */
@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {
    
    @Autowired
    private WebSocketNotificationHandler webSocketNotificationHandler;
    
    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        
        // Customer notifications endpoint
        registry.addHandler(webSocketNotificationHandler, "/ws/notifications")
                .setAllowedOrigins("*") // Configure based on your domain requirements
                .withSockJS(); // Enable SockJS fallback for older browsers
        
        // Admin monitoring endpoint (could be separate handler)
        registry.addHandler(webSocketNotificationHandler, "/ws/admin/notifications")
                .setAllowedOrigins("*")
                .withSockJS();
        
        // System status endpoint
        registry.addHandler(webSocketNotificationHandler, "/ws/system/status")
                .setAllowedOrigins("*")
                .withSockJS();
    }
}