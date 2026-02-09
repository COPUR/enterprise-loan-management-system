package com.enterprise.openfinance.uc09.domain.port.in;

import com.enterprise.openfinance.uc09.domain.model.InsurancePolicyItemResult;
import com.enterprise.openfinance.uc09.domain.model.InsurancePolicyListResult;
import com.enterprise.openfinance.uc09.domain.query.GetMotorPoliciesQuery;
import com.enterprise.openfinance.uc09.domain.query.GetMotorPolicyQuery;

public interface InsuranceDataUseCase {

    InsurancePolicyListResult listMotorPolicies(GetMotorPoliciesQuery query);

    InsurancePolicyItemResult getMotorPolicy(GetMotorPolicyQuery query);
}
