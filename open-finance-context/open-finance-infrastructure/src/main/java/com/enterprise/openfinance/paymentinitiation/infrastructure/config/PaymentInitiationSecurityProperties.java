package com.enterprise.openfinance.paymentinitiation.infrastructure.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "openfinance.paymentinitiation.security")
public class PaymentInitiationSecurityProperties {

    private String detachedSignaturePrefix = "detached-";
    private String payloadHashAlgorithm = "SHA-256";

    public String getDetachedSignaturePrefix() {
        return detachedSignaturePrefix;
    }

    public void setDetachedSignaturePrefix(String detachedSignaturePrefix) {
        this.detachedSignaturePrefix = detachedSignaturePrefix;
    }

    public String getPayloadHashAlgorithm() {
        return payloadHashAlgorithm;
    }

    public void setPayloadHashAlgorithm(String payloadHashAlgorithm) {
        this.payloadHashAlgorithm = payloadHashAlgorithm;
    }
}
