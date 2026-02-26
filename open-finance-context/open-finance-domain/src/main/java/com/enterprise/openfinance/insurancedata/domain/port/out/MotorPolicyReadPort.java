package com.enterprise.openfinance.insurancedata.domain.port.out;

import com.enterprise.openfinance.insurancedata.domain.model.MotorPolicy;

import java.util.List;
import java.util.Optional;
import java.util.Set;

public interface MotorPolicyReadPort {

    List<MotorPolicy> findByPolicyIds(Set<String> policyIds);

    Optional<MotorPolicy> findByPolicyId(String policyId);
}
