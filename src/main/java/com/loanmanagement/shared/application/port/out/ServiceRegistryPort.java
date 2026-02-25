package com.loanmanagement.shared.application.port.out;

import java.util.List;
import java.util.Optional;

/**
 * Outbound Port for Service Registry
 * Abstracts service discovery and registration from the application layer
 */
public interface ServiceRegistryPort {
    
    /**
     * Register a service instance
     */
    void registerService(ServiceInstance instance);
    
    /**
     * Deregister a service instance
     */
    void deregisterService(String serviceId, String instanceId);
    
    /**
     * Discover service instances by service name
     */
    List<ServiceInstance> discoverServices(String serviceName);
    
    /**
     * Get a specific service instance
     */
    Optional<ServiceInstance> getServiceInstance(String serviceId, String instanceId);
    
    /**
     * Check service health
     */
    HealthStatus checkServiceHealth(String serviceId, String instanceId);
    
    /**
     * Service instance information
     */
    record ServiceInstance(
            String serviceId,
            String instanceId,
            String host,
            int port,
            boolean secure,
            java.util.Map<String, String> metadata,
            HealthStatus status
    ) {
        public String getUrl() {
            String protocol = secure ? "https" : "http";
            return String.format("%s://%s:%d", protocol, host, port);
        }
    }
    
    /**
     * Health status of a service
     */
    enum HealthStatus {
        UP, DOWN, OUT_OF_SERVICE, UNKNOWN
    }
}