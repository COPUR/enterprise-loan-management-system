package com.enterprise.openfinance.uc09.domain.model;

import java.util.List;
import java.util.Optional;

public record InsurancePolicyListResult(
        List<MotorPolicy> policies,
        int page,
        int pageSize,
        int totalRecords,
        boolean cacheHit
) {

    public InsurancePolicyListResult {
        if (policies == null) {
            throw new IllegalArgumentException("policies is required");
        }
        if (page <= 0) {
            throw new IllegalArgumentException("page must be positive");
        }
        if (pageSize <= 0) {
            throw new IllegalArgumentException("pageSize must be positive");
        }
        if (totalRecords < 0) {
            throw new IllegalArgumentException("totalRecords cannot be negative");
        }

        policies = List.copyOf(policies);
    }

    public Optional<Integer> nextPage() {
        if ((long) page * pageSize >= totalRecords) {
            return Optional.empty();
        }
        return Optional.of(page + 1);
    }

    public InsurancePolicyListResult withCacheHit(boolean hit) {
        return new InsurancePolicyListResult(policies, page, pageSize, totalRecords, hit);
    }
}
