package com.bank.infrastructure.api.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.List;
import java.util.Map;

/**
 * Security Headers Configuration
 * 
 * Configures security headers for API responses:
 * - FAPI 2.0 compliance headers
 * - OWASP recommended headers
 * - Content Security Policy
 * - Custom banking security headers
 */
@Configuration
@ConfigurationProperties(prefix = "banking.api.security.headers")
public class SecurityHeadersConfiguration {
    
    private boolean enabled = true;
    private boolean enableFapiHeaders = true;
    private boolean enableOwaspHeaders = true;
    private boolean enableHsts = true;
    private boolean enableCsp = true;
    private boolean enableCacheControl = true;
    
    // FAPI headers
    private boolean fapiInteractionId = true;
    private boolean fapiAuthDate = true;
    private boolean fapiCustomerIpAddress = false;
    
    // OWASP headers
    private String xContentTypeOptions = "nosniff";
    private String xFrameOptions = "DENY";
    private String xXssProtection = "1; mode=block";
    private String referrerPolicy = "strict-origin-when-cross-origin";
    private String permissionsPolicy = "geolocation=(), microphone=(), camera=(), payment=()";
    
    // HSTS configuration
    private long hstsMaxAge = 63072000; // 2 years
    private boolean hstsIncludeSubDomains = true;
    private boolean hstsPreload = true;
    
    // Content Security Policy
    private String cspDefaultSrc = "'self'";
    private String cspScriptSrc = "'self' 'unsafe-inline' 'unsafe-eval'";
    private String cspStyleSrc = "'self' 'unsafe-inline'";
    private String cspImgSrc = "'self' data: https:";
    private String cspFontSrc = "'self' https:";
    private String cspConnectSrc = "'self' https:";
    private String cspFrameAncestors = "'none'";
    
    // Cache control
    private String cacheControlValue = "no-store, no-cache, must-revalidate, private";
    private String pragmaValue = "no-cache";
    private String expiresValue = "0";
    
    // Custom headers
    private Map<String, String> customHeaders = Map.of(
        "X-Banking-API-Version", "v1.0.0",
        "X-Banking-Environment", "production"
    );
    
    // Paths to exclude from security headers
    private List<String> excludePaths = List.of(
        "/actuator/health",
        "/swagger-ui/**",
        "/v3/api-docs/**"
    );
    
