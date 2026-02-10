package com.enterprise.openfinance.bankingmetadata.infrastructure.security;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpHeaders;
import org.springframework.security.oauth2.server.resource.web.BearerTokenResolver;
import org.springframework.security.oauth2.server.resource.web.DefaultBearerTokenResolver;

public class DpopAwareBearerTokenResolver implements BearerTokenResolver {

    private static final String DPOP_PREFIX = "DPoP ";
    private final DefaultBearerTokenResolver delegate = new DefaultBearerTokenResolver();

    @Override
    public String resolve(HttpServletRequest request) {
        String authorization = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (authorization != null && authorization.startsWith(DPOP_PREFIX)) {
            String token = authorization.substring(DPOP_PREFIX.length()).trim();
            return token.isEmpty() ? null : token;
        }
        return delegate.resolve(request);
    }
}
