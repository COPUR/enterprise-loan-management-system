package com.enterprise.openfinance.uc09.domain.model;

public record InsurancePolicyItemResult(
        MotorPolicy policy,
        boolean cacheHit
) {

    public InsurancePolicyItemResult {
        if (policy == null) {
            throw new IllegalArgumentException("policy is required");
        }
    }

    public InsurancePolicyItemResult withCacheHit(boolean hit) {
        return new InsurancePolicyItemResult(policy, hit);
    }
}
