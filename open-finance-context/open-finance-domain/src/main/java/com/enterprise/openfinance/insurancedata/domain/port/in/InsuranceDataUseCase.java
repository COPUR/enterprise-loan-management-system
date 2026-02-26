package com.enterprise.openfinance.insurancedata.domain.port.in;

import com.enterprise.openfinance.insurancedata.domain.model.InsurancePolicyItemResult;
import com.enterprise.openfinance.insurancedata.domain.model.InsurancePolicyListResult;
import com.enterprise.openfinance.insurancedata.domain.query.GetMotorPoliciesQuery;
import com.enterprise.openfinance.insurancedata.domain.query.GetMotorPolicyQuery;

public interface InsuranceDataUseCase {

    InsurancePolicyListResult listMotorPolicies(GetMotorPoliciesQuery query);

    InsurancePolicyItemResult getMotorPolicy(GetMotorPolicyQuery query);
}
