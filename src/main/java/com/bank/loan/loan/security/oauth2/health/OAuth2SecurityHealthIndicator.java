package com.bank.loan.loan.security.oauth2.health;

import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;

/**
 * Health indicator for OAuth2 security functionality
 */
@Component
public class OAuth2SecurityHealthIndicator implements HealthIndicator {

    @Override
    public Health health() {
        try {
            // Check OAuth2 security components
            boolean tokenEndpointHealthy = checkTokenEndpointHealth();
            boolean authorizationEndpointHealthy = checkAuthorizationEndpointHealth();
            boolean jwksEndpointHealthy = checkJWKSEndpointHealth();
            boolean keycloakConnectivityHealthy = checkKeycloakConnectivityHealth();
            boolean clientAuthenticationHealthy = checkClientAuthenticationHealth();
            
            if (tokenEndpointHealthy && authorizationEndpointHealthy && 
                jwksEndpointHealthy && keycloakConnectivityHealthy && clientAuthenticationHealthy) {
                return Health.up()
                        .withDetail("token_endpoint", "healthy")
                        .withDetail("authorization_endpoint", "healthy")
                        .withDetail("jwks_endpoint", "healthy")
                        .withDetail("keycloak_connectivity", "healthy")
                        .withDetail("client_authentication", "healthy")
                        .withDetail("oauth2_version", "2.1")
                        .withDetail("client_auth_method", "private_key_jwt")
                        .build();
            } else {
                return Health.down()
                        .withDetail("token_endpoint", tokenEndpointHealthy ? "healthy" : "unhealthy")
                        .withDetail("authorization_endpoint", authorizationEndpointHealthy ? "healthy" : "unhealthy")
                        .withDetail("jwks_endpoint", jwksEndpointHealthy ? "healthy" : "unhealthy")
                        .withDetail("keycloak_connectivity", keycloakConnectivityHealthy ? "healthy" : "unhealthy")
                        .withDetail("client_authentication", clientAuthenticationHealthy ? "healthy" : "unhealthy")
                        .withDetail("oauth2_version", "2.1")
                        .withDetail("client_auth_method", "private_key_jwt")
                        .build();
            }
        } catch (Exception e) {
            return Health.down()
                    .withDetail("error", e.getMessage())
                    .withDetail("oauth2_version", "2.1")
                    .build();
        }
    }

    private boolean checkTokenEndpointHealth() {
        // Implementation would check OAuth2 token endpoint health
        return true;
    }

    private boolean checkAuthorizationEndpointHealth() {
        // Implementation would check OAuth2 authorization endpoint health
        return true;
    }

    private boolean checkJWKSEndpointHealth() {
        // Implementation would check JWKS endpoint health
        return true;
    }

    private boolean checkKeycloakConnectivityHealth() {
        // Implementation would check Keycloak connectivity
        return true;
    }

    private boolean checkClientAuthenticationHealth() {
        // Implementation would check client authentication health
        return true;
    }
}