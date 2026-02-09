package com.enterprise.openfinance.uc06.infrastructure.security;

import com.enterprise.openfinance.uc06.domain.port.out.JwsSignatureValidationPort;
import com.enterprise.openfinance.uc06.infrastructure.config.Uc06SecurityProperties;
import org.springframework.stereotype.Component;

@Component
public class PrefixJwsSignatureValidationAdapter implements JwsSignatureValidationPort {

    private final String signaturePrefix;

    public PrefixJwsSignatureValidationAdapter(Uc06SecurityProperties securityProperties) {
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
