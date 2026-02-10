package com.enterprise.openfinance.uc13.domain.model;

public record PayRequestResult(
        PayRequest request,
        boolean cacheHit
) {

    public PayRequestResult {
        if (request == null) {
            throw new IllegalArgumentException("request is required");
        }
    }

    public PayRequestStatus status() {
        return request.status();
    }

    public PayRequestResult withCacheHit(boolean cacheHitValue) {
        return new PayRequestResult(request, cacheHitValue);
    }
}
