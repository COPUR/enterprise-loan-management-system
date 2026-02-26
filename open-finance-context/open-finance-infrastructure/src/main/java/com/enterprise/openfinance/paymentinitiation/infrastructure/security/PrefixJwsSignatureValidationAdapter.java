package com.enterprise.openfinance.paymentinitiation.infrastructure.security;

import com.enterprise.openfinance.paymentinitiation.domain.port.out.JwsSignatureValidationPort;
import com.enterprise.openfinance.paymentinitiation.infrastructure.config.PaymentInitiationSecurityProperties;
import org.springframework.stereotype.Component;

@Component
public class PrefixJwsSignatureValidationAdapter implements JwsSignatureValidationPort {

    private final String signaturePrefix;

    public PrefixJwsSignatureValidationAdapter(PaymentInitiationSecurityProperties securityProperties) {
        this.signaturePrefix = securityProperties.getDetachedSignaturePrefix();
    }

    @Override
    public boolean isValid(String detachedJwsSignature, String payload) {
        return detachedJwsSignature != null
                && !detachedJwsSignature.isBlank()
                && payload != null
                && !payload.isBlank()
                && detachedJwsSignature.startsWith(signaturePrefix);
    }
}
