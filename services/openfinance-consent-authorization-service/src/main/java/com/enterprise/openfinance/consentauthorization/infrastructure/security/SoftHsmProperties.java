package com.enterprise.openfinance.consentauthorization.infrastructure.security;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "openfinance.security.softhsm")
public class SoftHsmProperties {

    private boolean enabled = true;
    private boolean fallbackToJdk = true;
    private String libraryPath = "/opt/softhsm2-devel/lib/softhsm/libsofthsm2.so";
    private String tokenLabel = "first token";
    private Integer slotListIndex = 0;

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public boolean isFallbackToJdk() {
        return fallbackToJdk;
    }

    public void setFallbackToJdk(boolean fallbackToJdk) {
        this.fallbackToJdk = fallbackToJdk;
    }

    public String getLibraryPath() {
        return libraryPath;
    }

    public void setLibraryPath(String libraryPath) {
        this.libraryPath = libraryPath;
    }

    public String getTokenLabel() {
        return tokenLabel;
    }

    public void setTokenLabel(String tokenLabel) {
        this.tokenLabel = tokenLabel;
    }

    public Integer getSlotListIndex() {
        return slotListIndex;
    }

    public void setSlotListIndex(Integer slotListIndex) {
        this.slotListIndex = slotListIndex;
    }
}

