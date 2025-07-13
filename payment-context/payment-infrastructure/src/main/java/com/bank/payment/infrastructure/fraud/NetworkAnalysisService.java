package com.bank.payment.infrastructure.fraud;

import org.springframework.stereotype.Service;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Network Analysis Service for Fraud Detection
 */
@Service
public class NetworkAnalysisService {
    
    private final Map<String, List<NetworkEvent>> ipHistory = new ConcurrentHashMap<>();
    private final Set<String> knownMaliciousIPs = Set.of(
        "192.168.100.1", "10.0.0.1" // Mock malicious IPs
    );
    
    public int analyzeNetworkRisk(NetworkContext context) {
        int score = 0;
        
        // Check malicious IP
        if (knownMaliciousIPs.contains(context.getIpAddress())) {
            score += 50;
        }
        
        // Check IP velocity
        List<NetworkEvent> events = ipHistory.computeIfAbsent(
            context.getIpAddress(), k -> new ArrayList<>()
        );
        
        events.add(new NetworkEvent(context.getIpAddress(), LocalDateTime.now()));
        
        // Count recent connections from this IP
        long recentConnections = events.stream()
            .filter(event -> event.isRecent(1)) // Last hour
            .count();
        
        if (recentConnections > 100) {
            score += 30;
        } else if (recentConnections > 50) {
            score += 20;
        }
        
        return score;
    }
    
    private static class NetworkEvent {
        private final String ipAddress;
        private final LocalDateTime timestamp;
        
        public NetworkEvent(String ipAddress, LocalDateTime timestamp) {
            this.ipAddress = ipAddress;
            this.timestamp = timestamp;
        }
        
        public boolean isRecent(int hours) {
            return timestamp.isAfter(LocalDateTime.now().minusHours(hours));
        }
    }
}