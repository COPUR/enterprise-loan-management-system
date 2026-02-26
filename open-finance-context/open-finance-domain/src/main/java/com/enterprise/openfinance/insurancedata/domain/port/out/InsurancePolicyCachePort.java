package com.enterprise.openfinance.insurancedata.domain.port.out;

import com.enterprise.openfinance.insurancedata.domain.model.InsurancePolicyItemResult;
import com.enterprise.openfinance.insurancedata.domain.model.InsurancePolicyListResult;

import java.time.Instant;
import java.util.Optional;

public interface InsurancePolicyCachePort {

    Optional<InsurancePolicyListResult> getPolicyList(String key, Instant now);

    void putPolicyList(String key, InsurancePolicyListResult result, Instant expiresAt);

    Optional<InsurancePolicyItemResult> getPolicy(String key, Instant now);

    void putPolicy(String key, InsurancePolicyItemResult result, Instant expiresAt);
}
