package com.bank.infrastructure.api.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.List;
import java.util.Map;

/**
 * API Versioning Configuration
 * 
 * Manages API versioning policies and settings:
 * - Supported API versions
 * - Deprecation policies
 * - Version routing rules
 * - Backward compatibility settings
 */
@Configuration
@ConfigurationProperties(prefix = "banking.api.versioning")
public class ApiVersioningConfiguration {
    
    private boolean enabled = true;
    private String currentVersion = "v1";
    private List<String> supportedVersions = List.of("v1");
    private List<String> deprecatedVersions = List.of();
    private Map<String, String> versionRouting = Map.of();
    private boolean strictVersioning = false;
    private boolean allowVersionInUrl = true;
    private boolean allowVersionInHeader = true;
    private boolean allowVersionInQuery = false;
    private String defaultVersion = "v1";
    
    // Getters and setters
    public boolean isEnabled() {
        return enabled;
    }
    
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }
    
    public String getCurrentVersion() {
        return currentVersion;
    }
    
    public void setCurrentVersion(String currentVersion) {
        this.currentVersion = currentVersion;
    }
    
    public List<String> getSupportedVersions() {
        return supportedVersions;
    }
    
    public void setSupportedVersions(List<String> supportedVersions) {
        this.supportedVersions = supportedVersions;
    }
    
    public List<String> getDeprecatedVersions() {
        return deprecatedVersions;
    }
    
    public void setDeprecatedVersions(List<String> deprecatedVersions) {
        this.deprecatedVersions = deprecatedVersions;
    }
    
    public Map<String, String> getVersionRouting() {
        return versionRouting;
    }
    
    public void setVersionRouting(Map<String, String> versionRouting) {
        this.versionRouting = versionRouting;
    }
    
    public boolean isStrictVersioning() {
        return strictVersioning;
    }
    
    public void setStrictVersioning(boolean strictVersioning) {
        this.strictVersioning = strictVersioning;
    }
    
    public boolean isAllowVersionInUrl() {
        return allowVersionInUrl;
    }
    
    public void setAllowVersionInUrl(boolean allowVersionInUrl) {
        this.allowVersionInUrl = allowVersionInUrl;
    }
    
    public boolean isAllowVersionInHeader() {
        return allowVersionInHeader;
    }
    
    public void setAllowVersionInHeader(boolean allowVersionInHeader) {
        this.allowVersionInHeader = allowVersionInHeader;
    }
    
    public boolean isAllowVersionInQuery() {
        return allowVersionInQuery;
    }
    
    public void setAllowVersionInQuery(boolean allowVersionInQuery) {
        this.allowVersionInQuery = allowVersionInQuery;
    }
    
    public String getDefaultVersion() {
        return defaultVersion;
    }
    
    public void setDefaultVersion(String defaultVersion) {
        this.defaultVersion = defaultVersion;
    }
}