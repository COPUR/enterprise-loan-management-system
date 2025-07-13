package com.bank.infrastructure.versioning;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.http.ResponseEntity;
import org.springframework.http.MediaType;

import java.time.Instant;
import java.util.Map;
import java.util.List;

/**
 * Versioned API Controller for Banking Platform
 * 
 * Provides version-specific endpoints and backward compatibility:
 * - Version discovery and capabilities
 * - Migration guides between versions
 * - Feature deprecation notices
 * - Backward compatibility endpoints
 */
@RestController
@RequestMapping("/api")
public class VersionedApiController {
    
    /**
     * Get API version information and supported versions
     */
    @GetMapping("/versions")
    public ResponseEntity<Map<String, Object>> getVersionInfo() {
        Map<String, Object> versionInfo = Map.of(
            "currentVersion", ApiVersioningConfiguration.CURRENT_VERSION,
            "supportedVersions", List.of("1.0", "2.0"),
            "deprecatedVersions", List.of("1.0"),
            "timestamp", Instant.now().toString(),
            "versioning", Map.of(
                "urlPattern", "/api/v{version}/",
                "headerPattern", "Accept: application/vnd.banking.v{version}+json",
                "customHeader", "X-API-Version: {version}"
            ),
            "documentation", Map.of(
                "v1", "/api/v1/docs",
                "v2", "/api/v2/docs",
                "latest", "/api/docs"
            )
        );
        
        return ResponseEntity.ok(versionInfo);
    }
    
    /**
     * V1 API Root - Deprecated but maintained for backward compatibility
     */
    @GetMapping(value = "/v1", produces = {MediaType.APPLICATION_JSON_VALUE, "application/vnd.banking.v1+json"})
    @ApiVersion(value = "1.0", deprecated = "2.0")
    public ResponseEntity<Map<String, Object>> getV1ApiRoot() {
        Map<String, Object> apiRoot = Map.of(
            "version", "1.0",
            "status", "deprecated",
            "deprecationDate", "2024-01-01",
            "sunsetDate", "2024-12-31",
            "migrationGuide", "/api/v1/migration-guide",
            "currentVersion", ApiVersioningConfiguration.CURRENT_VERSION,
            "upgradeRecommendation", "Please upgrade to API v2.0 for enhanced features and security",
            "links", Map.of(
                "customers", "/api/v1/customers",
                "loans", "/api/v1/loans", 
                "payments", "/api/v1/payments",
                "documentation", "/api/v1/docs"
            )
        );
        
        return ResponseEntity.ok()
            .header("X-API-Deprecated", "true")
            .header("X-API-Deprecation-Date", "2024-01-01")
            .header("X-API-Sunset-Date", "2024-12-31")
            .header("Warning", "299 - \"API version 1.0 is deprecated. Please upgrade to version 2.0\"")
            .body(apiRoot);
    }
    
    /**
     * V2 API Root - Current version
     */
    @GetMapping(value = "/v2", produces = {MediaType.APPLICATION_JSON_VALUE, "application/vnd.banking.v2+json"})
    @ApiVersion(value = "2.0", since = "2.0")
    public ResponseEntity<Map<String, Object>> getV2ApiRoot() {
        Map<String, Object> apiRoot = Map.of(
            "version", "2.0",
            "status", "current",
            "features", List.of(
                "Enhanced FAPI 2.0 security",
                "Real-time SSE events",
                "Improved HATEOAS support",
                "Advanced fraud detection",
                "Comprehensive observability"
            ),
            "links", Map.of(
                "customers", "/api/v2/customers",
                "loans", "/api/v2/loans",
                "payments", "/api/v2/payments",
                "documentation", "/api/v2/docs",
                "openapi", "/api/v2/openapi.json",
                "asyncapi", "/api/v2/asyncapi.json"
            )
        );
        
        return ResponseEntity.ok()
            .header("X-API-Version", "2.0")
            .header("X-API-Status", "current")
            .body(apiRoot);
    }
    
    /**
     * Latest API Root - Always points to current version
     */
    @GetMapping(value = {"", "/"}, produces = {MediaType.APPLICATION_JSON_VALUE, "application/vnd.banking+json"})
    public ResponseEntity<Map<String, Object>> getLatestApiRoot() {
        // Redirect to current version
        return getV2ApiRoot();
    }
    
