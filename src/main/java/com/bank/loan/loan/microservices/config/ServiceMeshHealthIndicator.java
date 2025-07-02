package com.bank.loanmanagement.loan.microservices.config;

import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;

@Component
public class ServiceMeshHealthIndicator implements HealthIndicator {

    @Override
    public Health health() {
        try {
            // Check if Envoy sidecar is available
            String istioEnabled = System.getenv("ISTIO_ENABLED");
            String podIP = System.getenv("POD_IP");
            
            if ("true".equals(istioEnabled) && podIP != null) {
                return Health.up()
                    .withDetail("istio", "enabled")
                    .withDetail("sidecar", "injected")
                    .withDetail("pod-ip", podIP)
                    .withDetail("service-mesh", "operational")
                    .build();
            } else {
                return Health.up()
                    .withDetail("istio", "disabled")
                    .withDetail("mode", "standalone")
                    .build();
            }
        } catch (Exception e) {
            return Health.down()
                .withDetail("error", e.getMessage())
                .withDetail("service-mesh", "failed")
                .build();
        }
    }
}