package com.bank.payment.infrastructure.fraud;

import java.time.LocalDateTime;

/**
 * Network Context for Fraud Analysis
 */
public class NetworkContext {
    
    private final String ipAddress;
    private final String userAgent;
    private final String sessionId;
    private final LocalDateTime timestamp;
    
    private NetworkContext(Builder builder) {
        this.ipAddress = builder.ipAddress;
        this.userAgent = builder.userAgent;
        this.sessionId = builder.sessionId;
        this.timestamp = builder.timestamp;
    }
    
    // Getters
    public String getIpAddress() { return ipAddress; }
    public String getUserAgent() { return userAgent; }
    public String getSessionId() { return sessionId; }
    public LocalDateTime getTimestamp() { return timestamp; }
    
    public static Builder builder() {
        return new Builder();
    }
    
    public static class Builder {
        private String ipAddress;
        private String userAgent;
        private String sessionId;
        private LocalDateTime timestamp;
        
        public Builder ipAddress(String ipAddress) {
            this.ipAddress = ipAddress;
            return this;
        }
        
        public Builder userAgent(String userAgent) {
            this.userAgent = userAgent;
            return this;
        }
        
        public Builder sessionId(String sessionId) {
            this.sessionId = sessionId;
            return this;
        }
        
        public Builder timestamp(LocalDateTime timestamp) {
            this.timestamp = timestamp;
            return this;
        }
        
        public NetworkContext build() {
            return new NetworkContext(this);
        }
    }
}