    // Getters and setters
    public boolean isEnabled() {
        return enabled;
    }
    
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }
    
    public boolean isEnableFapiHeaders() {
        return enableFapiHeaders;
    }
    
    public void setEnableFapiHeaders(boolean enableFapiHeaders) {
        this.enableFapiHeaders = enableFapiHeaders;
    }
    
    public boolean isEnableOwaspHeaders() {
        return enableOwaspHeaders;
    }
    
    public void setEnableOwaspHeaders(boolean enableOwaspHeaders) {
        this.enableOwaspHeaders = enableOwaspHeaders;
    }
    
    public boolean isEnableHsts() {
        return enableHsts;
    }
    
    public void setEnableHsts(boolean enableHsts) {
        this.enableHsts = enableHsts;
    }
    
    public boolean isEnableCsp() {
        return enableCsp;
    }
    
    public void setEnableCsp(boolean enableCsp) {
        this.enableCsp = enableCsp;
    }
    
    public boolean isEnableCacheControl() {
        return enableCacheControl;
    }
    
    public void setEnableCacheControl(boolean enableCacheControl) {
        this.enableCacheControl = enableCacheControl;
    }
    
    public boolean isFapiInteractionId() {
        return fapiInteractionId;
    }
    
    public void setFapiInteractionId(boolean fapiInteractionId) {
        this.fapiInteractionId = fapiInteractionId;
    }
    
    public boolean isFapiAuthDate() {
        return fapiAuthDate;
    }
    
    public void setFapiAuthDate(boolean fapiAuthDate) {
        this.fapiAuthDate = fapiAuthDate;
    }
    
    public boolean isFapiCustomerIpAddress() {
        return fapiCustomerIpAddress;
    }
    
    public void setFapiCustomerIpAddress(boolean fapiCustomerIpAddress) {
        this.fapiCustomerIpAddress = fapiCustomerIpAddress;
    }
    
    public String getXContentTypeOptions() {
        return xContentTypeOptions;
    }
    
    public void setXContentTypeOptions(String xContentTypeOptions) {
        this.xContentTypeOptions = xContentTypeOptions;
    }
    
    public String getXFrameOptions() {
        return xFrameOptions;
    }
    
    public void setXFrameOptions(String xFrameOptions) {
        this.xFrameOptions = xFrameOptions;
    }
    
    public String getXXssProtection() {
        return xXssProtection;
    }
    
    public void setXXssProtection(String xXssProtection) {
        this.xXssProtection = xXssProtection;
    }
    
    public String getReferrerPolicy() {
        return referrerPolicy;
    }
    
    public void setReferrerPolicy(String referrerPolicy) {
        this.referrerPolicy = referrerPolicy;
    }
    
    public String getPermissionsPolicy() {
        return permissionsPolicy;
    }
    
    public void setPermissionsPolicy(String permissionsPolicy) {
        this.permissionsPolicy = permissionsPolicy;
    }
    
    public long getHstsMaxAge() {
        return hstsMaxAge;
    }
    
    public void setHstsMaxAge(long hstsMaxAge) {
        this.hstsMaxAge = hstsMaxAge;
    }
    
    public boolean isHstsIncludeSubDomains() {
        return hstsIncludeSubDomains;
    }
    
    public void setHstsIncludeSubDomains(boolean hstsIncludeSubDomains) {
        this.hstsIncludeSubDomains = hstsIncludeSubDomains;
    }
    
    public boolean isHstsPreload() {
        return hstsPreload;
    }
    
    public void setHstsPreload(boolean hstsPreload) {
        this.hstsPreload = hstsPreload;
    }
    
    public String getCspDefaultSrc() {
        return cspDefaultSrc;
    }
    
    public void setCspDefaultSrc(String cspDefaultSrc) {
        this.cspDefaultSrc = cspDefaultSrc;
    }
    
    public String getCspScriptSrc() {
        return cspScriptSrc;
    }
    
    public void setCspScriptSrc(String cspScriptSrc) {
        this.cspScriptSrc = cspScriptSrc;
    }
    
    public String getCspStyleSrc() {
        return cspStyleSrc;
    }
    
    public void setCspStyleSrc(String cspStyleSrc) {
        this.cspStyleSrc = cspStyleSrc;
    }
    
    public String getCspImgSrc() {
        return cspImgSrc;
    }
    
    public void setCspImgSrc(String cspImgSrc) {
        this.cspImgSrc = cspImgSrc;
    }
    
    public String getCspFontSrc() {
        return cspFontSrc;
    }
    
    public void setCspFontSrc(String cspFontSrc) {
        this.cspFontSrc = cspFontSrc;
    }
    
    public String getCspConnectSrc() {
        return cspConnectSrc;
    }
    
    public void setCspConnectSrc(String cspConnectSrc) {
        this.cspConnectSrc = cspConnectSrc;
    }
    
    public String getCspFrameAncestors() {
        return cspFrameAncestors;
    }
    
    public void setCspFrameAncestors(String cspFrameAncestors) {
        this.cspFrameAncestors = cspFrameAncestors;
    }
    
    public String getCacheControlValue() {
        return cacheControlValue;
    }
    
    public void setCacheControlValue(String cacheControlValue) {
        this.cacheControlValue = cacheControlValue;
    }
    
    public String getPragmaValue() {
        return pragmaValue;
    }
    
    public void setPragmaValue(String pragmaValue) {
        this.pragmaValue = pragmaValue;
    }
    
    public String getExpiresValue() {
        return expiresValue;
    }
    
    public void setExpiresValue(String expiresValue) {
        this.expiresValue = expiresValue;
    }
    
    public Map<String, String> getCustomHeaders() {
        return customHeaders;
    }
    
    public void setCustomHeaders(Map<String, String> customHeaders) {
        this.customHeaders = customHeaders;
    }
    
    public List<String> getExcludePaths() {
        return excludePaths;
    }
    
    public void setExcludePaths(List<String> excludePaths) {
        this.excludePaths = excludePaths;
    }
    
    public String buildContentSecurityPolicy() {
        return String.format(
            "default-src %s; script-src %s; style-src %s; img-src %s; font-src %s; connect-src %s; frame-ancestors %s",
            cspDefaultSrc, cspScriptSrc, cspStyleSrc, cspImgSrc, cspFontSrc, cspConnectSrc, cspFrameAncestors
        );
    }
    
    public String buildHstsHeader() {
        StringBuilder hsts = new StringBuilder();
        hsts.append("max-age=").append(hstsMaxAge);
        
        if (hstsIncludeSubDomains) {
            hsts.append("; includeSubDomains");
        }
        
        if (hstsPreload) {
            hsts.append("; preload");
        }
        
        return hsts.toString();
    }
}