    /**
     * V1 Migration Guide
     */
    @GetMapping(value = "/v1/migration-guide", produces = MediaType.APPLICATION_JSON_VALUE)
    @ApiVersion(value = "1.0")
    public ResponseEntity<Map<String, Object>> getV1MigrationGuide() {
        Map<String, Object> migrationGuide = Map.of(
            "title", "Migration Guide: API v1.0 to v2.0",
            "overview", "This guide helps you migrate from deprecated API v1.0 to current v2.0",
            "breakingChanges", List.of(
                Map.of(
                    "change", "Authentication",
                    "description", "OAuth 2.1 with mTLS now required",
                    "migration", "Update to use mutual TLS certificates and OAuth 2.1 flows"
                ),
                Map.of(
                    "change", "Response Format",
                    "description", "All responses now include HATEOAS links",
                    "migration", "Update client code to handle _links property in responses"
                ),
                Map.of(
                    "change", "Error Format",
                    "description", "Errors now follow RFC 9457 Problem Details format",
                    "migration", "Update error handling to use new error structure"
                ),
                Map.of(
                    "change", "Headers",
                    "description", "FAPI 2.0 headers now required",
                    "migration", "Include X-FAPI-Financial-Id and X-FAPI-Interaction-Id headers"
                )
            ),
            "newFeatures", List.of(
                "Real-time Server-Sent Events",
                "Webhook notifications", 
                "Enhanced fraud detection",
                "Comprehensive audit trails",
                "Advanced rate limiting"
            ),
            "timeline", Map.of(
                "deprecationDate", "2024-01-01",
                "migrationDeadline", "2024-06-30",
                "sunsetDate", "2024-12-31"
            ),
            "support", Map.of(
                "documentation", "/api/v2/docs",
                "examples", "/api/v2/examples",
                "email", "api-migration@banking.local"
            )
        );
        
        return ResponseEntity.ok()
            .header("X-API-Deprecated", "true")
            .body(migrationGuide);
    }
    
    /**
     * Feature compatibility matrix
     */
    @GetMapping("/compatibility")
    public ResponseEntity<Map<String, Object>> getCompatibilityMatrix() {
        Map<String, Object> compatibility = Map.of(
            "versions", Map.of(
                "1.0", Map.of(
                    "status", "deprecated",
                    "features", Map.of(
                        "basicAuth", true,
                        "oauth2", false,
                        "mtls", false,
                        "hateoas", false,
                        "sse", false,
                        "webhooks", false,
                        "rateLimit", "basic",
                        "fraudDetection", "basic"
                    )
                ),
                "2.0", Map.of(
                    "status", "current",
                    "features", Map.of(
                        "basicAuth", false,
                        "oauth2", true,
                        "mtls", true,
                        "hateoas", true,
                        "sse", true,
                        "webhooks", true,
                        "rateLimit", "advanced",
                        "fraudDetection", "advanced"
                    )
                )
            ),
            "migrationPath", Map.of(
                "1.0 -> 2.0", Map.of(
                    "difficulty", "moderate",
                    "estimatedEffort", "2-4 weeks",
                    "keyChanges", List.of(
                        "Implement OAuth 2.1 + mTLS authentication",
                        "Update response parsing for HATEOAS",
                        "Implement new error handling",
                        "Add FAPI 2.0 headers"
                    ),
                    "benefits", List.of(
                        "Enhanced security",
                        "Real-time capabilities",
                        "Better error handling",
                        "Future-proof architecture"
                    )
                )
            )
        );
        
        return ResponseEntity.ok(compatibility);
    }
    
    /**
     * Health check with version information
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> getHealthWithVersion() {
        Map<String, Object> health = Map.of(
            "status", "UP",
            "timestamp", Instant.now().toString(),
            "api", Map.of(
                "currentVersion", ApiVersioningConfiguration.CURRENT_VERSION,
                "supportedVersions", List.of("1.0", "2.0"),
                "deprecatedVersions", List.of("1.0")
            ),
            "features", Map.of(
                "authentication", "OAuth 2.1 + mTLS",
                "security", "FAPI 2.0 compliant",
                "realtime", "Server-Sent Events",
                "observability", "OpenTelemetry + Prometheus"
            )
        );
        
        return ResponseEntity.ok(health);
    }
    
    /**
     * Get deprecation notices for current client
     */
    @GetMapping("/deprecation-notices")
    public ResponseEntity<Map<String, Object>> getDeprecationNotices() {
        Map<String, Object> notices = Map.of(
            "active", List.of(
                Map.of(
                    "component", "API v1.0",
                    "deprecatedSince", "2024-01-01",
                    "sunsetDate", "2024-12-31",
                    "reason", "Security enhancements and new features in v2.0",
                    "migration", "/api/v1/migration-guide",
                    "impact", "high"
                )
            ),
            "upcoming", List.of(
                // Future deprecations would be listed here
            ),
            "recommendations", List.of(
                "Migrate to API v2.0 before sunset date",
                "Test new authentication flows in staging",
                "Update client libraries to support HATEOAS",
                "Implement proper error handling for new format"
            )
        );
        
        return ResponseEntity.ok(notices);
    }
}